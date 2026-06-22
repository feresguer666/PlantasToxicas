#!/usr/bin/env python3
"""Importa cambios desde CSV hacia app/src/main/assets/plants_*.json.

Soporta dos formatos:

1) Formato largo:
   id,field,value
   123,description,"Nueva descripción"

2) Formato ancho:
   id,description,symptoms,toxicityLevel
   123,"Nueva descripción","Nuevos síntomas",Alto

Uso seguro:
   python3 tools/import_csv_changes_to_assets.py cambios.csv --dry-run

Aplicar:
   python3 tools/import_csv_changes_to_assets.py cambios.csv --apply

Para campos personales en app privada:
   python3 tools/import_csv_changes_to_assets.py cambios.csv --apply --include-personal
"""
from __future__ import annotations

import argparse
import csv
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

BOOL_FIELDS = {"isFavorite"}
FLOAT_FIELDS = {"latitude", "longitude"}
INT_FIELDS = {"id"}


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def plant_file_number(path: Path) -> int:
    try:
        return int(path.stem.split("_")[1])
    except Exception:
        return 10**9


@dataclass
class PlantRef:
    path: Path
    index: int
    plant: dict[str, Any]


@dataclass
class CsvChange:
    plant_id: int
    field: str
    value: Any
    row_number: int


@dataclass
class ImportPlan:
    updates: list[tuple[PlantRef, dict[str, tuple[Any, Any, int]]]] = field(default_factory=list)
    skipped_unknown_id: list[CsvChange] = field(default_factory=list)
    skipped_invalid_field: list[CsvChange] = field(default_factory=list)
    skipped_unsafe_image: list[CsvChange] = field(default_factory=list)
    skipped_no_change: int = 0
    errors: list[str] = field(default_factory=list)

    @property
    def changed_files(self) -> set[Path]:
        return {ref.path for ref, _ in self.updates}


def load_assets(assets_dir: Path) -> tuple[dict[Path, list[dict[str, Any]]], dict[int, PlantRef]]:
    files = sorted(assets_dir.glob("plants_*.json"), key=plant_file_number)
    if not files:
        raise FileNotFoundError(f"No se encontraron plants_*.json en {assets_dir}")
    by_file: dict[Path, list[dict[str, Any]]] = {}
    by_id: dict[int, PlantRef] = {}
    for path in files:
        data = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(data, list):
            raise ValueError(f"{rel(path)} no contiene una lista")
        by_file[path] = data
        for idx, item in enumerate(data):
            if isinstance(item, dict) and isinstance(item.get("id"), int):
                by_id[item["id"]] = PlantRef(path, idx, item)
    return by_file, by_id


def is_safe_image_url(value: Any) -> bool:
    if not isinstance(value, str):
        return True
    v = value.strip().lower()
    if not v:
        return True
    if v.startswith("http://") or v.startswith("https://"):
        return True
    if v.startswith("file:///android_asset/"):
        return True
    if v.startswith("content://"):
        return False
    if v.startswith("file:///data/") or v.startswith("file:/data/"):
        return False
    if v.startswith("file:///storage/") or v.startswith("file:/storage/"):
        return False
    if v.startswith("file:///sdcard/") or v.startswith("file:/sdcard/"):
        return False
    return True


def parse_value(field: str, raw: str) -> Any:
    value = raw.strip()
    if field in BOOL_FIELDS:
        low = value.lower()
        if low in {"true", "1", "yes", "si", "sí", "y"}:
            return True
        if low in {"false", "0", "no", "n"}:
            return False
        raise ValueError(f"Valor booleano inválido para {field}: {raw!r}")
    if field in FLOAT_FIELDS:
        if value == "":
            return None
        return float(value.replace(",", "."))
    # Para campos string personales/catalogo: vacío = "" salvo campos nullable personales.
    if field in {"locationName", "foundDate", "notes"} and value == "":
        return None
    return raw


def read_csv_changes(csv_path: Path, allowed_fields: set[str]) -> tuple[list[CsvChange], list[str]]:
    errors: list[str] = []
    changes: list[CsvChange] = []
    with csv_path.open(newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        if not reader.fieldnames:
            return [], ["CSV sin cabecera"]
        fieldnames = [name.strip() for name in reader.fieldnames]
        long_mode = {"id", "field", "value"}.issubset(set(fieldnames))

        for row_number, row in enumerate(reader, start=2):
            raw_id = (row.get("id") or "").strip()
            try:
                plant_id = int(raw_id)
            except ValueError:
                errors.append(f"Fila {row_number}: id inválido {raw_id!r}")
                continue

            if long_mode:
                field = (row.get("field") or "").strip()
                raw_value = row.get("value") or ""
                if field not in allowed_fields:
                    # Lo guardamos como cambio para reportarlo como campo no permitido.
                    changes.append(CsvChange(plant_id, field, raw_value, row_number))
                    continue
                try:
                    value = parse_value(field, raw_value)
                except Exception as exc:
                    errors.append(f"Fila {row_number}: {exc}")
                    continue
                changes.append(CsvChange(plant_id, field, value, row_number))
            else:
                for field in fieldnames:
                    if field == "id":
                        continue
                    if field not in row or row[field] is None:
                        continue
                    # En modo ancho, celda vacía significa actualizar a vacío/null.
                    raw_value = row[field]
                    if field not in allowed_fields:
                        changes.append(CsvChange(plant_id, field, raw_value, row_number))
                        continue
                    try:
                        value = parse_value(field, raw_value)
                    except Exception as exc:
                        errors.append(f"Fila {row_number}, campo {field}: {exc}")
                        continue
                    changes.append(CsvChange(plant_id, field, value, row_number))
    return changes, errors


def build_plan(changes: list[CsvChange], errors: list[str], by_id: dict[int, PlantRef], allowed_fields: set[str]) -> ImportPlan:
    plan = ImportPlan(errors=errors[:])
    grouped: dict[int, dict[str, CsvChange]] = {}
    for change in changes:
        if change.field not in allowed_fields:
            plan.skipped_invalid_field.append(change)
            continue
        if change.plant_id not in by_id:
            plan.skipped_unknown_id.append(change)
            continue
        if change.field == "imageUrl" and not is_safe_image_url(change.value):
            plan.skipped_unsafe_image.append(change)
            continue
        grouped.setdefault(change.plant_id, {})[change.field] = change

    for plant_id, field_changes in grouped.items():
        ref = by_id[plant_id]
        diffs: dict[str, tuple[Any, Any, int]] = {}
        for field, change in field_changes.items():
            old = ref.plant.get(field)
            new = change.value
            if old != new:
                diffs[field] = (old, new, change.row_number)
            else:
                plan.skipped_no_change += 1
        if diffs:
            plan.updates.append((ref, diffs))
    return plan


def short(value: Any, max_len: int = 120) -> str:
    if value is None:
        text = "null"
    elif isinstance(value, str):
        text = value
    else:
        text = json.dumps(value, ensure_ascii=False)
    text = text.replace("\n", "\\n")
    if len(text) > max_len:
        return text[: max_len - 1] + "…"
    return text


def print_plan(plan: ImportPlan, max_details: int) -> None:
    print("Import CSV changes → assets")
    print("===========================")
    print(f"Actualizaciones: {len(plan.updates)} plantas")
    print(f"Sin cambios: {plan.skipped_no_change}")
    print(f"IDs no encontrados: {len(plan.skipped_unknown_id)}")
    print(f"Campos no permitidos: {len(plan.skipped_invalid_field)}")
    print(f"Imágenes locales ignoradas: {len(plan.skipped_unsafe_image)}")
    print(f"Errores de CSV: {len(plan.errors)}")
    print()

    shown = 0
    for ref, diffs in plan.updates:
        if shown >= max_details:
            break
        print(f"MOD {rel(ref.path)} · id={ref.plant.get('id')} · {ref.plant.get('scientificName') or ref.plant.get('commonName')}")
        for field, (old, new, row) in diffs.items():
            print(f"  fila {row} · {field}:")
            print(f"    - {short(old)}")
            print(f"    + {short(new)}")
        print()
        shown += 1
    remaining = len(plan.updates) - shown
    if remaining > 0:
        print(f"… {remaining} plantas más no mostradas. Usa --max-details para ver más.")

    if plan.errors:
        print("Errores CSV (primeros 10):")
        for err in plan.errors[:10]:
            print(f"  - {err}")
    if plan.skipped_invalid_field:
        print("Campos no permitidos (primeros 10):")
        for c in plan.skipped_invalid_field[:10]:
            print(f"  - fila {c.row_number}: {c.field}")
    if plan.skipped_unknown_id:
        print("IDs no encontrados (primeros 10):")
        for c in plan.skipped_unknown_id[:10]:
            print(f"  - fila {c.row_number}: id={c.plant_id}")
    if plan.skipped_unsafe_image:
        print("imageUrl locales ignoradas (primeras 10):")
        for c in plan.skipped_unsafe_image[:10]:
            print(f"  - fila {c.row_number}: id={c.plant_id}: {short(c.value)}")


def backup_files(paths: set[Path]) -> Path | None:
    if not paths:
        return None
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_dir = ROOT / f"backups_assets_csv_{stamp}"
    backup_dir.mkdir(parents=True, exist_ok=True)
    for path in sorted(paths):
        target = backup_dir / path.relative_to(ROOT)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, target)
    return backup_dir


def apply_plan(plan: ImportPlan, by_file: dict[Path, list[dict[str, Any]]]) -> set[Path]:
    changed_files = plan.changed_files
    backup_dir = backup_files(changed_files)
    if backup_dir:
        print(f"Backup automático en: {rel(backup_dir)}")

    for ref, diffs in plan.updates:
        item = by_file[ref.path][ref.index]
        for field, (_, new, _) in diffs.items():
            item[field] = new
        by_file[ref.path][ref.index] = item

    for path in sorted(changed_files, key=plant_file_number):
        path.write_text(json.dumps(by_file[path], ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return changed_files


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Importa cambios CSV hacia assets/plants_*.json")
    parser.add_argument("csv", type=Path, help="CSV de cambios")
    parser.add_argument("--assets-dir", type=Path, default=DEFAULT_ASSETS_DIR, help="Directorio assets")
    parser.add_argument("--dry-run", action="store_true", help="Simula sin escribir (por defecto si no usas --apply)")
    parser.add_argument("--apply", action="store_true", help="Aplica cambios")
    parser.add_argument("--include-personal", action="store_true", help="Permite isFavorite, ubicación y notes")
    parser.add_argument("--max-details", type=int, default=40, help="Máximo de plantas detalladas")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.apply and args.dry_run:
        print("ERROR: usa --apply o --dry-run, no ambos", file=sys.stderr)
        return 2
    if not args.csv.exists():
        print(f"ERROR: no existe CSV: {args.csv}", file=sys.stderr)
        return 2
    if not args.assets_dir.exists():
        print(f"ERROR: no existe assets-dir: {args.assets_dir}", file=sys.stderr)
        return 2

    allowed_fields = set(CATALOG_FIELDS + (PERSONAL_FIELDS if args.include_personal else []))
    by_file, by_id = load_assets(args.assets_dir)
    changes, errors = read_csv_changes(args.csv, allowed_fields)
    plan = build_plan(changes, errors, by_id, allowed_fields)
    print_plan(plan, args.max_details)

    if not args.apply:
        print("DRY-RUN: no se ha escrito nada. Para aplicar usa --apply.")
        return 1 if plan.errors else 0

    if plan.errors:
        print("ERROR: hay errores en el CSV. Corrige antes de aplicar.", file=sys.stderr)
        return 2

    changed = apply_plan(plan, by_file)
    print(f"Aplicado. Archivos modificados: {len(changed)}")
    for path in sorted(changed, key=plant_file_number):
        print(f"  - {rel(path)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
