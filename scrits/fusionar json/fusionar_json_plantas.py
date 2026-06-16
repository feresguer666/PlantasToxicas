#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fusiona varios JSON de plantas en el formato de PlantasToxicas, elimina duplicados
por nombre científico y deja IDs correlativos.

Ejemplos:
  python3 fusionar_json_plantas.py app/src/main/assets/plants.json plants_online_5000.json --output plants_merged.json

  python3 fusionar_json_plantas.py app/src/main/assets/plants.json plants_online_5000.json --output app/src/main/assets/plants.json
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Tuple

APP_FIELDS = [
    "id", "isFavorite", "latitude", "longitude", "locationName", "foundDate", "notes",
    "commonName", "scientificName", "family", "toxicityLevel", "toxicParts",
    "symptoms", "description", "habitat", "geographicDistribution", "firstAid",
    "imageUrl", "category",
]

DEFAULTS = {
    "id": 0,
    "isFavorite": False,
    "latitude": None,
    "longitude": None,
    "locationName": None,
    "foundDate": None,
    "notes": None,
    "commonName": "",
    "scientificName": "",
    "family": "",
    "toxicityLevel": "Desconocido",
    "toxicParts": "",
    "symptoms": "",
    "description": "",
    "habitat": "",
    "geographicDistribution": "",
    "firstAid": "",
    "imageUrl": "",
    "category": "Exterior",
}


def load_json_list(path: Path) -> List[Dict[str, Any]]:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    # Soporta formato lista directa y formato {"plants": [...]}.
    if isinstance(data, dict) and isinstance(data.get("plants"), list):
        data = data["plants"]
    if not isinstance(data, list):
        raise ValueError(f"{path} no contiene una lista JSON ni un objeto con clave 'plants'")
    return [x for x in data if isinstance(x, dict)]


def clean_text(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


def normalize_scientific(name: Any) -> str:
    """Normaliza para detectar duplicados aunque haya autores botánicos."""
    s = clean_text(name)
    if not s:
        return ""
    s = s.replace("×", "x")
    s = re.sub(r"\b(spp|sp)\.?\b", "spp", s, flags=re.I)
    tokens = re.findall(r"[A-Za-zÀ-ÿ-]+", s.lower())
    if not tokens:
        return ""
    # Género + especie o Género + spp. Ignora autores como Mill., L., Boiss., etc.
    if len(tokens) >= 2:
        if tokens[1] in {"spp", "sp"}:
            return f"{tokens[0]} spp"
        return f"{tokens[0]} {tokens[1]}"
    return tokens[0]


def normalize_common(name: Any) -> str:
    s = clean_text(name).lower()
    s = re.sub(r"[^a-záéíóúüñ0-9]+", " ", s, flags=re.I)
    return re.sub(r"\s+", " ", s).strip()


def duplicate_key(plant: Dict[str, Any]) -> str:
    sci = normalize_scientific(plant.get("scientificName"))
    if sci:
        return "sci:" + sci
    common = normalize_common(plant.get("commonName"))
    if common:
        return "common:" + common
    # Último recurso: no debería ocurrir.
    return "unknown:" + str(id(plant))


def score_completeness(plant: Dict[str, Any]) -> int:
    """Puntuación sencilla para saber qué ficha trae más información."""
    score = 0
    for k in APP_FIELDS:
        v = plant.get(k)
        if v not in (None, "", [], {}):
            score += 1
            if isinstance(v, str):
                score += min(len(v), 500) // 80
    if plant.get("imageUrl"):
        score += 5
    if plant.get("notes"):
        score += 2
    return score


def normalize_record(plant: Dict[str, Any]) -> Dict[str, Any]:
    out = dict(DEFAULTS)
    for k in APP_FIELDS:
        if k in plant:
            out[k] = plant[k]
    # Normaliza tipos básicos.
    out["isFavorite"] = bool(out.get("isFavorite", False))
    for k in ["commonName", "scientificName", "family", "toxicityLevel", "toxicParts", "symptoms", "description", "habitat", "geographicDistribution", "firstAid", "imageUrl", "category"]:
        out[k] = clean_text(out.get(k))
    for k in ["latitude", "longitude", "locationName", "foundDate", "notes"]:
        if out.get(k) == "":
            out[k] = None
    return {k: out.get(k) for k in APP_FIELDS}


def merge_missing(base: Dict[str, Any], extra: Dict[str, Any]) -> Dict[str, Any]:
    """Mantiene la ficha base, pero rellena campos vacíos con la ficha duplicada."""
    merged = dict(base)
    for k in APP_FIELDS:
        if k == "id":
            continue
        if merged.get(k) in (None, "", [], {}) and extra.get(k) not in (None, "", [], {}):
            merged[k] = extra[k]
    # Concatena notes si ambas tienen fuentes distintas.
    a = clean_text(base.get("notes"))
    b = clean_text(extra.get("notes"))
    if a and b and b not in a:
        merged["notes"] = a + " | Duplicado fusionado: " + b
    return merged


def backup_if_needed(path: Path) -> Path | None:
    if not path.exists():
        return None
    bak = path.with_suffix(path.suffix + ".bak-" + datetime.now().strftime("%Y%m%d-%H%M%S"))
    shutil.copy2(path, bak)
    return bak


def main() -> None:
    parser = argparse.ArgumentParser(description="Fusiona JSON de plantas, elimina duplicados y renumera IDs correlativos.")
    parser.add_argument("inputs", nargs="+", help="Archivos JSON a fusionar. El primero tiene prioridad en duplicados.")
    parser.add_argument("--output", "-o", required=True, help="Archivo JSON de salida")
    parser.add_argument("--start-id", type=int, default=1, help="Primer ID. Por defecto: 1")
    parser.add_argument("--prefer-complete", action="store_true", help="Si hay duplicado, conserva la ficha más completa en lugar de la primera")
    parser.add_argument("--no-backup", action="store_true", help="No crear backup si el output ya existe")
    parser.add_argument("--indent", type=int, default=1, help="Indentación JSON. Por defecto: 1 como tu plants.json")
    args = parser.parse_args()

    inputs = [Path(x) for x in args.inputs]
    output = Path(args.output)

    merged_by_key: Dict[str, Dict[str, Any]] = {}
    order: List[str] = []
    total_read = 0
    duplicates = 0

    for path in inputs:
        if not path.exists():
            raise FileNotFoundError(f"No existe: {path}")
        plants = load_json_list(path)
        print(f"Leyendo {path}: {len(plants)} registros")
        total_read += len(plants)
        for raw in plants:
            plant = normalize_record(raw)
            key = duplicate_key(plant)
            if key not in merged_by_key:
                merged_by_key[key] = plant
                order.append(key)
            else:
                duplicates += 1
                current = merged_by_key[key]
                if args.prefer_complete and score_completeness(plant) > score_completeness(current):
                    merged_by_key[key] = merge_missing(plant, current)
                else:
                    merged_by_key[key] = merge_missing(current, plant)

    result = [merged_by_key[k] for k in order]

    # Renumerar IDs correlativos.
    next_id = args.start_id
    for plant in result:
        plant["id"] = next_id
        next_id += 1

    output.parent.mkdir(parents=True, exist_ok=True)
    bak = None if args.no_backup else backup_if_needed(output)
    with open(output, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=args.indent)

    if bak:
        print(f"Backup creado: {bak}")
    print("--- RESUMEN ---")
    print(f"Registros leídos: {total_read}")
    print(f"Duplicados fusionados: {duplicates}")
    print(f"Registros finales: {len(result)}")
    print(f"IDs: {args.start_id} - {next_id - 1}")
    print(f"Salida: {output}")


if __name__ == "__main__":
    main()
