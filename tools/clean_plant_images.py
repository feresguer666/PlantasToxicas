#!/usr/bin/env python3
"""
clean_plant_images.py
=====================
Limpia fotos duplicadas en la carpeta downloaded_galleries/

Problema que resuelve:
- Algunas plantas (como Abeto blanco) tienen 22 fotos en vez de 3
- Este script deja solo las 3 primeras fotos de cada planta

Uso:
    python3 tools/clean_plant_images.py
"""

from pathlib import Path
import os

OUTPUT_DIR = Path("downloaded_galleries")

def clean_galleries():
    if not OUTPUT_DIR.exists():
        print("❌ No existe la carpeta downloaded_galleries/")
        return

    files = list(OUTPUT_DIR.glob("plant_*.jpg"))
    print(f"📁 Encontradas {len(files)} fotos en total")

    # Agrupar por ID de planta
    plant_files = {}
    for f in files:
        # Extraer el ID de la planta del nombre del archivo
        name = f.name
        if name.startswith("plant_"):
            try:
                # plant_123.jpg o plant_123_extra_1.jpg
                parts = name.replace(".jpg", "").split("_")
                plant_id = int(parts[1])
                if plant_id not in plant_files:
                    plant_files[plant_id] = []
                plant_files[plant_id].append(f)
            except:
                continue

    print(f"🌿 Plantas con fotos: {len(plant_files)}")

    deleted = 0
    kept = 0

    for plant_id, photo_list in plant_files.items():
        # Ordenar: principal primero, luego extras
        photo_list.sort(key=lambda x: (
            0 if x.name == f"plant_{plant_id}.jpg" else 1,
            x.name
        ))

        # Mantener solo las primeras 3
        to_keep = photo_list[:3]
        to_delete = photo_list[3:]

        for f in to_keep:
            kept += 1

        for f in to_delete:
            try:
                f.unlink()
                deleted += 1
                print(f"🗑️  Eliminada: {f.name}")
            except Exception as e:
                print(f"⚠️  Error eliminando {f.name}: {e}")

    print("\n" + "=" * 50)
    print("✅ LIMPIEZA FINALIZADA")
    print(f"   Fotos conservadas: {kept}")
    print(f"   Fotos eliminadas:  {deleted}")
    print("=" * 50)

if __name__ == "__main__":
    clean_galleries()