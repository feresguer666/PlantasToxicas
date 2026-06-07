#!/usr/bin/env python3
"""
=======================================================================
  rellenar_imagenes_json.py  —  PlantasToxicas
=======================================================================
Rellena el campo "imageUrl" de todos los archivos plants_1.json …
plants_8.json buscando fotos reales en múltiples fuentes:

  1. Wikipedia REST API (en/es)
  2. MediaWiki API (pageimages)
  3. Wikimedia Commons
  4. iNaturalist API
  5. Encyclopedia of Life (EOL)

Uso:
    pip install requests
    python rellenar_imagenes_json.py

Coloca este script en la RAÍZ del proyecto (junto a build.gradle.kts).
Los JSON están en:
    app/src/main/assets/plants_1.json  …  plants_8.json

El script modifica los archivos en su sitio. Haz git commit después.
=======================================================================
"""

import json
import time
import os
import sys
import re
from pathlib import Path

try:
    import requests
except ImportError:
    print("❌  Falta la librería 'requests'. Instálala con:  pip install requests")
    sys.exit(1)

# ─────────────────────────── Configuración ───────────────────────────
ASSETS_DIR = Path("app/src/main/assets")
JSON_FILES  = [f"plants_{i}.json" for i in range(1, 9)]

HEADERS = {"User-Agent": "PlantasToxicasApp/2.0 (educational; contact: github.com/feresguer666)"}
TIMEOUT = 12          # segundos por petición
DELAY   = 0.4         # pausa entre peticiones para no hacer spam

# URLs que se consideran "vacías" o inválidas → hay que rellenarlas
INVALID_URL_PATTERNS = [
    "https://wikimedia.org",      # placeholder sin imagen real
    "https://www.wikimedia.org",
    "",
]

# ─────────────────────── Funciones de búsqueda ───────────────────────

def is_invalid_url(url: str) -> bool:
    """Devuelve True si la URL es un placeholder o está vacía."""
    url = url.strip()
    if not url:
        return True
    for pat in INVALID_URL_PATTERNS:
        if url.rstrip("/") == pat.rstrip("/"):
            return True
    # URL demasiado corta para ser real
    if len(url) < 20:
        return True
    return False


def normalize_url(url: str) -> str:
    """
    Convierte URLs de thumbnail de Wikipedia en URL directa de imagen.
    Ej: .../thumb/a/b/Foto.jpg/300px-Foto.jpg → .../a/b/Foto.jpg
    """
    if not url:
        return url
    # Eliminar parte /thumb/ y el sufijo de tamaño
    thumb_pat = re.compile(
        r"(https://upload\.wikimedia\.org/wikipedia/(?:commons|[a-z]{2}))"
        r"/thumb(/[^/]+/[^/]+/[^/]+)/\d+px-[^/]+$"
    )
    m = thumb_pat.match(url)
    if m:
        return m.group(1) + m.group(2)
    return url


# ── Fuente 1: Wikipedia REST API ──────────────────────────────────────
def from_wikipedia_rest(name: str, lang: str = "en") -> str:
    slug = name.strip().replace(" ", "_")
    url  = f"https://{lang}.wikipedia.org/api/rest_v1/page/summary/{slug}"
    try:
        r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
        if r.status_code != 200:
            return ""
        data = r.json()
        for key in ("originalimage", "thumbnail"):
            if key in data:
                src = data[key].get("source", "")
                if src:
                    return normalize_url(src)
    except Exception:
        pass
    return ""


# ── Fuente 2: MediaWiki API (pageimages) ─────────────────────────────
def from_mediawiki_pageimages(name: str, lang: str = "en") -> str:
    params = {
        "action": "query",
        "titles": name,
        "prop": "pageimages",
        "pithumbsize": 800,
        "format": "json",
    }
    try:
        r = requests.get(
            f"https://{lang}.wikipedia.org/w/api.php",
            params=params, headers=HEADERS, timeout=TIMEOUT
        )
        data = r.json()
        pages = data.get("query", {}).get("pages", {})
        for page in pages.values():
            thumb = page.get("thumbnail", {}).get("source", "")
            if thumb:
                return normalize_url(thumb)
    except Exception:
        pass
    return ""


# ── Fuente 3: Wikimedia Commons ───────────────────────────────────────
def from_wikimedia_commons(name: str) -> str:
    # Buscar en Commons
    params = {
        "action": "query",
        "list": "search",
        "srsearch": name,
        "srnamespace": 6,      # File:
        "format": "json",
        "srlimit": 3,
    }
    try:
        r = requests.get(
            "https://commons.wikimedia.org/w/api.php",
            params=params, headers=HEADERS, timeout=TIMEOUT
        )
        data = r.json()
        results = data.get("query", {}).get("search", [])
        for result in results:
            title = result.get("title", "")
            if title.startswith("File:"):
                # Obtener URL directa del archivo
                file_url = get_commons_file_url(title)
                if file_url:
                    return file_url
    except Exception:
        pass
    return ""


def get_commons_file_url(file_title: str) -> str:
    """Obtiene la URL directa de un archivo de Wikimedia Commons."""
    params = {
        "action": "query",
        "titles": file_title,
        "prop": "imageinfo",
        "iiprop": "url",
        "format": "json",
    }
    try:
        r = requests.get(
            "https://commons.wikimedia.org/w/api.php",
            params=params, headers=HEADERS, timeout=TIMEOUT
        )
        data = r.json()
        pages = data.get("query", {}).get("pages", {})
        for page in pages.values():
            info = page.get("imageinfo", [])
            if info:
                url = info[0].get("url", "")
                if url:
                    return url
    except Exception:
        pass
    return ""


# ── Fuente 4: iNaturalist API ─────────────────────────────────────────
def from_inaturalist(scientific_name: str) -> str:
    params = {
        "q": scientific_name,
        "sources": "taxa",
        "per_page": 5,
    }
    try:
        r = requests.get(
            "https://api.inaturalist.org/v1/search",
            params=params, headers=HEADERS, timeout=TIMEOUT
        )
        data = r.json()
        results = data.get("results", [])
        for result in results:
            taxon = result.get("taxon", {})
            # Verificar que el nombre coincida (evitar falsos positivos)
            taxon_name = taxon.get("name", "").lower()
            search_genus = scientific_name.split()[0].lower()
            if search_genus not in taxon_name:
                continue
            photo = taxon.get("default_photo", {})
            url = photo.get("medium_url", "") or photo.get("square_url", "")
            if url:
                return url
    except Exception:
        pass
    return ""


# ── Fuente 5: Encyclopedia of Life ────────────────────────────────────
def from_eol(scientific_name: str) -> str:
    try:
        # Buscar el taxón
        r = requests.get(
            "https://eol.org/api/search/1.0.json",
            params={"q": scientific_name, "page": 1, "exact": False},
            headers=HEADERS, timeout=TIMEOUT
        )
        data = r.json()
        results = data.get("results", [])
        if not results:
            return ""
        taxon_id = results[0].get("id")
        if not taxon_id:
            return ""

        # Obtener página del taxón con imágenes
        r2 = requests.get(
            f"https://eol.org/api/pages/1.0/{taxon_id}.json",
            params={"images_per_page": 1, "language": "es"},
            headers=HEADERS, timeout=TIMEOUT
        )
        data2 = r2.json()
        data_objects = data2.get("taxonConcept", {}).get("dataObjects", [])
        for obj in data_objects:
            if obj.get("dataType", "").endswith("StillImage"):
                url = obj.get("mediaURL", "")
                if url:
                    return url
    except Exception:
        pass
    return ""


# ─────────────────────── Función principal de búsqueda ───────────────

def search_image(scientific_name: str, common_name: str) -> str:
    """
    Busca una imagen para la planta usando múltiples fuentes en orden.
    Devuelve la primera URL válida encontrada, o "" si no hay ninguna.
    """
    sources = []

    # — Wikipedia en inglés con nombre científico —
    sources.append(lambda: from_wikipedia_rest(scientific_name, "en"))
    # — Wikipedia en español con nombre científico —
    sources.append(lambda: from_wikipedia_rest(scientific_name, "es"))
    # — MediaWiki en inglés —
    sources.append(lambda: from_mediawiki_pageimages(scientific_name, "en"))
    # — MediaWiki en español —
    sources.append(lambda: from_mediawiki_pageimages(scientific_name, "es"))
    # — Wikimedia Commons con nombre científico —
    sources.append(lambda: from_wikimedia_commons(scientific_name))
    # — iNaturalist con nombre científico —
    sources.append(lambda: from_inaturalist(scientific_name))
    # — Wikipedia en inglés con nombre común —
    if common_name and common_name.lower() != scientific_name.lower():
        sources.append(lambda: from_wikipedia_rest(common_name, "en"))
        sources.append(lambda: from_wikipedia_rest(common_name, "es"))
        sources.append(lambda: from_wikimedia_commons(common_name))
    # — Solo género (primera palabra) —
    genus = scientific_name.split()[0] if scientific_name else ""
    if genus and genus.lower() != scientific_name.lower():
        sources.append(lambda: from_wikipedia_rest(genus, "en"))
        sources.append(lambda: from_inaturalist(genus))
    # — EOL —
    sources.append(lambda: from_eol(scientific_name))

    for source_fn in sources:
        try:
            url = source_fn()
            if url and len(url) > 20:
                return url
        except Exception:
            pass
        time.sleep(DELAY)

    return ""


# ─────────────────────────── Proceso principal ───────────────────────

def process_file(filepath: Path) -> tuple[int, int, int]:
    """
    Procesa un archivo JSON de plantas.
    Devuelve (total, updated, skipped).
    """
    print(f"\n{'='*60}")
    print(f"  Procesando: {filepath.name}")
    print(f"{'='*60}")

    with open(filepath, encoding="utf-8") as f:
        plants = json.load(f)

    total   = len(plants)
    updated = 0
    skipped = 0

    for i, plant in enumerate(plants, 1):
        pid         = plant.get("id", "?")
        common      = plant.get("commonName", "")
        scientific  = plant.get("scientificName", "")
        current_url = plant.get("imageUrl", "")

        prefix = f"  [{i:4d}/{total}] {common[:30]:30s}"

        if not is_invalid_url(current_url):
            print(f"{prefix} ✅ ya tiene URL")
            skipped += 1
            continue

        print(f"{prefix} 🔍 buscando imagen...", end="", flush=True)

        new_url = search_image(scientific, common)

        if new_url:
            plant["imageUrl"] = new_url
            updated += 1
            short = new_url[:60] + "…" if len(new_url) > 60 else new_url
            print(f" ✅ {short}")
        else:
            print(f" ❌ no encontrada")

        # Guardar progreso parcial cada 50 plantas (por si se interrumpe)
        if i % 50 == 0:
            with open(filepath, "w", encoding="utf-8") as f:
                json.dump(plants, f, ensure_ascii=False, indent=2)
            print(f"  💾 Guardado parcial: {i}/{total}")

    # Guardar al final
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(plants, f, ensure_ascii=False, indent=2)

    return total, updated, skipped


def main():
    if not ASSETS_DIR.exists():
        print(f"❌ No se encontró el directorio: {ASSETS_DIR}")
        print("   Ejecuta el script desde la RAÍZ del proyecto.")
        sys.exit(1)

    total_plants   = 0
    total_updated  = 0
    total_skipped  = 0
    total_missing  = 0

    files_found = []
    for json_file in JSON_FILES:
        path = ASSETS_DIR / json_file
        if path.exists():
            files_found.append(path)
        else:
            print(f"⚠️  No encontrado: {path}")

    if not files_found:
        print("❌ No se encontró ningún archivo plants_N.json")
        sys.exit(1)

    print(f"\n🌿 PlantasToxicas — Relleno de imágenes")
    print(f"   Archivos encontrados: {len(files_found)}")
    print(f"   Directorio: {ASSETS_DIR.resolve()}")

    for filepath in files_found:
        t, u, s = process_file(filepath)
        missing = t - u - s
        total_plants  += t
        total_updated += u
        total_skipped += s
        total_missing += missing

    print(f"\n{'='*60}")
    print(f"  RESUMEN FINAL")
    print(f"{'='*60}")
    print(f"  Total plantas procesadas : {total_plants}")
    print(f"  ✅ Ya tenían imagen       : {total_skipped}")
    print(f"  🆕 Imagen encontrada     : {total_updated}")
    print(f"  ❌ Sin imagen            : {total_missing}")
    print(f"{'='*60}")
    print(f"\n✅ Listo. Haz commit de los archivos JSON actualizados.")
    print(f"   git add app/src/main/assets/plants_*.json")
    print(f"   git commit -m 'feat: imageUrl rellenada automáticamente'")


if __name__ == "__main__":
    main()
