#!/usr/bin/env python3
"""Valida los JSON del catálogo y recursos en app/src/main/assets.

Uso habitual:
    python3 tools/check_assets_json.py
    python3 tools/check_assets_json.py --validate-plants

El script no modifica archivos. Comprueba que todos los `.json` bajo assets sean
JSON válidos y muestra un resumen de los catálogos principales.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = ROOT / "app/src/main/assets"
PLANTS_RE = re.compile(r"plants_(\d+)\.json$")

# Campos mínimos esperados en cada entrada de plants_N.json.
# La validación de campos vacíos se emite como aviso por defecto para no bloquear
# catálogos en curso de mejora. Usa --strict-plants si quieres tratar esos avisos como error.
PLANT_REQUIRED_FIELDS = [
    "id",
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
]
PLANT_TEXT_FIELDS = [field for field in PLANT_REQUIRED_FIELDS if field != "id"]
KNOWN_TOXICITY_LEVELS = {"Bajo", "Moderado", "Alto", "Muy alto", "Mortal", "Desconocido"}


def sort_key(path: Path) -> tuple[str, int, str]:
    """Orden natural para que plants_2 vaya antes que plants_10."""
    match = PLANTS_RE.match(path.name)
    if match:
        return ("plants", int(match.group(1)), path.name)
    return (path.name, -1, path.name)


@dataclass
class JsonInfo:
    path: Path
    kind: str
    count: int | None


def describe_json(data: Any) -> tuple[str, int | None]:
    if isinstance(data, list):
        return "list", len(data)
    if isinstance(data, dict):
        return "dict", len(data)
    return type(data).__name__, None


def rel(path: Path) -> str:
    return str(path.relative_to(ROOT))


def is_blank(value: Any) -> bool:
    return value is None or (isinstance(value, str) and not value.strip())


def add_example(examples: dict[str, list[str]], key: str, text: str, max_examples: int) -> None:
    bucket = examples[key]
    if len(bucket) < max_examples:
        bucket.append(text)


def validate_plants(max_examples: int) -> tuple[list[str], list[str], dict[str, int]]:
    """Valida estructura básica de plants_N.json.

    Devuelve (errores, avisos, métricas). No modifica archivos.
    """
    errors: list[str] = []
    warnings: list[str] = []
    examples: dict[str, list[str]] = defaultdict(list)
    missing_counts: Counter[str] = Counter()
    blank_counts: Counter[str] = Counter()
    unknown_toxicity: Counter[str] = Counter()
    seen_ids: dict[int, str] = {}
    duplicate_ids: list[str] = []
    total_records = 0
    non_object_records = 0

    plant_files = sorted(ASSETS_DIR.glob("plants_*.json"), key=sort_key)
    if not plant_files:
        errors.append("No se encontraron archivos plants_*.json")
        return errors, warnings, {"total_records": 0}

    for path in plant_files:
        try:
            data = json.load(path.open(encoding="utf-8"))
        except Exception as exc:  # ya se informa en la validación general; aquí evitamos duplicar trazas.
            errors.append(f"No se pudo validar plantas en {rel(path)}: {exc}")
            continue

        if not isinstance(data, list):
            errors.append(f"{rel(path)} debería contener una lista de plantas")
            continue

        for index, item in enumerate(data):
            total_records += 1
            location = f"{rel(path)}[{index}]"
            if not isinstance(item, dict):
                non_object_records += 1
                add_example(examples, "non_object", location, max_examples)
                continue

            plant_id = item.get("id")
            label = f"{location} id={plant_id!r} {item.get('scientificName') or item.get('commonName') or ''}".strip()

            if not isinstance(plant_id, int) or isinstance(plant_id, bool):
                errors.append(f"{label}: `id` debe ser un entero")
            elif plant_id in seen_ids:
                duplicate_ids.append(f"id={plant_id} repetido en {seen_ids[plant_id]} y {location}")
            else:
                seen_ids[plant_id] = location

            for field in PLANT_REQUIRED_FIELDS:
                if field not in item:
                    missing_counts[field] += 1
                    add_example(examples, f"missing:{field}", label, max_examples)

            for field in PLANT_TEXT_FIELDS:
                if field in item and is_blank(item[field]):
                    blank_counts[field] += 1
                    add_example(examples, f"blank:{field}", label, max_examples)

            toxicity = item.get("toxicityLevel")
            if isinstance(toxicity, str) and toxicity.strip() and toxicity not in KNOWN_TOXICITY_LEVELS:
                unknown_toxicity[toxicity] += 1
                add_example(examples, "unknown_toxicity", f"{label}: {toxicity!r}", max_examples)

    if non_object_records:
        errors.append(f"Hay {non_object_records} registros de plantas que no son objetos JSON/dict")
        for example in examples.get("non_object", []):
            errors.append(f"  ejemplo: {example}")

    if duplicate_ids:
        errors.append(f"Hay {len(duplicate_ids)} IDs duplicados en plantas")
        for example in duplicate_ids[:max_examples]:
            errors.append(f"  ejemplo: {example}")

    if missing_counts:
        for field, count in sorted(missing_counts.items()):
            errors.append(f"Falta campo obligatorio `{field}` en {count} plantas")
            for example in examples.get(f"missing:{field}", []):
                errors.append(f"  ejemplo: {example}")

    if blank_counts:
        warnings.append("Campos obligatorios presentes pero vacíos/null:")
        for field, count in sorted(blank_counts.items()):
            warnings.append(f"  - `{field}` vacío/null en {count} plantas")
            for example in examples.get(f"blank:{field}", []):
                warnings.append(f"    ejemplo: {example}")

    if unknown_toxicity:
        warnings.append("Niveles de toxicidad no reconocidos:")
        for value, count in sorted(unknown_toxicity.items()):
            warnings.append(f"  - {value!r}: {count} plantas")
        for example in examples.get("unknown_toxicity", []):
            warnings.append(f"    ejemplo: {example}")

    metrics = {
        "total_records": total_records,
        "unique_ids": len(seen_ids),
        "duplicate_ids": len(duplicate_ids),
        "blank_fields": sum(blank_counts.values()),
        "missing_fields": sum(missing_counts.values()),
    }
    return errors, warnings, metrics


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida JSON en app/src/main/assets.")
    parser.add_argument(
        "--top-level-only",
        action="store_true",
        help="Valida solo los JSON directamente dentro de app/src/main/assets.",
    )
    parser.add_argument(
        "--validate-plants",
        action="store_true",
        help="Valida campos mínimos, IDs y avisos de calidad en plants_*.json.",
    )
    parser.add_argument(
        "--strict-plants",
        action="store_true",
        help="Con --validate-plants, trata avisos de plantas como error de salida.",
    )
    parser.add_argument(
        "--max-examples",
        type=int,
        default=5,
        help="Número máximo de ejemplos por tipo de problema al validar plantas (por defecto: 5).",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Muestra solo problemas y el resumen final.",
    )
    args = parser.parse_args()

    if args.max_examples < 0:
        parser.error("--max-examples debe ser 0 o mayor")

    if not ASSETS_DIR.exists():
        print(f"ERROR: no existe {rel(ASSETS_DIR)}", file=sys.stderr)
        return 1

    pattern = "*.json" if args.top_level_only else "**/*.json"
    json_files = sorted(ASSETS_DIR.glob(pattern), key=sort_key)

    if not json_files:
        print(f"ERROR: no se encontraron JSON en {rel(ASSETS_DIR)}", file=sys.stderr)
        return 1

    infos: list[JsonInfo] = []
    errors: list[str] = []
    warnings: list[str] = []

    for path in json_files:
        try:
            with path.open(encoding="utf-8") as fh:
                data = json.load(fh)
        except json.JSONDecodeError as exc:
            errors.append(f"JSON inválido en {rel(path)}: línea {exc.lineno}, columna {exc.colno}: {exc.msg}")
            continue
        except UnicodeDecodeError as exc:
            errors.append(f"Codificación inválida en {rel(path)}: {exc}")
            continue
        except OSError as exc:
            errors.append(f"No se pudo leer {rel(path)}: {exc}")
            continue

        kind, count = describe_json(data)
        infos.append(JsonInfo(path=path, kind=kind, count=count))

    # Comprobación de secuencia plants_N.json en el nivel superior.
    plant_numbers: list[int] = []
    plant_counts: dict[int, int] = {}
    for info in infos:
        if info.path.parent != ASSETS_DIR:
            continue
        match = PLANTS_RE.match(info.path.name)
        if not match:
            continue
        number = int(match.group(1))
        plant_numbers.append(number)
        if info.kind != "list":
            warnings.append(f"{rel(info.path)} debería ser una lista, pero es {info.kind}")
        elif info.count is not None:
            plant_counts[number] = info.count

    if plant_numbers:
        minimum = min(plant_numbers)
        maximum = max(plant_numbers)
        missing = sorted(set(range(minimum, maximum + 1)) - set(plant_numbers))
        if minimum != 1:
            warnings.append(f"La secuencia plants_N.json empieza en {minimum}; se esperaba 1")
        if missing:
            warnings.append("Faltan archivos de plantas: " + ", ".join(f"plants_{n}.json" for n in missing))

    top_level = [info for info in infos if info.path.parent == ASSETS_DIR]
    total_plants = sum(plant_counts.values())

    plant_metrics: dict[str, int] | None = None
    if args.validate_plants:
        plant_errors, plant_warnings, plant_metrics = validate_plants(args.max_examples)
        errors.extend(plant_errors)
        warnings.extend(plant_warnings)

    if not args.quiet:
        print("JSON assets check")
        print("=================")
        print(f"Directorio: {rel(ASSETS_DIR)}")
        print(f"JSON validados: {len(infos)} de {len(json_files)} encontrados")
        print()
        print("Catálogos principales en assets/:")
        for info in sorted(top_level, key=lambda item: sort_key(item.path)):
            count = "-" if info.count is None else str(info.count)
            print(f"  - {info.path.name}: {info.kind}, {count} elementos")
        print()
        if plant_numbers:
            print(f"Plantas: plants_{min(plant_numbers)}.json a plants_{max(plant_numbers)}.json")
            print(f"Total de registros en plants_*.json: {total_plants}")
            print()
        if plant_metrics is not None:
            print("Validación básica de plantas:")
            print(f"  - Registros revisados: {plant_metrics['total_records']}")
            print(f"  - IDs únicos: {plant_metrics['unique_ids']}")
            print(f"  - IDs duplicados: {plant_metrics['duplicate_ids']}")
            print(f"  - Campos obligatorios ausentes: {plant_metrics['missing_fields']}")
            print(f"  - Campos obligatorios vacíos/null: {plant_metrics['blank_fields']}")
            print()

    if warnings:
        print("Avisos:")
        for item in warnings:
            print(f"  - {item}")
        print()

    if errors:
        print("Errores:")
        for item in errors:
            print(f"  - {item}")
        print()
        print("Resultado: ERROR. Corrige los JSON anteriores antes de compilar/publicar.")
        return 1

    if args.strict_plants and args.validate_plants and warnings:
        print("Resultado: ERROR. --strict-plants trata los avisos de plantas como error.")
        return 1

    print("Resultado: OK. Todos los JSON encontrados son válidos.")
    if warnings:
        print("Hay avisos no bloqueantes; revísalos cuando puedas.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
