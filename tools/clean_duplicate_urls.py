#!/usr/bin/env python3
"""
clean_duplicate_urls.py - VERSIÓN AVANZADA (Global + Límite 3 URLs)
==================================================================

Limpia URLs repetidas en los archivos plants_*.json

Características:
- Elimina URLs duplicadas DENTRO de cada planta
- Elimina URLs que se repiten ENTRE diferentes plantas (global)
- Límite estricto: máximo 3 URLs únicas por planta
- Crea backup automático antes de modificar
- Modo --dry-run para probar sin tocar nada
- Estadísticas detalladas

Uso recomendado:
    # Primero ver qué haría (sin modificar)
    python3 tools/clean_duplicate_urls.py --dry-run --global

    # Ejecutar limpieza real
    python3 tools/clean_duplicate_urls.py --global
"""

import argparse
import json
import os
import shutil
import time
from pathlib import Path
from typing import List, Dict, Set

ASSETS_DIR = Path("app/src/main/assets")
BACKUP_DIR = Path("backups_urls_cleaned")

def parse_image_urls(image_url: str) -> List[str]:
    if not image_url:
        return []
    return [u.strip() for u in image_url.split("|") if u.strip()]

def join_image_urls(urls: List[str]) -> str:
    return " | ".join(urls)

def clean_plant_urls_global(
    plant: Dict,
    global_used: Set[str],
    max_urls: int = 3
) -> tuple:
    """
    Limpia las URLs de una planta con control global.
    Devuelve: (planta_modificada, duplicados_internos, duplicados_globales)
    """
    original = plant.get("imageUrl", "")
    urls = parse_image_urls(original)

    if not urls:
        return plant, 0, 0

    cleaned = []
    internal_duplicates = 0
    global_duplicates = 0

    for url in urls:
        # 1. Evitar duplicados dentro de la misma planta
        if url in cleaned:
            internal_duplicates += 1
            continue

        # 2. Evitar URLs ya usadas por otras plantas (global)
        if url in global_used:
            global_duplicates += 1
            continue

        # 3. Añadir si no hemos llegado al límite
        if len(cleaned) < max_urls:
            cleaned.append(url)
            global_used.add(url)

    # Reconstruir el campo imageUrl
    if cleaned:
        plant["imageUrl"] = join_image_urls(cleaned)
    else:
        plant["imageUrl"] = ""

    return plant, internal_duplicates, global_duplicates

def process_all_files(dry_run: bool = False, global_mode: bool = False, max_urls: int = 3):
    plant_files = sorted(ASSETS_DIR.glob("plants_*.json"))

    if not plant_files:
        print("❌ No se encontraron archivos plants_*.json")
        return

    print(f"📁 Encontrados {len(plant_files)} archivos de plantas")

    # Crear backup
    if not dry_run:
        BACKUP_DIR.mkdir(exist_ok=True)
        backup_name = f"backup_urls_{int(time.time())}"
        backup_path = BACKUP_DIR / backup_name
        print(f"💾 Creando backup en: {backup_path}")
        shutil.copytree(ASSETS_DIR, backup_path, dirs_exist_ok=True)

    total_plants = 0
    total_internal = 0
    total_global = 0
    plants_modified = 0

    global_used_urls: Set[str] = set()

    for plant_file in plant_files:
        print(f"\n🔄 Procesando: {plant_file.name}")

        try:
            with open(plant_file, "r", encoding="utf-8") as f:
                plants = json.load(f)
        except Exception as e:
            print(f"   ❌ Error leyendo archivo: {e}")
            continue

        file_internal = 0
        file_global = 0
        file_modified = 0

        for plant in plants:
            total_plants += 1

            original_url = plant.get("imageUrl", "")
            cleaned_plant, internal_dups, global_dups = clean_plant_urls_global(
                plant, global_used_urls, max_urls
            )

            if internal_dups > 0 or global_dups > 0:
                file_modified += 1
                file_internal += internal_dups
                file_global += global_dups

        total_internal += file_internal
        total_global += file_global
        plants_modified += file_modified

        # Guardar si hay cambios
        if (file_internal > 0 or file_global > 0) and not dry_run:
            with open(plant_file, "w", encoding="utf-8") as f:
                json.dump(plants, f, ensure_ascii=False, indent=2)
            print(f"   ✅ Guardado - {file_internal} duplicados internos + {file_global} globales eliminados")
        elif dry_run and (file_internal > 0 or file_global > 0):
            print(f"   [DRY-RUN] Se eliminarían {file_internal} internos + {file_global} globales")

    # Resumen final
    print("\n" + "=" * 75)
    print("✅ LIMPIEZA DE URLs FINALIZADA")
    print(f"   Total plantas procesadas:        {total_plants}")
    print(f"   Plantas modificadas:             {plants_modified}")
    print(f"   Duplicados internos eliminados:  {total_internal}")
    if global_mode:
        print(f"   Duplicados globales eliminados:  {total_global}")
    print(f"   Archivos procesados:             {len(plant_files)}")
    print(f"   Máximo URLs por planta:          {max_urls}")
    print("=" * 75)

    if not dry_run:
        print(f"\n💾 Backup guardado en: {BACKUP_DIR}")
        print("   Puedes restaurarlo si algo sale mal.")

# ============================================================
# MAIN
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="Limpia URLs repetidas en los JSONs de plantas (versión avanzada)"
    )
    parser.add_argument("--dry-run", action="store_true",
                        help="Muestra qué haría sin modificar nada")
    parser.add_argument("--global", dest="global_mode", action="store_true",
                        help="Elimina URLs que se repiten entre diferentes plantas")
    parser.add_argument("--max", type=int, default=3,
                        help="Máximo de URLs por planta (por defecto 3)")

    args = parser.parse_args()

    if args.dry_run:
        print("🔍 MODO DRY-RUN activado (no se modificarán archivos)")
    if args.global_mode:
        print("🌍 MODO GLOBAL activado (se eliminarán URLs repetidas entre especies)")

    process_all_files(
        dry_run=args.dry_run,
        global_mode=args.global_mode,
        max_urls=args.max
    )

if __name__ == "__main__":
    main()