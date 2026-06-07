#!/usr/bin/env python3
import json
import re
import time
import shutil
import urllib.parse
import urllib.request
from pathlib import Path

# Ahora apuntamos al archivo JSON correcto
ARCHIVO = Path("app/src/main/assets/plants.json")
BACKUP = ARCHIVO.with_suffix(".json.bak")
REPORTE = Path("imagenes_pendientes.txt")

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
    if not u: # Si la URL está en blanco, ES MALA y debe buscarse
        return True
    if u == "https://wikimedia.org" or u == "https://wikimedia.org/":
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
        "User-Agent": "PlantasToxicasFixer/1.1 (tu_correo@ejemplo.com)"
    }

    titulo = nombre.replace(" ", "_")

    for lang in ("es", "en"):
        url = f"https://{lang}.wikipedia.org/api/rest_v1/page/summary/{urllib.parse.quote(titulo, safe='_()')}"
        req = urllib.request.Request(url, headers=headers)

        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                data = json.load(resp)

            imagen = None
            if "originalimage" in data and "source" in data["originalimage"]:
                imagen = data["originalimage"]["source"]
            elif "thumbnail" in data and "source" in data["thumbnail"]:
                imagen = data["thumbnail"]["source"]

            if imagen:
                imagen = normalizar_wikimedia(imagen)
                cache[nombre] = imagen
                time.sleep(0.1) # Pausa ligera para no saturar la API
                return imagen

        except Exception:
            pass
            
        time.sleep(0.1)

    cache[nombre] = None
    return None

def main():
    if not ARCHIVO.exists():
        print(f"No existe: {ARCHIVO}")
        return

    # Hacer backup
    shutil.copy2(ARCHIVO, BACKUP)
    print(f"Backup creado en: {BACKUP}")

    # Leer el JSON
    with open(ARCHIVO, "r", encoding="utf-8") as f:
        plantas = json.load(f)

    total = len(plantas)
    cambiadas = 0
    pendientes = []

    print("Buscando imágenes...")
    for planta in plantas:
        url_original = planta.get("imageUrl", "").strip()
        scientific = planta.get("scientificName", "")
        common = planta.get("commonName", "")
        
        url_nueva = normalizar_wikimedia(url_original)

        if es_url_mala(url_nueva):
            # Intentar buscar en Wikipedia primero por nombre científico, luego común
            url_api = buscar_imagen_wikipedia(scientific) or buscar_imagen_wikipedia(common)
            if url_api:
                url_nueva = url_api

        if es_url_mala(url_nueva):
            pendientes.append(f"{scientific or common} -> {url_original}")
            # Opcional: podrías poner url_nueva = "" si prefieres vaciar la imagen rota
        
        if url_nueva != url_original:
            planta["imageUrl"] = url_nueva
            cambiadas += 1
            print(f"[OK] {scientific or common}")

    # Guardar cambios
    with open(ARCHIVO, "w", encoding="utf-8") as f:
        json.dump(plantas, f, indent=1, ensure_ascii=False)

    REPORTE.write_text("\n".join(pendientes), encoding="utf-8")

    print("\n--- RESUMEN ---")
    print(f"Total plantas analizadas: {total}")
    print(f"Imágenes actualizadas: {cambiadas}")
    print(f"Pendientes sin resolver (Url mala o no encontradas): {len(pendientes)}")
    print("Hecho.")

if __name__ == "__main__":
    main()
