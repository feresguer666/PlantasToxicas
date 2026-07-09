#!/usr/bin/env python3
"""
download_galleries.py - VERSIÓN AVANZADA CON CONTROL GLOBAL DE DUPLICADOS
========================================================================

Esta versión evita que la misma foto se descargue para diferentes especies.

Características principales:
- Control global de URLs (no usa la misma URL en varias plantas)
- Control por hash de imagen (detecta fotos idénticas aunque vengan de URLs diferentes)
- Guarda el estado para reanudar ejecuciones
- Muestra cuántas repeticiones ha evitado
- Mucho más eficiente

Uso recomendado:
    python3 tools/download_galleries.py --all --target 3 --workers 10
"""

import argparse
import json
import os
import sys
import time
import hashlib
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Set

# ============================================================
# CONFIGURACIÓN
# ============================================================

ASSETS_DIR = Path("app/src/main/assets")
OUTPUT_DIR = Path("downloaded_galleries")

# Archivos de control de duplicados
USED_URLS_FILE = Path("used_image_urls.json")
USED_HASHES_FILE = Path("used_image_hashes.json")

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
}

# ============================================================
# ESTADO GLOBAL DE DUPLICADOS
# ============================================================

used_urls: Set[str] = set()
used_hashes: Set[str] = set()

def load_used_data():
    """Carga los datos de URLs y hashes ya usados (para reanudar)."""
    global used_urls, used_hashes

    if USED_URLS_FILE.exists():
        try:
            with open(USED_URLS_FILE, "r", encoding="utf-8") as f:
                used_urls = set(json.load(f))
            print(f"📋 Cargadas {len(used_urls)} URLs ya usadas")
        except:
            used_urls = set()

    if USED_HASHES_FILE.exists():
        try:
            with open(USED_HASHES_FILE, "r", encoding="utf-8") as f:
                used_hashes = set(json.load(f))
            print(f"📋 Cargados {len(used_hashes)} hashes de imágenes")
        except:
            used_hashes = set()

def save_used_data():
    """Guarda el estado actual de duplicados."""
    try:
        with open(USED_URLS_FILE, "w", encoding="utf-8") as f:
            json.dump(list(used_urls), f, indent=2)
        with open(USED_HASHES_FILE, "w", encoding="utf-8") as f:
            json.dump(list(used_hashes), f, indent=2)
    except Exception as e:
        print(f"⚠️  No se pudo guardar el estado de duplicados: {e}")

def normalize_url(url: str) -> str:
    """Normaliza una URL para comparación."""
    return url.split("?")[0].lower().strip()

def get_image_hash(data: bytes) -> str:
    """Calcula el hash MD5 del contenido de la imagen."""
    return hashlib.md5(data).hexdigest()

# ============================================================
# FUNCIONES AUXILIARES
# ============================================================

def parse_image_urls(image_url: str) -> List[str]:
    """Separa las URLs y elimina duplicados dentro de la misma planta."""
    if not image_url:
        return []
    urls = [u.strip() for u in image_url.split("|") if u.strip()]
    seen = set()
    unique = []
    for url in urls:
        if url not in seen:
            seen.add(url)
            unique.append(url)
    return unique

def get_existing_photos(plant_id: int) -> List[Path]:
    if not OUTPUT_DIR.exists():
        return []
    return sorted(OUTPUT_DIR.glob(f"plant_{plant_id}*.jpg"))

def has_enough_photos(plant_id: int, target: int) -> bool:
    photos = get_existing_photos(plant_id)
    good = [p for p in photos if p.stat().st_size > 8000]
    return len(good) >= target

def get_existing_count(plant_id: int) -> int:
    photos = get_existing_photos(plant_id)
    return len([p for p in photos if p.stat().st_size > 8000])

# ============================================================
# DESCARGA CON CONTROL DE DUPLICADOS
# ============================================================

def download_image_safe(url: str, dest_path: Path, timeout: int = 25) -> Tuple[bool, Optional[str]]:
    """
    Descarga una imagen comprobando duplicados globales.
    Devuelve (éxito, hash_de_la_imagen)
    """
    normalized = normalize_url(url)

    # 1. Comprobar si la URL ya fue usada globalmente
    if normalized in used_urls:
        return False, None

    try:
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req, timeout=timeout) as response:
            if response.status != 200:
                return False, None

            data = response.read()
            if len(data) < 8000:
                return False, None

            # 2. Calcular hash de la imagen
            img_hash = get_image_hash(data)

            # 3. Comprobar si ya existe una imagen idéntica
            if img_hash in used_hashes:
                return False, None

            # 4. Guardar la imagen
            dest_path.parent.mkdir(parents=True, exist_ok=True)
            with open(dest_path, "wb") as f:
                f.write(data)

            # 5. Registrar como usada
            used_urls.add(normalized)
            used_hashes.add(img_hash)

            return True, img_hash

    except Exception:
        return False, None

def download_gallery_for_plant(plant: Dict, target: int = 3, force: bool = False) -> Dict:
    plant_id = plant.get("id")
    if not plant_id:
        return {"id": 0, "downloaded": 0, "skipped": 0, "duplicates_avoided": 0}

    name = plant.get("commonName") or plant.get("scientificName", "Sin nombre")

    if not force and has_enough_photos(plant_id, target):
        return {
            "id": plant_id,
            "name": name,
            "downloaded": 0,
            "skipped": 1,
            "duplicates_avoided": 0,
            "existing": get_existing_count(plant_id)
        }

    urls = parse_image_urls(plant.get("imageUrl", ""))
    downloaded = 0
    skipped = 0
    duplicates_avoided = 0

    for idx, url in enumerate(urls[:target]):
        if idx == 0:
            filename = f"plant_{plant_id}.jpg"
        else:
            filename = f"plant_{plant_id}_extra_{idx}.jpg"

        dest = OUTPUT_DIR / filename

        if dest.exists() and dest.stat().st_size > 8000 and not force:
            skipped += 1
            continue

        success, img_hash = download_image_safe(url, dest)
        if success:
            downloaded += 1
        else:
            skipped += 1
            # Si falló por duplicado, lo contamos
            if normalize_url(url) in used_urls:
                duplicates_avoided += 1

    return {
        "id": plant_id,
        "name": name,
        "downloaded": downloaded,
        "skipped": skipped,
        "duplicates_avoided": duplicates_avoided,
        "existing": get_existing_count(plant_id)
    }

# ============================================================
# PROCESAMIENTO PRINCIPAL
# ============================================================

def process_all_plants(target: int, workers: int, specific_file: Optional[str] = None, force: bool = False):
    OUTPUT_DIR.mkdir(exist_ok=True)
    load_used_data()

    if specific_file:
        plant_files = [ASSETS_DIR / specific_file]
    else:
        plant_files = sorted(ASSETS_DIR.glob("plants_*.json"))

    all_plants = []
    for pf in plant_files:
        try:
            with open(pf, "r", encoding="utf-8") as f:
                data = json.load(f)
            plants = data if isinstance(data, list) else []
            all_plants.extend(plants)
            print(f"📄 {pf.name}: {len(plants)} plantas")
        except Exception as e:
            print(f"⚠️  Error leyendo {pf.name}: {e}")

    print(f"\n🌿 Total plantas: {len(all_plants)}")
    print(f"🎯 Fotos por planta: {target}")
    print(f"🧵 Hilos: {workers}")
    if force:
        print("⚠️  MODO FORZADO (se ignoran duplicados previos)")
    print("-" * 75)

    start_time = time.time()
    total_downloaded = 0
    total_skipped = 0
    total_duplicates_avoided = 0
    processed = 0

    with ThreadPoolExecutor(max_workers=workers) as executor:
        future_to_plant = {
            executor.submit(download_gallery_for_plant, plant, target, force): plant
            for plant in all_plants
        }

        for i, future in enumerate(as_completed(future_to_plant), 1):
            try:
                result = future.result()
                processed += 1

                total_downloaded += result["downloaded"]
                total_skipped += result["skipped"]
                total_duplicates_avoided += result.get("duplicates_avoided", 0)

                if i % 150 == 0 or i == len(all_plants):
                    elapsed = time.time() - start_time
                    print(f"[{i:5d}/{len(all_plants)}] "
                          f"✅ {total_downloaded:5d} nuevas | "
                          f"🚫 {total_duplicates_avoided:5d} duplicados evitados | "
                          f"⏱️ {elapsed:.0f}s")

            except Exception as e:
                print(f"❌ Error: {e}")

    # Guardar estado
    save_used_data()

    total_time = time.time() - start_time

    print("\n" + "=" * 75)
    print("✅ DESCARGA DE GALERÍAS FINALIZADA")
    print(f"   Plantas procesadas:           {processed}")
    print(f"   Fotos nuevas descargadas:     {total_downloaded}")
    print(f"   Duplicados evitados:          {total_duplicates_avoided}")
    print(f"   Fotos saltadas (ya existían): {total_skipped}")
    print(f"   Tiempo total:                 {total_time:.1f} segundos")
    print(f"   Velocidad:                    {processed/total_time:.1f} plantas/seg")
    print(f"   Carpeta destino:              {OUTPUT_DIR.resolve()}")
    print("=" * 75)

    print("\n📊 ESTADO ACTUAL DE DUPLICADOS:")
    print(f"   URLs únicas usadas:  {len(used_urls)}")
    print(f"   Imágenes únicas:     {len(used_hashes)}")

    print("\n📋 PRÓXIMOS PASOS:")
    print("1. Revisa la carpeta downloaded_galleries/")
    print("2. Copia al móvil:")
    print("   adb push downloaded_galleries/ /sdcard/Download/plant_images/")
    print("3. En la app usa el botón 'Descargar galerías'")

# ============================================================
# MAIN
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="Descarga galerías sin duplicados entre especies"
    )
    parser.add_argument("--all", action="store_true", help="Todos los archivos plants_*.json")
    parser.add_argument("--file", type=str, help="Solo un archivo (ej: plants_5.json)")
    parser.add_argument("--target", type=int, default=3, help="Fotos por planta")
    parser.add_argument("--workers", type=int, default=10, help="Hilos de descarga")
    parser.add_argument("--force", action="store_true", help="Forzar re-descarga")

    args = parser.parse_args()

    if not args.all and not args.file:
        parser.print_help()
        print("\nEjemplos:")
        print("  python3 tools/download_galleries.py --all --target 3 --workers 10")
        sys.exit(1)

    process_all_plants(
        target=args.target,
        workers=args.workers,
        specific_file=args.file,
        force=args.force
    )

if __name__ == "__main__":
    main()