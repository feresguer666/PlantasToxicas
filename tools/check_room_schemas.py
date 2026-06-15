#!/usr/bin/env python3
"""Comprueba que existan y sean JSON válidos los esquemas Room exportados.

Uso:
    python3 tools/check_room_schemas.py
    python3 tools/check_room_schemas.py --warn-only

El script no modifica archivos. Sirve para detectar rápido si falta algún
`app/schemas/.../N.json` respecto a la versión declarada en `PlantDatabase.kt`.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATABASE_KT = ROOT / "app/src/main/java/com/toxicplants/database/PlantDatabase.kt"
SCHEMA_DIR = ROOT / "app/schemas/com.toxicplants.database.PlantDatabase"


def read_declared_database_version() -> int:
    text = DATABASE_KT.read_text(encoding="utf-8")
    match = re.search(r"@Database\s*\([\s\S]*?version\s*=\s*(\d+)", text)
    if not match:
        raise RuntimeError(f"No se pudo leer `version = N` en {DATABASE_KT}")
    return int(match.group(1))


def main() -> int:
    parser = argparse.ArgumentParser(description="Comprueba esquemas Room exportados.")
    parser.add_argument(
        "--warn-only",
        action="store_true",
        help="Muestra avisos pero devuelve código 0 aunque falten esquemas.",
    )
    args = parser.parse_args()

    errors: list[str] = []
    warnings: list[str] = []

    if not DATABASE_KT.exists():
        errors.append(f"No existe {DATABASE_KT.relative_to(ROOT)}")
        version = 0
    else:
        try:
            version = read_declared_database_version()
        except Exception as exc:  # pragma: no cover - diagnóstico CLI
            errors.append(str(exc))
            version = 0

    if not SCHEMA_DIR.exists():
        errors.append(f"No existe {SCHEMA_DIR.relative_to(ROOT)}")
    elif version > 0:
        for number in range(1, version + 1):
            schema_file = SCHEMA_DIR / f"{number}.json"
            if not schema_file.exists():
                errors.append(f"Falta esquema Room: {schema_file.relative_to(ROOT)}")
                continue
            try:
                with schema_file.open(encoding="utf-8") as fh:
                    data = json.load(fh)
            except json.JSONDecodeError as exc:
                errors.append(f"JSON inválido en {schema_file.relative_to(ROOT)}: {exc}")
                continue

            schema_version = data.get("database", {}).get("version")
            if schema_version != number:
                warnings.append(
                    f"{schema_file.relative_to(ROOT)} declara database.version={schema_version}, "
                    f"pero el nombre del archivo es {number}.json"
                )

    print("Room schema check")
    print("=================")
    if version:
        print(f"Versión declarada en PlantDatabase.kt: {version}")
    print(f"Directorio de esquemas: {SCHEMA_DIR.relative_to(ROOT)}")
    print()

    if warnings:
        print("Avisos:")
        for item in warnings:
            print(f"  - {item}")
        print()

    if errors:
        print("Problemas detectados:")
        for item in errors:
            print(f"  - {item}")
        print()
        print("Siguiente paso recomendado:")
        print("  1. Abrir el proyecto en Android Studio con Android SDK configurado.")
        print("  2. Ejecutar una compilación para que Room/KSP regenere esquemas.")
        print("  3. Confirmar en Git los JSON generados bajo app/schemas/.")
        return 0 if args.warn_only else 1

    print("OK: todos los esquemas esperados existen y son JSON válidos.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
