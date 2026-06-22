#!/usr/bin/env python3
"""Sincroniza cambios de un backup de la app hacia app/src/main/assets/plants_*.json.

Uso seguro:
    python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz --dry-run

Aplicar cambios:
    python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz --apply --include-personal --only-edited

Opciones potentes para catálogo privado:
    --include-personal   sincroniza favoritos, ubicación y notas.
    --create-new         añade plantas del backup que no existan en assets.
    --delete-removed     elimina de assets IDs incluidos en deletedPlantIds.
    --only-edited        solo actualiza plantas incluidas en editedPlantIds.

El script crea backup automático de los JSON modificados antes de escribir.
"""
from __future__ import annotations

import argparse
import gzip
import json
import shutil
import sys
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ASSETS_DIR = ROOT / "app/src/main/assets"

CATALOG_FIELDS = [
    "commonName",
    "commonNames",
    "scientificName",
    "family",
    "toxicityLevel",
    "toxicParts",
    "symptoms",
    "description",
    "habitat",
    "geographicDistribution",
    "firstAid",
    "imageUrl",
    "category",
    "floweringMonths",
    "fruitingMonths",
    "maxToxicityMonths",
    "mythsAndLegends",
]

PERSONAL_FIELDS = [
    "isFavorite",
    "latitude",
    "longitude",
    "locationName",
    "foundDate",
    "notes",
]

FIELD_ORDER = [
    "id",
    "isFavorite",
    "latitude",
    "longitude",
    "locationName",
    "foundDate",
    "notes",
    "commonName",
    "scientificName",
    "family",
    "toxicityLevel",
    "toxicParts",
    "symptoms",
    "description",
    "habitat",
    "geographicDistribution",
    "firstAid",
    "imageUrl",
    "category",
    "commonNames",
    "floweringMonths",
    "fruitingMonths",
    "maxToxicityMonths",
    "mythsAndLegends",
]


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def open_maybe_gzip(path: Path):
    with path.open("rb") as fh:
        magic = fh.read(2)
    if magic == b"\x1f\x8b":
        return gzip.open(path, "rt", encoding="utf-8")
    return path.open("r", encoding="utf-8")


def load_backup(path: Path) -> dict[str, Any]:
    with open_maybe_gzip(path) as fh:
        data = json.load(fh)
    if not isinstance(data, dict):
        raise ValueError("El backup no es un objeto JSON")
    if "plants" not in data or not isinstance(data["plants"], list):
        raise ValueError("El backup no contiene un array 'plants'")
    return data


def plant_file_number(path: Path) -> int:
    stem = path.stem  # plants_12
    try:
        return int(stem.split("_")[1])
    except Exception:
        return 10**9


@dataclass
class AssetPlantRef:
    path: Path
    index: int
    plant: dict[str, Any]


@dataclass
class SyncPlan:
    updates: list[tuple[AssetPlantRef, dict[str, tuple[Any, Any]]]] = field(default_factory=list)
    creates: list[dict[str, Any]] = field(default_factory=list)
    deletes: list[AssetPlantRef] = field(default_factory=list)
    skipped_unsafe_images: list[tuple[int, str]] = field(default_factory=list)
    skipped_not_edited: int = 0

    @property
    def changed_files(self) -> set[Path]:
        files = {ref.path for ref, _ in self.updates}
        files.update(ref.path for ref in self.deletes)
        return files


def load_assets(assets_dir: Path) -> tuple[dict[Path, list[dict[str, Any]]], dict[int, AssetPlantRef]]:
    files = sorted(assets_dir.glob("plants_*.json"), key=plant_file_number)
    if not files:
        raise FileNotFoundError(f"No se encontraron plants_*.json en {assets_dir}")

    by_file: dict[Path, list[dict[str, Any]]] = {}
    by_id: dict[int, AssetPlantRef] = {}
    for path in files:
        with path.open(encoding="utf-8") as fh:
            data = json.load(fh)
        if not isinstance(data, list):
            raise ValueError(f"{rel(path)} no contiene una lista")
        by_file[path] = data
        for idx, item in enumerate(data):
            if not isinstance(item, dict):
                continue
            plant_id = item.get("id")
            if isinstance(plant_id, int) and plant_id != 0:
                by_id[plant_id] = AssetPlantRef(path=path, index=idx, plant=item)
    return by_file, by_id


def is_safe_image_url(value: Any) -> bool:
    if value is None:
        return True
    if not isinstance(value, str):
        return True
    v = value.strip().lower()
    if not v:
        return True
    if v.startswith("http://") or v.startswith("https://"):
        return True
    if v.startswith("file:///android_asset/"):
        return True
    # Rutas locales del móvil: no sirven en el repo ni en otra instalación.
    if v.startswith("content://"):
        return False
    if v.startswith("file:///data/") or v.startswith("file:/data/"):
        return False
    if v.startswith("file:///storage/") or v.startswith("file:/storage/"):
        return False
    if v.startswith("file:///sdcard/") or v.startswith("file:/sdcard/"):
        return False
    return True


def normalize_for_assets(plant: dict[str, Any], fields: list[str]) -> dict[str, Any]:
    out: dict[str, Any] = {"id": plant.get("id", 0)}
    for field in fields:
        if field in plant:
            out[field] = plant[field]
    return order_plant_dict(out)


def order_plant_dict(data: dict[str, Any]) -> dict[str, Any]:
    ordered: dict[str, Any] = {}
    for key in FIELD_ORDER:
        if key in data:
            ordered[key] = data[key]
    for key in data.keys():
        if key not in ordered:
            ordered[key] = data[key]
    return ordered


def short(value: Any, max_len: int = 120) -> str:
    text = json.dumps(value, ensure_ascii=False) if not isinstance(value, str) else value
    text = text.replace("\n", "\\n")
    if len(text) > max_len:
        return text[: max_len - 1] + "…"
    return text


def build_plan(
    backup: dict[str, Any],
    asset_by_id: dict[int, AssetPlantRef],
    include_personal: bool,
    only_edited: bool,
    create_new: bool,
    delete_removed: bool,
) -> SyncPlan:
    plan = SyncPlan()
    fields = CATALOG_FIELDS + (PERSONAL_FIELDS if include_personal else [])
    edited_ids = set(x for x in backup.get("editedPlantIds", []) if isinstance(x, int))
    deleted_ids = set(x for x in backup.get("deletedPlantIds", []) if isinstance(x, int))

    backup_plants = []
    for item in backup.get("plants", []):
        if isinstance(item, dict) and isinstance(item.get("id"), int) and item.get("id") != 0:
            backup_plants.append(item)

    for bplant in backup_plants:
        plant_id = bplant["id"]
        if only_edited and plant_id not in edited_ids:
            if plant_id in asset_by_id:
                plan.skipped_not_edited += 1
            continue

        ref = asset_by_id.get(plant_id)
        if ref is None:
            if create_new:
                new_plant = normalize_for_assets(bplant, fields)
                if "imageUrl" in new_plant and not is_safe_image_url(new_plant["imageUrl"]):
                    plan.skipped_unsafe_images.append((plant_id, str(new_plant["imageUrl"])))
                    new_plant.pop("imageUrl", None)
                plan.creates.append(new_plant)
            continue

        changes: dict[str, tuple[Any, Any]] = {}
        for field_name in fields:
            if field_name not in bplant:
                continue
            new_value = bplant[field_name]
            if field_name == "imageUrl" and not is_safe_image_url(new_value):
                if ref.plant.get(field_name) != new_value:
                    plan.skipped_unsafe_images.append((plant_id, str(new_value)))
                continue
            old_value = ref.plant.get(field_name)
            if old_value != new_value:
                changes[field_name] = (old_value, new_value)
        if changes:
            plan.updates.append((ref, changes))

    if delete_removed:
        for plant_id in sorted(deleted_ids):
            ref = asset_by_id.get(plant_id)
            if ref is not None:
                plan.deletes.append(ref)

    return plan


def print_plan(plan: SyncPlan, backup: dict[str, Any], max_details: int) -> None:
    print("Sync backup → assets")
    print("====================")
    print(f"Backup version: {backup.get('backupVersion', 'desconocida')}")
    print(f"Plantas en backup: {len(backup.get('plants', []))}")
    print(f"editedPlantIds: {len(backup.get('editedPlantIds', []))}")
    print(f"deletedPlantIds: {len(backup.get('deletedPlantIds', []))}")
    print()
    print(f"Actualizaciones: {len(plan.updates)}")
    print(f"Nuevas plantas:  {len(plan.creates)}")
    print(f"Borrados:        {len(plan.deletes)}")
    print(f"Saltadas por --only-edited: {plan.skipped_not_edited}")
    print(f"Imágenes locales ignoradas: {len(plan.skipped_unsafe_images)}")
    print()

    shown = 0
    for ref, changes in plan.updates:
        if shown >= max_details:
            break
        name = ref.plant.get("scientificName") or ref.plant.get("commonName") or ""
        print(f"MOD {rel(ref.path)} · id={ref.plant.get('id')} · {name}")
        for field_name, (old, new) in changes.items():
            print(f"  {field_name}:")
            print(f"    - {short(old)}")
            print(f"    + {short(new)}")
        print()
        shown += 1

    for plant in plan.creates[: max(0, max_details - shown)]:
        print(f"NEW id={plant.get('id')} · {plant.get('scientificName') or plant.get('commonName')}")
        shown += 1

    for ref in plan.deletes[: max(0, max_details - shown)]:
        print(f"DEL {rel(ref.path)} · id={ref.plant.get('id')} · {ref.plant.get('scientificName') or ref.plant.get('commonName')}")
        shown += 1

    remaining = len(plan.updates) + len(plan.creates) + len(plan.deletes) - shown
    if remaining > 0:
        print(f"… {remaining} cambios más no mostrados. Usa --max-details para ver más.")
    if plan.skipped_unsafe_images:
        print()
        print("Imágenes locales ignoradas (primeras 10):")
        for plant_id, url in plan.skipped_unsafe_images[:10]:
            print(f"  id={plant_id}: {short(url)}")


def backup_asset_files(files: set[Path]) -> Path | None:
    if not files:
        return None
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_dir = ROOT / f"backups_assets_{stamp}"
    backup_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(files):
        target = backup_dir / path.relative_to(ROOT)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
    return backup_dir


def apply_plan(plan: SyncPlan, by_file: dict[Path, list[dict[str, Any]]], assets_dir: Path, max_per_file: int) -> set[Path]:
    changed_files: set[Path] = set(plan.changed_files)

    # Actualizar existentes.
    for ref, changes in plan.updates:
        item = by_file[ref.path][ref.index]
        for field_name, (_, new_value) in changes.items():
            item[field_name] = new_value
        by_file[ref.path][ref.index] = order_plant_dict(item)

    # Borrar, de atrás hacia delante por archivo para no desplazar índices.
    deletes_by_file: dict[Path, list[int]] = {}
    for ref in plan.deletes:
        deletes_by_file.setdefault(ref.path, []).append(ref.index)
    for path, indices in deletes_by_file.items():
        for idx in sorted(indices, reverse=True):
            del by_file[path][idx]
        changed_files.add(path)

    # Crear nuevas plantas en el último plants_N con hueco o nuevos archivos.
    if plan.creates:
        files = sorted(by_file.keys(), key=plant_file_number)
        last = files[-1]
        next_number = plant_file_number(last) + 1
        current = last
        for plant in plan.creates:
            if len(by_file[current]) >= max_per_file:
                current = assets_dir / f"plants_{next_number}.json"
                next_number += 1
                by_file[current] = []
            by_file[current].append(order_plant_dict(plant))
            changed_files.add(current)

    # Escribir JSON.
    for path in sorted(changed_files, key=plant_file_number):
        data = by_file[path]
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    return changed_files


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Sincroniza backup de PlantasToxicas hacia assets/plants_*.json")
    parser.add_argument("backup", type=Path, help="Ruta al backup .json o .json.gz")
    parser.add_argument("--assets-dir", type=Path, default=DEFAULT_ASSETS_DIR, help="Directorio assets (por defecto app/src/main/assets)")
    parser.add_argument("--dry-run", action="store_true", help="Simula cambios sin escribir (por defecto si no usas --apply)")
    parser.add_argument("--apply", action="store_true", help="Aplica cambios a plants_*.json")
    parser.add_argument("--include-personal", action="store_true", help="Incluye favoritos, ubicación y notas personales")
    parser.add_argument("--only-edited", action="store_true", help="Solo actualiza plantas incluidas en editedPlantIds")
    parser.add_argument("--create-new", action="store_true", help="Crea plantas del backup que no existan en assets")
    parser.add_argument("--delete-removed", action="store_true", help="Borra de assets IDs incluidos en deletedPlantIds")
    parser.add_argument("--max-per-file", type=int, default=500, help="Máximo de plantas por archivo al crear nuevas")
    parser.add_argument("--max-details", type=int, default=40, help="Máximo de cambios detallados en pantalla")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.apply and args.dry_run:
        print("ERROR: usa --apply o --dry-run, no ambos", file=sys.stderr)
        return 2
    dry_run = not args.apply

    if not args.backup.exists():
        print(f"ERROR: no existe backup: {args.backup}", file=sys.stderr)
        return 2
    if not args.assets_dir.exists():
        print(f"ERROR: no existe assets-dir: {args.assets_dir}", file=sys.stderr)
        return 2
    if args.max_per_file < 1:
        print("ERROR: --max-per-file debe ser >= 1", file=sys.stderr)
        return 2

    backup = load_backup(args.backup)
    by_file, by_id = load_assets(args.assets_dir)
    plan = build_plan(
        backup=backup,
        asset_by_id=by_id,
        include_personal=args.include_personal,
        only_edited=args.only_edited,
        create_new=args.create_new,
        delete_removed=args.delete_removed,
    )

    print_plan(plan, backup, args.max_details)

    if dry_run:
        print()
        print("DRY-RUN: no se ha escrito nada. Para aplicar usa --apply.")
        return 0

    changed_candidates = set(plan.changed_files)
    if plan.creates:
        # El último archivo o uno nuevo puede cambiar; backup de todos los plants_*.json por seguridad.
        changed_candidates.update(by_file.keys())
    backup_dir = backup_asset_files(changed_candidates)
    if backup_dir:
        print(f"Backup automático de JSON en: {rel(backup_dir)}")

    changed_files = apply_plan(plan, by_file, args.assets_dir, args.max_per_file)
    print(f"Aplicado. Archivos modificados: {len(changed_files)}")
    for path in sorted(changed_files, key=plant_file_number):
        print(f"  - {rel(path)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
