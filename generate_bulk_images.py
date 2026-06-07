import os
import json
import requests
import time
from pathlib import Path

# Configuración
ASSETS_DIR = "app/src/main/assets"
IMAGES_DIR = os.path.join(ASSETS_DIR, "generated_images")
JSON_FILES = [f for f in os.listdir(ASSETS_DIR) if f.endswith('.json')]

# Crear carpeta de imágenes si no existe
os.makedirs(IMAGES_DIR, exist_ok=True)

def generate_prompt(plant):
    sci_name = plant.get('scientificName', 'Unknown plant')
    family = plant.get('family', 'Unknown family')
    common_name = plant.get('commonName', 'Plant')
    return f"Scientific botanical illustration of {sci_name} ({common_name}), family {family}, detailed morphology, white background, high resolution, encyclopedia style, accurate botanical details, professional biology drawing, 4k"

def download_image(plant, prompt):
    plant_id = plant.get('id', 'unknown')
    common_name = plant.get('commonName', 'plant')
    
    # Opción 1: Pollinations AI (Generativa)
    # Opción 2: LoremFlickr (Fotos reales gratuitas basadas en keywords)
    urls = [
        f"https://image.pollinations.ai/prompt/{prompt}", 
        f"https://loremflickr.com/1024/1024/{common_name},plant,botany"
    ]
    
    file_name = f"plant_{plant_id}.jpg"
    file_path = os.path.join(IMAGES_DIR, file_name)
    
    for i, url in enumerate(urls):
        try:
            source = "IA" if i == 0 else "Foto Real"
            print(f"   -> Intentando {source}...")
            
            response = requests.get(url, timeout=30, allow_redirects=True)
            
            if response.status_code == 200:
                # Guardamos el contenido
                with open(file_path, 'wb') as f:
                    f.write(response.content)
                
                # Verificamos que el archivo sea válido (más de 10KB)
                if os.path.exists(file_path) and os.path.getsize(file_path) > 10000:
                    return f"file:///android_asset/generated_images/{file_name}"
                else:
                    print(f"   ⚠️ Archivo demasiado pequeño, saltando...")
            elif response.status_code == 402:
                print(f"   🚫 {source} requiere pago (402).")
            else:
                print(f"   ❌ {source} Error HTTP: {response.status_code}")
                
        except Exception as e:
            print(f"   ❌ Error en {source}: {e}")
    
    return None

def process_catalogs():
    for json_file in JSON_FILES:
        path = os.path.join(ASSETS_DIR, json_file)
        print(f"\n--- Procesando {json_file} ---")
        
        with open(path, 'r', encoding='utf-8') as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError:
                print(f"Error leyendo {json_file}, saltando...")
                continue

        plants = data if isinstance(data, list) else data.get('plants', [])
        
        modified = False
        for plant in plants:
            # Generar si no tiene imagen o la URL es inválida/vacía
            if not plant.get('imageUrl') or plant['imageUrl'] == "" or "placeholder" in plant.get('imageUrl', '').lower():
                common = plant.get('commonName', 'Desconocida')
                sci = plant.get('scientificName', 'Desconocida')
                print(f"Buscando imagen para: {common} ({sci})...")
                
                prompt = generate_prompt(plant)
                local_url = download_image(plant, prompt)
                
                if local_url:
                    print(f"   ✅ ¡Obtenida con éxito!")
                    plant['imageUrl'] = local_url
                    modified = True
                    time.sleep(1) 
                else:
                    print(f"   ❌ No se encontró imagen en ninguna fuente.")

        if modified:
            with open(path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2, ensure_ascii=False)
            print(f"Archivo {json_file} actualizado correctamente.")

if __name__ == "__main__":
    print("Iniciando recuperador de imágenes botánicas (IA + Fotos Reales)...")
    process_catalogs()
    print("\nProceso completado. Revisa la carpeta app/src/main/assets/generated_images/")

