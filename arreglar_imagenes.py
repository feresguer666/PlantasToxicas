#!/usr/bin/env python3
import json
import re
import time
import shutil
import urllib.parse
import urllib.request
from pathlib import Path

ARCHIVO = Path("/home/fer/AndroidStudioProjects/PlantasToxicas/app/src/main/java/com/toxicplants/database/PlantDataSource.kt")
BACKUP = ARCHIVO.with_suffix(".kt.bak")
REPORTE = Path("/home/fer/AndroidStudioProjects/PlantasToxicas/imagenes_pendientes.txt")

re_common = re.compile(r'commonName\s*=\s*"([^"]*)"')
re_scientific = re.compile(r'scientificName\s*=\s*"([^"]*)"')
re_image = re.compile(r'(imageUrl\s*=\s*")([^"]*)(")')

re_wikimedia_thumb = re.compile(
    r'^https?://upload\.wikimedia\.org/wikipedia/commons/thumb/([^/]+)/([^/]+)/([^/]+)/[^"]+$'
)

cache = {}

def normalizar_wikimedia(url: str) -> str:
    m = re_wikimedia_thumb.match(url.strip())
    if not m:
        return url.strip()
    h1, h2, archivo = m.groups()
    return f"https://upload.wikimedia.org/wikipedia/commons/{h1}/{h2}/{archivo}"

def es_url_mala(url: str) -> bool:
    u = url.strip().lower()
    if not u:
        return True
    if "encrypted-tbn0.gstatic.com" in u:
        return True
    if "gstatic.com" in u and "wikimedia" not in u:
        return True
    if "googleusercontent.com" in u:
        return True
    return False

def buscar_imagen_wikipedia(nombre: str):
    if not nombre:
        return None

    nombre = nombre.strip()
    if not nombre:
        return None

    if nombre in cache:
        return cache[nombre]

    headers = {
        "User-Agent": "PlantasToxicasFixer/1.0"
    }

    titulo = nombre.replace(" ", "_")

    for lang in ("es", "en"):
        url = f"https://{lang}.wikipedia.org/api/rest_v1/page/summary/{urllib.parse.quote(titulo, safe='_()')}"
        req = urllib.request.Request(url, headers=headers)

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.load(resp)

            imagen = None
            if "originalimage" in data and "source" in data["originalimage"]:
                imagen = data["originalimage"]["source"]
            elif "thumbnail" in data and "source" in data["thumbnail"]:
                imagen = data["thumbnail"]["source"]

            if imagen:
                imagen = normalizar_wikimedia(imagen)
                cache[nombre] = imagen
                time.sleep(0.2)
                return imagen

        except Exception:
            pass

        time.sleep(0.2)

    cache[nombre] = None
    return None

def main():
    if not ARCHIVO.exists():
        print(f"No existe: {ARCHIVO}")
        return

    shutil.copy2(ARCHIVO, BACKUP)
    print(f"Backup creado: {BACKUP}")

    lineas = ARCHIVO.read_text(encoding="utf-8").splitlines(keepends=True)

    nuevas = []
    common_name = None
    scientific_name = None

    total = 0
    cambiadas = 0
    pendientes = []

    for linea in lineas:
        m = re_common.search(linea)
        if m:
            common_name = m.group(1)

        m = re_scientific.search(linea)
        if m:
            scientific_name = m.group(1)

        m = re_image.search(linea)
        if m:
            total += 1
            url_original = m.group(2).strip()
            url_nueva = url_original

            url_nueva = normalizar_wikimedia(url_nueva)

            if es_url_mala(url_nueva):
                url_api = buscar_imagen_wikipedia(scientific_name) or buscar_imagen_wikipedia(common_name)
                if url_api:
                    url_nueva = url_api

            if es_url_mala(url_nueva):
                pendientes.append(f"{scientific_name or common_name} -> {url_original}")
                url_nueva = ""

            if url_nueva != url_original:
                linea = linea[:m.start(2)] + url_nueva + linea[m.end(2):]
                cambiadas += 1
                print(f"[OK] {scientific_name or common_name}")
                print(f"     vieja: {url_original}")
                print(f"     nueva: {url_nueva}")

        nuevas.append(linea)

    ARCHIVO.write_text("".join(nuevas), encoding="utf-8")
    REPORTE.write_text("\n".join(pendientes), encoding="utf-8")

    print()
    print(f"Total imageUrl: {total}")
    print(f"Cambiadas: {cambiadas}")
    print(f"Pendientes sin resolver: {len(pendientes)}")
    print(f"Reporte: {REPORTE}")
    print("Hecho.")

if __name__ == "__main__":
    main()
