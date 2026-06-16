#!/usr/bin/env python3
"""Extrae fotos de plantas desde un backup completo hacia assets/generated_images.

Uso seguro:
    python3 tools/extract_backup_images_to_assets.py backups/completo.json.gz --dry-run

Aplicar solo a plantas sin imagen o con ruta local no portable:
    python3 tools/extract_backup_images_to_assets.py backups/completo.json.gz --apply --only-missing

Sobrescribir imágenes/URLs de todas las fotos presentes en el backup:
    python3 tools/extract_backup_images_to_assets.py backups/completo.json.gz --apply --overwrite

Nota: este script necesita una COPIA COMPLETA, porque las incrementales actuales no incluyen fotos.
"""
from __future__ import annotations

import argparse
import base64
import gzip
import json
import re
import shutil
import sys
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ASSETS_DIR = ROOT / "app/src/main/assets"
DEFAULT_OUTPUT_DIR = DEFAULT_ASSETS_DIR / "generated_images"
IMAGE_NAME_RE = re.compile(r"(?:^|[_-])(\d+)(?:\D|$)")
ASSET_URL_PREFIX = "file:///android_asset/"


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
    return data


def plant_file_number(path: Path) -> int:
    try:
        return int(path.stem.split("_")[1])
    except Exception:
        return 10**9


@dataclass
class AssetPlantRef:
    path: Path
    index: int
    plant: dict[str, Any]


@dataclass
class ImagePlanItem:
    plant_id: int
    relative_path: str
    target_path: Path
    new_image_url: str
    current_image_url: str
    reason: str
    base64_data: str


@dataclass
class ImagePlan:
    items: list[ImagePlanItem] = field(default_factory=list)
    skipped_no_id: int = 0
    skipped_no_asset_plant: int = 0
    skipped_existing: int = 0
    skipped_only_edited: int = 0
    skipped_limit: int = 0
    invalid_entries: int = 0

    @property
    def changed_json_files(self) -> set[Path]:
        return set()


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


def plant_id_from_relative_path(relative_path: str) -> int | None:
    name = Path(relative_path).name
    # Prefer plant_123.ext style.
    m = re.search(r"plant[_-](\d+)", name, re.IGNORECASE)
    if m:
        return int(m.group(1))
    # Fallback: first number in name.
    m = re.search(r"(\d+)", name)
    if m:
        return int(m.group(1))
    return None


def extension_for_image(relative_path: str) -> str:
    suffix = Path(relative_path).suffix.lower()
    if suffix in {".jpg", ".jpeg", ".png", ".webp"}:
        return suffix
    return ".jpg"


def is_mobile_local_url(value: str) -> bool:
    v = value.strip().lower()
    return (
        v.startswith("content://")
        or v.startswith("file:///data/")
        or v.startswith("file:/data/")
        or v.startswith("file:///storage/")
        or v.startswith("file:/storage/")
        or v.startswith("file:///sdcard/")
        or v.startswith("file:/sdcard/")
    )


def is_missing_or_nonportable_image(image_url: str, assets_dir: Path) -> bool:
    if not image_url.strip():
        return True
    if is_mobile_local_url(image_url):
        return True
    if image_url.startswith(ASSET_URL_PREFIX):
        asset_rel = image_url.removeprefix(ASSET_URL_PREFIX)
        return not (assets_dir / asset_rel).exists()
    return False


def build_plan(
    backup: dict[str, Any],
    asset_by_id: dict[int, AssetPlantRef],
    assets_dir: Path,
    output_dir: Path,
    only_missing: bool,
    overwrite: bool,
    only_edited: bool,
    max_images: int | None,
) -> ImagePlan:
    if only_missing and overwrite:
        raise ValueError("No uses --only-missing y --overwrite a la vez")

    edited_ids = set(x for x in backup.get("editedPlantIds", []) if isinstance(x, int))
    entries = backup.get("plantImages", [])
    if not isinstance(entries, list):
        raise ValueError("El backup no contiene plantImages como lista")

    plan = ImagePlan()
    for entry in entries:
        if max_images is not None and len(plan.items) >= max_images:
            plan.skipped_limit += 1
            continue
        if not isinstance(entry, dict):
            plan.invalid_entries += 1
            continue
        relative_path = str(entry.get("relativePath", "")).strip()
        b64 = entry.get("base64")
        if not relative_path or not isinstance(b64, str) or not b64.strip():
            plan.invalid_entries += 1
            continue
        plant_id = plant_id_from_relative_path(relative_path)
        if plant_id is None:
            plan.skipped_no_id += 1
            continue
        if only_edited and plant_id not in edited_ids:
            plan.skipped_only_edited += 1
            continue
        ref = asset_by_id.get(plant_id)
        if ref is None:
            plan.skipped_no_asset_plant += 1
            continue

        current_url = str(ref.plant.get("imageUrl", "") or "")
        should_take = False
        reason = ""
        if overwrite:
            should_take = True
            reason = "overwrite"
        elif only_missing:
            should_take = is_missing_or_nonportable_image(current_url, assets_dir)
            reason = "missing/non-portable"
        else:
            should_take = is_missing_or_nonportable_image(current_url, assets_dir)
            reason = "missing/non-portable"

        if not should_take:
            plan.skipped_existing += 1
            continue

        ext = extension_for_image(relative_path)
        filename = f"plant_{plant_id}{ext}"
        target_path = output_dir / filename
        new_url = f"{ASSET_URL_PREFIX}generated_images/{filename}"
        plan.items.append(
            ImagePlanItem(
                plant_id=plant_id,
                relative_path=relative_path,
                target_path=target_path,
                new_image_url=new_url,
                current_image_url=current_url,
                reason=reason,
                base64_data=b64,
            )
        )
    return plan


def print_plan(plan: ImagePlan, backup: dict[str, Any], max_details: int) -> None:
    print("Extract backup images → assets")
    print("==============================")
    print(f"Backup version: {backup.get('backupVersion', 'desconocida')}")
    print(f"Backup type:    {backup.get('backupType', 'desconocido')}")
    print(f"plantImages en backup: {len(backup.get('plantImages', [])) if isinstance(backup.get('plantImages'), list) else 'no-list'}")
    print()
    print(f"Imágenes a extraer: {len(plan.items)}")
    print(f"Saltadas con imagen existente: {plan.skipped_existing}")
    print(f"Saltadas por --only-edited: {plan.skipped_only_edited}")
    print(f"Saltadas sin ID detectable: {plan.skipped_no_id}")
    print(f"Saltadas sin planta en assets: {plan.skipped_no_asset_plant}")
    print(f"Entradas inválidas: {plan.invalid_entries}")
    print()

    for item in plan.items[:max_details]:
        print(f"IMG id={item.plant_id} · {item.reason}")
        print(f"  backup: {item.relative_path}")
        print(f"  out:    {rel(item.target_path)}")
        print(f"  url:    {item.current_image_url!r} -> {item.new_image_url!r}")
        print()
    remaining = len(plan.items) - max_details
    if remaining > 0:
        print(f"… {remaining} imágenes más no mostradas. Usa --max-details para ver más.")


def backup_existing_files(paths: set[Path]) -> Path | None:
    existing = {p for p in paths if p.exists()}
    if not existing:
        return None
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_dir = ROOT / f"backups_assets_images_{stamp}"
    backup_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(existing):
        target = backup_dir / path.relative_to(ROOT)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
    return backup_dir


def apply_plan(plan: ImagePlan, by_file: dict[Path, list[dict[str, Any]]], by_id: dict[int, AssetPlantRef]) -> set[Path]:
    image_paths = {item.target_path for item in plan.items}
    json_paths = {by_id[item.plant_id].path for item in plan.items if item.plant_id in by_id}
    backup_dir = backup_existing_files(image_paths | json_paths)
    if backup_dir:
        print(f"Backup automático de archivos existentes en: {rel(backup_dir)}")

    changed_json: set[Path] = set()
    for item in plan.items:
        try:
            raw = base64.b64decode(item.base64_data, validate=False)
        except Exception as exc:
            print(f"AVISO: no se pudo decodificar id={item.plant_id}: {exc}", file=sys.stderr)
            continue
        item.target_path.parent.mkdir(parents=True, exist_ok=True)
        item.target_path.write_bytes(raw)

        ref = by_id[item.plant_id]
        ref.plant["imageUrl"] = item.new_image_url
        by_file[ref.path][ref.index] = ref.plant
        changed_json.add(ref.path)

    for path in sorted(changed_json, key=plant_file_number):
        path.write_text(json.dumps(by_file[path], ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return changed_json


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extrae fotos de backup completo hacia assets/generated_images")
    parser.add_argument("backup", type=Path, help="Backup completo .json o .json.gz")
    parser.add_argument("--assets-dir", type=Path, default=DEFAULT_ASSETS_DIR, help="Directorio assets")
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR, help="Directorio destino para imágenes")
    parser.add_argument("--dry-run", action="store_true", help="Simula sin escribir (por defecto si no usas --apply)")
    parser.add_argument("--apply", action="store_true", help="Escribe imágenes y actualiza imageUrl")
    parser.add_argument("--only-missing", action="store_true", help="Solo procesa plantas sin imagen o con ruta local/no portable")
    parser.add_argument("--overwrite", action="store_true", help="Sobrescribe imageUrl e imagen aunque ya exista")
    parser.add_argument("--only-edited", action="store_true", help="Solo procesa plantas incluidas en editedPlantIds")
    parser.add_argument("--max-images", type=int, default=None, help="Límite de imágenes a procesar")
    parser.add_argument("--max-details", type=int, default=40, help="Número de imágenes detalladas a mostrar")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.apply and args.dry_run:
        print("ERROR: usa --apply o --dry-run, no ambos", file=sys.stderr)
        return 2
    dry_run = not args.apply
    if args.only_missing and args.overwrite:
        print("ERROR: no uses --only-missing y --overwrite a la vez", file=sys.stderr)
        return 2
    if not args.backup.exists():
        print(f"ERROR: no existe backup: {args.backup}", file=sys.stderr)
        return 2
    if not args.assets_dir.exists():
        print(f"ERROR: no existe assets-dir: {args.assets_dir}", file=sys.stderr)
        return 2

    backup = load_backup(args.backup)
    if backup.get("backupType") == "incremental" and not backup.get("plantImages"):
        print("AVISO: este backup parece incremental y no contiene fotos. Usa una copia COMPLETA.")

    by_file, by_id = load_assets(args.assets_dir)
    # Por seguridad, si no se indica nada, actuamos como --only-missing.
    only_missing = args.only_missing or not args.overwrite
    plan = build_plan(
        backup=backup,
        asset_by_id=by_id,
        assets_dir=args.assets_dir,
        output_dir=args.output_dir,
        only_missing=only_missing,
        overwrite=args.overwrite,
        only_edited=args.only_edited,
        max_images=args.max_images,
    )
    print_plan(plan, backup, args.max_details)

    if dry_run:
        print("DRY-RUN: no se ha escrito nada. Para aplicar usa --apply.")
        return 0

    changed = apply_plan(plan, by_file, by_id)
    print(f"Aplicado. JSON modificados: {len(changed)}")
    for path in sorted(changed, key=plant_file_number):
        print(f"  - {rel(path)}")
    print(f"Imágenes escritas: {len(plan.items)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
