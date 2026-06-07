import os
import json
import requests
import time
import sys
from pathlib import Path

# Configuración
ASSETS_DIR = "app/src/main/assets"
IMAGES_DIR = os.path.join(ASSETS_DIR, "generated_images")
os.makedirs(IMAGES_DIR, exist_ok=True)

# CABECERAS PARA EVITAR BLOQUEOS
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
}

def clean_scientific_name(name):
    if not name: return ""
    parts = name.split()
    if len(parts) >= 2:
        return f"{parts[0]} {parts[1]}"
    return name

def safe_request(url, retries=3):
    """Realiza una petición HTTP con reintentos en caso de error."""
    for i in range(retries):
        try:
            response = requests.get(url, headers=HEADERS, timeout=15)
            if response.status_code == 200:
                # Verificamos que el contenido sea JSON antes de parsear
                if 'application/json' in response.headers.get('Content-Type', '').lower():
                    return response.json()
                return response # Devolvemos la respuesta raw si no es JSON
            elif response.status_code == 429: # Too Many Requests
                print(f"   ⏳ Rate limit detectado. Esperando { (i+1)*5 } seg...")
                time.sleep((i + 1) * 5)
        except Exception as e:
            print(f"   ⚠️ Intento {i+1} fallido: {e}")
            time.sleep(2)
    return None

def find_real_image(scientific_name):
    if not scientific_name or scientific_name == "None":
        return None

    clean_name = clean_scientific_name(scientific_name)
    
    # 1. Wikimedia Commons
    try:
        search_url = f"https://commons.wikimedia.org/w/api.php?action=query&list=search&srsearch={clean_name.replace(' ', '_')}&format=json&srlimit=1"
        res_data = safe_request(search_url)
        
        if res_data and isinstance(res_data, dict):
            search_results = res_data.get('query', {}).get('search', [])
            if search_results:
                title = search_results[0]['title']
                img_url = f"https://en.wikipedia.org/w/api.php?action=query&titles={title}&prop=pageimages&format=json&pithumbsize=1000"
                img_data = safe_request(img_url)
                if img_data and isinstance(img_data, dict):
                    pages = img_data.get('query', {}).get('pages', {})
                    for p_id in pages:
                        thumb = pages[p_id].get('thumbnail')
                        if thumb:
                            return thumb['source']
    except Exception:
        pass

    # 2. iNaturalist
    try:
        inat_url = f"https://api.inaturalist.org/v1/search?q={clean_name}&sources=taxa&per_page=1"
        res_data = safe_request(inat_url)
        if res_data and isinstance(res_data, dict):
            results = res_data.get('results', [])
            if results:
                taxon = results[0].get('taxon', {})
                photo = taxon.get('default_photo')
                if photo:
                    return photo.get('medium_url')
    except Exception:
        pass

    return None

def process_json(filename):
    path = os.path.join(ASSETS_DIR, filename)
    if not os.path.exists(path):
        print(f"❌ Error: El archivo {filename} no existe.")
        return

    print(f"🚀 Iniciando búsqueda masiva robusta para: {filename}")
    
    with open(path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except:
            print("❌ Error JSON.")
            return

    plants = data if isinstance(data, list) else data.get('plants', [])
    total = len(plants)
    count_success = 0

    for i, plant in enumerate(plants, 1):
        sci_name = plant.get('scientificName')
        print(f"[{i}/{total}] {sci_name}...", end=" ")
        
        img_url = find_real_image(sci_name)
        
        if img_url:
            plant_id = plant.get('id', 'unknown')
            file_name = f"plant_{plant_id}.jpg"
            file_path = os.path.join(IMAGES_DIR, file_name)
            
            try:
                img_data = requests.get(img_url, headers=HEADERS, timeout=20).content
                with open(file_path, 'wb') as img_file:
                    img_file.write(img_data)
                
                plant['imageUrl'] = f"file:///android_asset/generated_images/{file_name}"
                count_success += 1
                print("✅ OK")
            except Exception as e:
                print(f"❌ Error guardando: {e}")
        else:
            print("⚠️ No encontrada.")
        
        # PAUSA SEGURA: 1.5 segundos para evitar el baneo de Wikimedia
        time.sleep(1.5)

    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    
    print(f"\n✨ Finalizado: {count_success}/{total} imágenes actualizadas en {filename}.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python3 auto_fotos_reales.py <archivo.json>")
    else:
        process_json(sys.argv[1])

