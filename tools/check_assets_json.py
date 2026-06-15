#!/usr/bin/env python3
"""Valida los JSON del catálogo y recursos en app/src/main/assets.

Uso habitual:
    python3 tools/check_assets_json.py

El script no modifica archivos. Comprueba que todos los `.json` bajo assets sean
JSON válidos y muestra un resumen de los catálogos principales.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSETS_DIR = ROOT / "app/src/main/assets"
PLANTS_RE = re.compile(r"plants_(\d+)\.json$")


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


def main() -> int:
    parser = argparse.ArgumentParser(description="Valida JSON en app/src/main/assets.")
    parser.add_argument(
        "--top-level-only",
        action="store_true",
        help="Valida solo los JSON directamente dentro de app/src/main/assets.",
    )
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="Muestra solo problemas y el resumen final.",
    )
    args = parser.parse_args()

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

    print("Resultado: OK. Todos los JSON encontrados son válidos.")
    if warnings:
        print("Hay avisos no bloqueantes; revísalos cuando puedas.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
