#!/usr/bin/env python3
"""
Divide el archivo plants.json en chunks más pequeños.
Uso: python3 divide_catalogo.py [ruta_json] [plantas_por_archivo]
"""

import json
import sys
import os

def divide_json(input_file, plants_per_file=2000):
    """Divide un JSON de plantas en múltiples archivos."""
    
    output_dir = os.path.dirname(input_file)
    if not output_dir:
        output_dir = "."
    
    print(f"Leyendo {input_file}...")
    with open(input_file, 'r', encoding='utf-8') as f:
        plants = json.load(f)
    
    total = len(plants)
    print(f"Total de plantas: {total}")
    
    num_files = (total + plants_per_file - 1) // plants_per_file
    print(f"Se crearán {num_files} archivos")
    
    base_name = os.path.join(output_dir, "plants")
    
    for i in range(num_files):
        start = i * plants_per_file
        end = min((i + 1) * plants_per_file, total)
        chunk = plants[start:end]
        
        for j, plant in enumerate(chunk):
            plant['id'] = start + j + 1
        
        output_file = f"{base_name}_{i+1}.json"
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(chunk, f, ensure_ascii=False, indent=None, separators=(',', ':'))
        
        size_kb = os.path.getsize(output_file) / 1024
        print(f"  {output_file}: plantas {start+1}-{end} ({size_kb:.1f} KB)")
    
    print(f"\nListo! {num_files} archivos creados.")

if __name__ == "__main__":
    input_file = "app/src/main/assets/plants.json"
    plants_per_file = 2000
    
    if len(sys.argv) > 1:
        input_file = sys.argv[1]
    if len(sys.argv) > 2:
        plants_per_file = int(sys.argv[2])
    
    divide_json(input_file, plants_per_file)
