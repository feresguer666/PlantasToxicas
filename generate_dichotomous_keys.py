#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_dichotomous_keys.py
============================
Genera un fichero `app/src/main/assets/dichotomous_keys.json` MASIVO a partir
del catálogo de plantas, garantizando que CADA planta sea alcanzable.

Estrategia:
  1. Carga todas las plantas de plants_*.json.
  2. Conserva las claves "curadas a mano" definidas en CURATED_KEYS (más abajo)
     — son las que tienen preguntas botánicas naturales para Solanaceae,
     Apocynaceae, etc.
  3. Genera AUTOMÁTICAMENTE:
     - Clave general profunda: porte → familia (top 30) → género → especie.
     - Clave por familia (una por cada familia con >=5 plantas).
     - Clave por género (submenú dentro de cada familia con muchos géneros).
     - Clave alfabética A-Z por nombre científico.
     - Clave por categoría (Jardín, Silvestre, Tropical, ...).
     - Clave por toxicidad (Mortal, Muy alto, Alto, Moderado, Bajo).
     - Clave por hábitat / hábito (árbol, arbusto, hierba, trepadora, bulbosa,
       suculenta, hongo) usando keywords.
  4. Verifica que cada planta sea alcanzable y reporta cuántas rutas la encuentran.

Uso:
    python generate_dichotomous_keys.py
"""

import json
import glob
import os
import re
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).parent
ASSETS = ROOT / "app" / "src" / "main" / "assets"
OUT = ASSETS / "dichotomous_keys.json"

# ── Umbrales ────────────────────────────────────────────────────────────
MIN_PLANTS_FAMILY_KEY = 5      # crear clave por familia si tiene >= N
MIN_PLANTS_GENUS_OPTION = 3    # en la clave por familia, dar opción de género si >= N
TOP_FAMILIES_GENERAL = 30      # cuántas familias listar en la clave general
MAX_OPTIONS_PER_NODE = 30      # particionar nodos con demasiadas opciones

# ── Carga del catálogo ──────────────────────────────────────────────────

def load_plants():
    plants = []
    for f in sorted(ASSETS.glob("plants_*.json")):
        plants.extend(json.load(open(f, encoding="utf-8")))
    return plants

def genus_of(p):
    sn = (p.get("scientificName", "") or "").strip()
    if not sn:
        return ""
    # quitar "x" híbrido o caracteres raros al principio
    parts = sn.split()
    return parts[0].lstrip("×x").strip() if parts else ""

def family_of(p):
    return (p.get("family", "") or "").strip()

def initial_of(p):
    sn = (p.get("scientificName", "") or "").strip()
    if not sn:
        return "?"
    c = sn[0].upper()
    if c.isalpha() and c.isascii():
        return c
    return "?"

# ── Claves curadas a mano (las que ya tenías) ───────────────────────────
# Las copio TAL CUAL del JSON anterior; el script las preservará.

CURATED_KEYS = [
    {
        "id": "fam_solanaceae",
        "title": "Solanáceas (Solanaceae) — clave guiada",
        "subtitle": "Datura, belladona, floripondio, estramonio, dulcamara…",
        "scope": "family",
        "family": "Solanaceae",
        "icon": "spa",
        "rootNodeId": "sol_porte",
        "nodes": [
            {
                "id": "sol_porte",
                "question": "¿Qué porte tiene la solanácea?",
                "options": [
                    {"label": "Hierba alta con trompetas y frutos espinosos (Datura/estramonio)",
                     "description": "Hierba anual, hojas grandes lobuladas, fruto en cápsula espinosa.",
                     "image": "key_images/flower_trumpet.png",
                     "filter": {"genera": ["Datura"]},
                     "resultNote": "Género Datura — TODAS MORTALES. Alcaloides tropánicos."},
                    {"label": "Árbol pequeño con trompetas colgantes enormes (floripondio)",
                     "image": "key_images/flower_trumpet.png",
                     "filter": {"genera": ["Brugmansia"]},
                     "nextNodeId": "sol_brugmansia"},
                    {"label": "Hierba con flores violetas acampanadas y bayas negras (belladona)",
                     "image": "key_images/flower_bell.png",
                     "filter": {"genera": ["Atropa"]}},
                    {"label": "Arbusto con flores que cambian de color (Brunfelsia)",
                     "filter": {"genera": ["Brunfelsia"]}},
                    {"label": "Arbusto con flores tubulares blancas/amarillas (Cestrum)",
                     "filter": {"genera": ["Cestrum"]}},
                    {"label": "Solanum (hierba o arbusto con bayas)",
                     "filter": {"genera": ["Solanum"]}},
                    {"label": "Tabaco, mandrágora, beleño, capsicum, lycium…",
                     "filter": {"genera": ["Nicotiana", "Mandragora", "Hyoscyamus", "Physalis", "Capsicum", "Lycium"]}},
                    {"label": "Cualquier Solanácea (ver todas)", "filter": {}}
                ]
            },
            {
                "id": "sol_brugmansia",
                "question": "¿De qué color es la flor del floripondio?",
                "help": "Todas las Brugmansia son MORTALES.",
                "options": [
                    {"label": "Blancas", "image": "key_images/flower_white.png",
                     "filter": {"genera": ["Brugmansia"], "anyKeyword": ["blanc"]}},
                    {"label": "Amarillas", "image": "key_images/flower_yellow.png",
                     "filter": {"genera": ["Brugmansia"], "anyKeyword": ["amarill", "aurea"]}},
                    {"label": "Rojas/anaranjadas", "image": "key_images/flower_red.png",
                     "filter": {"genera": ["Brugmansia"], "anyKeyword": ["roj", "anaranjad", "sanguine"]}},
                    {"label": "Cualquier Brugmansia", "filter": {"genera": ["Brugmansia"]}}
                ]
            }
        ]
    },
    {
        "id": "fam_apocynaceae",
        "title": "Apocináceas (Apocynaceae) — clave guiada",
        "subtitle": "Adelfa, vinca, asclepias, cerbera, adenium…",
        "scope": "family",
        "family": "Apocynaceae",
        "icon": "spa",
        "rootNodeId": "apo_porte",
        "nodes": [
            {
                "id": "apo_porte",
                "question": "¿Qué tipo de planta es?",
                "help": "Casi todas tienen látex blanco al cortar. Cardiotóxicos peligrosos.",
                "options": [
                    {"label": "Arbusto con flores rosas/blancas, hojas lanceoladas (adelfa)",
                     "image": "key_images/flower_oleander.png",
                     "filter": {"genera": ["Nerium"]},
                     "resultNote": "Nerium oleander — MORTAL."},
                    {"label": "Arbolito con flores amarillas en trompeta (Cascabela/Thevetia)",
                     "filter": {"genera": ["Cascabela", "Thevetia"]}},
                    {"label": "Hierba/subarbusto con flores en racimo (Asclepias)",
                     "filter": {"genera": ["Asclepias"]}},
                    {"label": "Planta rastrera con flores azuladas/blancas (Vinca, Catharanthus)",
                     "filter": {"genera": ["Vinca", "Catharanthus"]}},
                    {"label": "Suculenta con flores rosa/rojas (Adenium, rosa del desierto)",
                     "filter": {"genera": ["Adenium"]}},
                    {"label": "Trepadora con flores trompeta (Allamanda)",
                     "filter": {"genera": ["Allamanda"]}},
                    {"label": "Cerbera (mango venenoso)",
                     "filter": {"genera": ["Cerbera"]}},
                    {"label": "Cualquier Apocynaceae", "filter": {}}
                ]
            }
        ]
    },
    {
        "id": "fam_ranunculaceae",
        "title": "Ranunculáceas (Ranunculaceae) — clave guiada",
        "subtitle": "Acónitos, ranúnculos, anémonas, eléboros…",
        "scope": "family",
        "family": "Ranunculaceae",
        "icon": "spa",
        "rootNodeId": "ran_flor",
        "nodes": [
            {
                "id": "ran_flor",
                "question": "¿Cómo es la flor?",
                "options": [
                    {"label": "Capuchón sobre los pétalos (acónito, matalobos)",
                     "image": "key_images/flower_aconitum.png",
                     "filter": {"genera": ["Aconitum"]},
                     "nextNodeId": "ran_aconito"},
                    {"label": "Espolón largo (Delphinium, Consolida)",
                     "filter": {"genera": ["Delphinium", "Consolida"]}},
                    {"label": "Flores amarillas brillantes de 5 pétalos (ranúnculo, adonis)",
                     "image": "key_images/flower_yellow.png",
                     "filter": {"genera": ["Ranunculus", "Adonis"]}},
                    {"label": "Flores en anémona (Anemone, Pulsatilla)",
                     "filter": {"genera": ["Anemone", "Pulsatilla"]}},
                    {"label": "Eléboros (Helleborus)",
                     "filter": {"genera": ["Helleborus"]}},
                    {"label": "Cualquier Ranunculaceae", "filter": {}}
                ]
            },
            {
                "id": "ran_aconito",
                "question": "¿De qué color es la flor del acónito?",
                "options": [
                    {"label": "Azul/violeta", "image": "key_images/flower_violet.png",
                     "filter": {"genera": ["Aconitum"], "anyKeyword": ["azul", "violet", "púrpur"]}},
                    {"label": "Amarillo pálido", "image": "key_images/flower_yellow.png",
                     "filter": {"genera": ["Aconitum"], "anyKeyword": ["amarill", "anthora", "lycoctonum", "vulparia"]}},
                    {"label": "Cualquier acónito", "filter": {"genera": ["Aconitum"]}}
                ]
            }
        ]
    },
    {
        "id": "fam_ericaceae",
        "title": "Ericáceas (Ericaceae) — clave guiada",
        "subtitle": "Rododendros, azaleas, brezos, andrómeda…",
        "scope": "family",
        "family": "Ericaceae",
        "icon": "local_florist",
        "rootNodeId": "eri_porte",
        "nodes": [
            {
                "id": "eri_porte",
                "question": "¿Qué tipo de planta es?",
                "options": [
                    {"label": "Arbusto con flores acampanadas vistosas (rododendro/azalea)",
                     "image": "key_images/flower_bell.png",
                     "filter": {"genera": ["Rhododendron"]},
                     "resultNote": "Rhododendron — grayanotoxinas."},
                    {"label": "Arbusto con hojas en aguja (brezo)",
                     "image": "key_images/leaf_needle.png",
                     "filter": {"genera": ["Calluna", "Erica"]}},
                    {"label": "Arbusto con frutos rojos (madroño, gayuba)",
                     "filter": {"genera": ["Arbutus", "Arctostaphylos"]}},
                    {"label": "Andrómeda, Pieris, Kalmia, Leucothoe",
                     "filter": {"genera": ["Andromeda", "Pieris", "Kalmia", "Leucothoe"]}},
                    {"label": "Cualquier Ericaceae", "filter": {}}
                ]
            }
        ]
    },
    {
        "id": "fam_apiaceae",
        "title": "Apiáceas (Apiaceae) — clave guiada",
        "subtitle": "Cicuta, oenanthe, ferula…",
        "scope": "family",
        "family": "Apiaceae",
        "icon": "spa",
        "rootNodeId": "api_porte",
        "nodes": [
            {
                "id": "api_porte",
                "question": "¿Algún rasgo distintivo?",
                "help": "ATENCIÓN: la cicuta se confunde con perejil y es MORTAL.",
                "options": [
                    {"label": "Tallo con manchas púrpuras + olor malo (cicuta, Conium)",
                     "filter": {"genera": ["Conium"]},
                     "resultNote": "Conium maculatum — MORTAL. Coniína."},
                    {"label": "En agua o suelos húmedos (Oenanthe, Cicuta virosa)",
                     "filter": {"genera": ["Oenanthe", "Cicuta"]}},
                    {"label": "Ferula gigantesca (cañaheja)",
                     "filter": {"genera": ["Ferula"]}},
                    {"label": "Cualquier Apiaceae", "filter": {}}
                ]
            }
        ]
    },
    {
        "id": "fam_euphorbiaceae",
        "title": "Euforbiáceas (Euphorbiaceae) — clave guiada",
        "subtitle": "Euphorbia, ricino, manzanillo…",
        "scope": "family",
        "family": "Euphorbiaceae",
        "icon": "local_florist",
        "rootNodeId": "eup_porte",
        "nodes": [
            {
                "id": "eup_porte",
                "question": "¿Qué tipo de Euforbiacea es?",
                "help": "Casi todas tienen látex blanco muy irritante. NUNCA tocar los ojos.",
                "options": [
                    {"label": "Suculenta cactiforme (Euphorbia tipo cactus)",
                     "filter": {"genera": ["Euphorbia"], "anyKeyword": ["suculenta", "cactiforme", "carnosa"]}},
                    {"label": "Hierba/subarbusto Euphorbia",
                     "filter": {"genera": ["Euphorbia"]}},
                    {"label": "Ricino (Ricinus)",
                     "filter": {"genera": ["Ricinus"]},
                     "resultNote": "Ricinus communis — ricina, MORTAL."},
                    {"label": "Manzanillo de la muerte (Hippomane)",
                     "filter": {"genera": ["Hippomane"]},
                     "resultNote": "Hippomane mancinella — el árbol más tóxico del mundo."},
                    {"label": "Cualquier Euphorbiaceae", "filter": {}}
                ]
            }
        ]
    },
]

CURATED_FAMILIES = {k["family"] for k in CURATED_KEYS if k.get("scope") == "family"}

# ── Helpers de generación ───────────────────────────────────────────────

def slug(s):
    """Normaliza una cadena para usarla como id (ascii, sin espacios)."""
    s = unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s or "x"

def make_option(label, filter_dict=None, next_node=None, description="", image=None, note=""):
    o = {"label": label}
    if description: o["description"] = description
    if image: o["image"] = image
    if filter_dict: o["filter"] = filter_dict
    if next_node: o["nextNodeId"] = next_node
    if note: o["resultNote"] = note
    return o

def partition_options(options, base_node_id, base_question, base_help=""):
    """
    Si hay demasiadas opciones, las particiona en sub-nodos alfabéticos
    (1-25, 26-50, ...) para no saturar la UI.
    Devuelve (root_node_id, list_of_nodes_to_add).
    """
    if len(options) <= MAX_OPTIONS_PER_NODE:
        return [{
            "id": base_node_id,
            "question": base_question,
            "help": base_help,
            "options": options
        }]
    # Particionar
    nodes = []
    chunks = [options[i:i+MAX_OPTIONS_PER_NODE]
              for i in range(0, len(options), MAX_OPTIONS_PER_NODE)]
    root_options = []
    for i, chunk in enumerate(chunks, 1):
        sub_id = f"{base_node_id}_p{i}"
        # tomar el primer y último label para etiquetar la opción
        first = chunk[0]["label"][:25]
        last = chunk[-1]["label"][:25]
        root_options.append({
            "label": f"({i}/{len(chunks)}) {first} … {last}",
            "nextNodeId": sub_id
        })
        nodes.append({
            "id": sub_id,
            "question": base_question,
            "options": chunk
        })
    nodes.insert(0, {
        "id": base_node_id,
        "question": base_question + " — elige bloque",
        "help": base_help + " (Hay muchas opciones, agrupadas por bloques).",
        "options": root_options
    })
    return nodes

# ── Generadores específicos ─────────────────────────────────────────────

def build_family_key(family, plants_of_family):
    """Genera una clave por familia con submenú por género."""
    kid = "fam_" + slug(family)
    # Agrupar por género
    by_genus = defaultdict(list)
    for p in plants_of_family:
        g = genus_of(p)
        if g:
            by_genus[g].append(p)
    big_genera = sorted([g for g, lst in by_genus.items()
                         if len(lst) >= MIN_PLANTS_GENUS_OPTION])
    small_genera_count = sum(len(lst) for g, lst in by_genus.items()
                             if len(lst) < MIN_PLANTS_GENUS_OPTION)

    options = []
    # Una opción "ver todas las de la familia"
    options.append(make_option(
        f"Ver TODAS las {family} ({len(plants_of_family)} plantas)",
        filter_dict={}
    ))
    # Una opción por género grande
    for g in sorted(big_genera, key=lambda x: (-len(by_genus[x]), x)):
        n = len(by_genus[g])
        options.append(make_option(
            f"{g} ({n})",
            filter_dict={"genera": [g]}
        ))
    # Opción para los géneros pequeños agrupados
    small = [g for g, lst in by_genus.items() if len(lst) < MIN_PLANTS_GENUS_OPTION]
    if small:
        options.append(make_option(
            f"Otros géneros menores ({small_genera_count} plantas en {len(small)} géneros)",
            filter_dict={"genera": small}
        ))
    # Plantas sin género detectable
    no_genus = [p for p in plants_of_family if not genus_of(p)]
    if no_genus:
        options.append(make_option(
            f"Sin género asignable ({len(no_genus)} plantas)",
            filter_dict={"families": [family], "noneKeyword": []}
        ))

    nodes = partition_options(
        options,
        base_node_id=f"{kid}_root",
        base_question=f"¿Qué género de {family} buscas?",
        base_help=f"La familia {family} tiene {len(plants_of_family)} plantas. "
                  f"Elige el género si lo conoces, o 'Ver todas' para listarlas."
    )

    return {
        "id": kid,
        "title": f"{family}",
        "subtitle": f"{len(plants_of_family)} plantas · {len(by_genus)} géneros",
        "scope": "family",
        "family": family,
        "icon": "spa",
        "rootNodeId": f"{kid}_root",
        "nodes": nodes
    }


def build_general_key(plants):
    """Clave general profunda con varios caminos de entrada."""
    fams = Counter(family_of(p) for p in plants if family_of(p))
    top_fams = [f for f, _ in fams.most_common(TOP_FAMILIES_GENERAL)]

    nodes = []

    # Nodo raíz: elegir punto de entrada
    nodes.append({
        "id": "g_root",
        "question": "¿Cómo quieres buscar la planta?",
        "help": "Elige el camino más fácil según lo que sepas de la planta.",
        "options": [
            make_option("Por porte y morfología (forma)",
                        next_node="g_porte",
                        description="No conoces familia ni nombre, solo cómo es.",
                        image="key_images/habit_herb.png"),
            make_option("Por familia botánica (Solanaceae, Apocynaceae…)",
                        next_node="g_familia",
                        description="Conoces o sospechas la familia.",
                        image="key_images/leaf_broad.png"),
            make_option("Por nivel de toxicidad",
                        next_node="g_toxicidad",
                        description="Te interesan solo las mortales, altas, etc.",
                        image="key_images/flower_red.png"),
            make_option("Por categoría / dónde vive",
                        next_node="g_categoria",
                        description="Jardín, silvestre, interior, tropical, hongo…",
                        image="key_images/habit_tree.png"),
            make_option("Por letra del nombre científico (A-Z)",
                        next_node="g_alfabetica",
                        description="Atajo: sabes que empieza por D, R…",
                        image="key_images/leaf_alternate.png"),
        ]
    })

    # ── Por porte (mantenemos la lógica anterior pero ampliada) ─────────
    nodes.append({
        "id": "g_porte",
        "question": "¿Qué porte tiene la planta?",
        "help": "Observa el tronco, la altura y la consistencia del tallo.",
        "options": [
            make_option("Árbol o arbolito",
                        filter_dict={"anyKeyword": ["árbol", "arbóreo", "arbolito"]},
                        next_node="g_arbol",
                        image="key_images/habit_tree.png"),
            make_option("Arbusto leñoso",
                        filter_dict={"anyKeyword": ["arbusto", "arbustivo"]},
                        next_node="g_arbusto",
                        image="key_images/habit_shrub.png"),
            make_option("Hierba o planta blanda",
                        filter_dict={"anyKeyword": ["hierba", "herbácea", "herbáceo", "anual", "vivaz"]},
                        next_node="g_hierba",
                        image="key_images/habit_herb.png"),
            make_option("Trepadora, liana o enredadera",
                        filter_dict={"anyKeyword": ["trepadora", "enredadera", "liana", "voluble"]},
                        next_node="g_familia",
                        image="key_images/habit_climber.png"),
            make_option("Bulbo, geófito",
                        filter_dict={"anyKeyword": ["bulbo", "bulbosa", "geófito", "rizoma", "tubérculo"]},
                        next_node="g_familia",
                        image="key_images/leaf_rosette.png"),
            make_option("Suculenta o cactus",
                        filter_dict={"anyKeyword": ["suculenta", "cactus", "carnosa", "cactiforme"]},
                        next_node="g_familia"),
            make_option("Seta u hongo",
                        filter_dict={"categories": ["Hongo"]},
                        next_node="g_familia"),
        ]
    })

    # Sub-nodos g_arbol, g_arbusto, g_hierba: van a g_familia tras refinar
    nodes.append({
        "id": "g_arbol",
        "question": "¿Quieres afinar el árbol o pasar directamente a familia?",
        "options": [
            make_option("Conífera (hojas en aguja, piñas)",
                        filter_dict={"anyKeyword": ["acicular", "agujas", "conífera", "escama", "piña"]},
                        next_node="g_familia",
                        image="key_images/leaf_needle.png"),
            make_option("Frondosa (hojas anchas planas)",
                        filter_dict={"noneKeyword": ["acicular", "conífera"]},
                        next_node="g_familia",
                        image="key_images/leaf_broad.png"),
            make_option("Saltar y elegir familia",
                        next_node="g_familia"),
        ]
    })
    nodes.append({
        "id": "g_arbusto",
        "question": "¿Quieres afinar el arbusto o pasar directamente a familia?",
        "options": [
            make_option("Con flores grandes vistosas",
                        filter_dict={"anyKeyword": ["vistosa", "vistosas", "ornamental"]},
                        next_node="g_familia"),
            make_option("Con bayas o frutos carnosos",
                        filter_dict={"anyKeyword": ["baya", "bayas", "drupa", "fruto carnoso"]},
                        next_node="g_familia",
                        image="key_images/fruit_berry.png"),
            make_option("Saltar y elegir familia",
                        next_node="g_familia"),
        ]
    })
    nodes.append({
        "id": "g_hierba",
        "question": "¿Quieres afinar la hierba?",
        "options": [
            make_option("Con flores en trompeta grande (Datura, Brugmansia…)",
                        filter_dict={"anyKeyword": ["trompeta"]},
                        image="key_images/flower_trumpet.png"),
            make_option("Con flores acampanadas en espiga (digital, dedalera)",
                        filter_dict={"genera": ["Digitalis"]},
                        image="key_images/flower_digital.png"),
            make_option("Con capuchón sobre pétalos (acónito)",
                        filter_dict={"genera": ["Aconitum"]},
                        image="key_images/flower_aconitum.png"),
            make_option("Con umbela (familia Apiaceae)",
                        filter_dict={"families": ["Apiaceae"]},
                        image="key_images/flower_umbel.png"),
            make_option("Con látex al cortar",
                        filter_dict={"anyKeyword": ["látex"]},
                        image="key_images/latex_white.png"),
            make_option("Saltar y elegir familia",
                        next_node="g_familia"),
        ]
    })

    # ── Por familia (top + opción "otra") ───────────────────────────────
    fam_options = []
    for f in top_fams:
        kid = "fam_" + slug(f)
        fam_options.append(make_option(
            f"{f} ({fams[f]})",
            filter_dict={"families": [f]},
        ))
    # las demás familias agrupadas
    other_fams = [f for f, _ in fams.most_common() if f not in top_fams]
    if other_fams:
        fam_options.append(make_option(
            f"Otra familia menos común ({sum(fams[f] for f in other_fams)} plantas en {len(other_fams)} familias)",
            next_node="g_familia_otras",
        ))
    fam_options.append(make_option("No sé la familia — buscar por letra",
                                   next_node="g_alfabetica"))

    nodes.extend(partition_options(
        fam_options,
        base_node_id="g_familia",
        base_question="¿A qué familia pertenece?",
        base_help="Las familias se muestran ordenadas por número de plantas en el catálogo."
    ))

    # Familias menos comunes → elegir entre las restantes
    other_options = [
        make_option(f"{f} ({fams[f]})", filter_dict={"families": [f]})
        for f in sorted(other_fams)
    ]
    nodes.extend(partition_options(
        other_options,
        base_node_id="g_familia_otras",
        base_question="Familias menos comunes",
        base_help="Familias con menos plantas en el catálogo."
    ))

    # ── Por toxicidad ───────────────────────────────────────────────────
    tox_levels = ["Mortal", "Muy alto", "Alto", "Moderado", "Bajo"]
    nodes.append({
        "id": "g_toxicidad",
        "question": "¿Qué nivel de toxicidad?",
        "options": [
            *[make_option(t, filter_dict={"toxicityLevels": [t]}, next_node="g_familia_tras_tox")
              for t in tox_levels],
            make_option("Cualquier nivel", next_node="g_familia"),
        ]
    })
    # Tras toxicidad: opcionalmente acotar por familia
    nodes.append({
        "id": "g_familia_tras_tox",
        "question": "¿Quieres acotar por familia o ver todas con ese nivel?",
        "options": [
            make_option("Ver todas las del nivel elegido", filter_dict={}),
            make_option("Acotar por familia", next_node="g_familia"),
            make_option("Acotar por letra (A-Z)", next_node="g_alfabetica"),
        ]
    })

    # ── Por categoría ───────────────────────────────────────────────────
    cats = Counter(p.get("category", "") for p in plants if p.get("category"))
    cat_options = []
    for c, n in cats.most_common():
        cat_options.append(make_option(f"{c} ({n})", filter_dict={"categories": [c]}))
    nodes.extend(partition_options(
        cat_options,
        base_node_id="g_categoria",
        base_question="¿En qué categoría/hábitat vive?",
    ))

    # ── Alfabética A-Z ──────────────────────────────────────────────────
    letters = sorted({initial_of(p) for p in plants if initial_of(p) != "?"})
    letter_options = []
    for L in letters:
        n = sum(1 for p in plants if initial_of(p) == L)
        letter_options.append(make_option(
            f"{L} ({n})",
            filter_dict={"anyKeyword": []},  # no filtra, lo hace el nextNode
            next_node=f"g_letra_{L}"
        ))
    nodes.append({
        "id": "g_alfabetica",
        "question": "¿Por qué letra empieza el nombre científico?",
        "options": letter_options
    })

    # Para cada letra: agrupar por género dentro de esa letra
    for L in letters:
        plants_L = [p for p in plants if initial_of(p) == L]
        by_genus = defaultdict(list)
        for p in plants_L:
            g = genus_of(p)
            if g:
                by_genus[g].append(p)
        opts = []
        # géneros con varias plantas → opción propia con filtro de género
        for g in sorted(by_genus.keys()):
            n = len(by_genus[g])
            if n >= 2:
                opts.append(make_option(
                    f"{g} ({n})",
                    filter_dict={"genera": [g]}
                ))
        # géneros con una sola → opción por planta concreta
        single_genera = [g for g, lst in by_genus.items() if len(lst) == 1]
        if single_genera:
            opts.append(make_option(
                f"Otros géneros con una sola planta ({len(single_genera)})",
                filter_dict={"genera": single_genera}
            ))
        opts.append(make_option(
            f"Ver todas las que empiezan por {L}",
            filter_dict={"genera": list(by_genus.keys())}
        ))
        nodes.extend(partition_options(
            opts,
            base_node_id=f"g_letra_{L}",
            base_question=f"Plantas cuyo nombre científico empieza por {L}",
            base_help=f"{len(plants_L)} plantas, agrupadas por género."
        ))

    return {
        "id": "general",
        "title": "Clave general de plantas tóxicas",
        "subtitle": f"{len(plants)} plantas · {len(fams)} familias · cualquier ruta llega",
        "scope": "general",
        "icon": "filter_alt",
        "rootNodeId": "g_root",
        "nodes": nodes
    }


def build_alphabetical_key(plants):
    """Clave dedicada A-Z (atajo independiente)."""
    letters = sorted({initial_of(p) for p in plants if initial_of(p) != "?"})
    nodes = []
    root_opts = []
    for L in letters:
        n = sum(1 for p in plants if initial_of(p) == L)
        root_opts.append(make_option(
            f"{L} ({n})",
            next_node=f"abc_{L}"
        ))
    nodes.append({
        "id": "abc_root",
        "question": "¿Por qué letra empieza el nombre científico?",
        "options": root_opts
    })
    for L in letters:
        plants_L = sorted([p for p in plants if initial_of(p) == L],
                          key=lambda p: p.get("scientificName", ""))
        by_genus = defaultdict(list)
        for p in plants_L:
            g = genus_of(p) or "?"
            by_genus[g].append(p)
        opts = []
        for g in sorted(by_genus.keys()):
            n = len(by_genus[g])
            opts.append(make_option(
                f"{g} ({n})",
                filter_dict={"genera": [g]} if g != "?" else {"anyKeyword": []}
            ))
        opts.append(make_option(
            f"Ver todas las que empiezan por {L} ({len(plants_L)})",
            filter_dict={"genera": [g for g in by_genus.keys() if g != "?"]}
        ))
        nodes.extend(partition_options(
            opts,
            base_node_id=f"abc_{L}",
            base_question=f"Plantas cuyo nombre científico empieza por {L}"
        ))
    return {
        "id": "alphabetical",
        "title": "Búsqueda alfabética (A-Z)",
        "subtitle": "Si conoces la inicial del nombre científico",
        "scope": "general",
        "icon": "filter_alt",
        "rootNodeId": "abc_root",
        "nodes": nodes
    }


def build_toxicity_key(plants):
    """Clave por nivel de toxicidad con subdivisión por familia."""
    tox_levels = ["Mortal", "Muy alto", "Alto", "Moderado", "Bajo"]
    nodes = []
    root_opts = []
    for t in tox_levels:
        n = sum(1 for p in plants if p.get("toxicityLevel") == t)
        if n == 0:
            continue
        root_opts.append(make_option(
            f"{t} ({n})",
            filter_dict={"toxicityLevels": [t]},
            next_node=f"tox_{slug(t)}"
        ))
    nodes.append({
        "id": "tox_root",
        "question": "¿Qué nivel de toxicidad?",
        "options": root_opts
    })
    for t in tox_levels:
        plants_t = [p for p in plants if p.get("toxicityLevel") == t]
        if not plants_t:
            continue
        fams = Counter(family_of(p) for p in plants_t if family_of(p))
        opts = [make_option("Ver TODAS las de este nivel", filter_dict={})]
        for f, n in fams.most_common(MAX_OPTIONS_PER_NODE - 1):
            opts.append(make_option(f"{f} ({n})", filter_dict={"families": [f]}))
        if len(fams) > MAX_OPTIONS_PER_NODE - 1:
            rest = [f for f, _ in fams.most_common()][MAX_OPTIONS_PER_NODE-1:]
            opts.append(make_option(
                f"Otras familias ({sum(fams[f] for f in rest)})",
                filter_dict={"families": rest}
            ))
        nodes.append({
            "id": f"tox_{slug(t)}",
            "question": f"Plantas de toxicidad '{t}' — elige familia",
            "options": opts
        })
    return {
        "id": "by_toxicity",
        "title": "Por nivel de toxicidad",
        "subtitle": "Mortales, muy altas, altas, moderadas, bajas",
        "scope": "general",
        "icon": "filter_alt",
        "rootNodeId": "tox_root",
        "nodes": nodes
    }


def build_category_key(plants):
    """Clave por categoría (Jardín, Silvestre, Hongo…)."""
    cats = Counter(p.get("category", "") for p in plants if p.get("category"))
    nodes = []
    root_opts = []
    for c, n in cats.most_common():
        root_opts.append(make_option(
            f"{c} ({n})",
            filter_dict={"categories": [c]},
            next_node=f"cat_{slug(c)}"
        ))
    nodes.append({
        "id": "cat_root",
        "question": "¿En qué categoría/hábitat vive?",
        "options": root_opts
    })
    for c, _ in cats.most_common():
        plants_c = [p for p in plants if p.get("category") == c]
        fams = Counter(family_of(p) for p in plants_c if family_of(p))
        opts = [make_option(f"Ver TODAS de '{c}'", filter_dict={})]
        for f, n in fams.most_common(MAX_OPTIONS_PER_NODE - 1):
            opts.append(make_option(f"{f} ({n})", filter_dict={"families": [f]}))
        nodes.append({
            "id": f"cat_{slug(c)}",
            "question": f"Categoría '{c}' — elige familia",
            "options": opts
        })
    return {
        "id": "by_category",
        "title": "Por categoría / hábitat",
        "subtitle": "Jardín, silvestre, interior, tropical, hongo…",
        "scope": "general",
        "icon": "local_florist",
        "rootNodeId": "cat_root",
        "nodes": nodes
    }


# ── Verificación: cada planta es alcanzable ─────────────────────────────

def haystack(p):
    fields = ["commonName", "commonNames", "scientificName", "family",
              "description", "habitat", "toxicParts", "symptoms",
              "geographicDistribution", "category"]
    return " ".join(str(p.get(k, "") or "") for k in fields).lower()

def matches(p, f):
    fam = (p.get("family", "") or "").lower()
    if f.get("families") and not any(fam == x.lower() or fam.startswith(x.lower()) for x in f["families"]):
        return False
    if f.get("notFamilies") and any(fam == x.lower() or fam.startswith(x.lower()) for x in f["notFamilies"]):
        return False
    if f.get("categories") and (p.get("category", "") or "").lower() not in [x.lower() for x in f["categories"]]:
        return False
    if f.get("toxicityLevels") and (p.get("toxicityLevel", "") or "").lower() not in [x.lower() for x in f["toxicityLevels"]]:
        return False
    if f.get("genera"):
        first = (p.get("scientificName", "") or "").strip().split(" ")[0].lstrip("×x").lower()
        if first not in [x.lower() for x in f["genera"]]:
            return False
    hay = haystack(p)
    if f.get("allKeywords") and any(k.lower() not in hay for k in f["allKeywords"]):
        return False
    if f.get("anyKeyword") and not any(k.lower() in hay for k in f["anyKeyword"]):
        return False
    if f.get("noneKeyword") and any(k.lower() in hay for k in f["noneKeyword"]):
        return False
    return True

def combine_filters(filters):
    out = {}
    for f in filters:
        if not f:
            continue
        for k, v in f.items():
            out.setdefault(k, []).extend(v)
    return {k: list(set(v)) for k, v in out.items() if v}

def all_paths(key):
    """Genera todos los caminos desde root hasta hojas, devolviendo
    lista de filtros acumulados a lo largo del camino."""
    nodes = {n["id"]: n for n in key["nodes"]}
    base = []
    if key.get("scope") == "family" and key.get("family"):
        base = [{"families": [key["family"]]}]
    elif key.get("scope") == "category" and key.get("category"):
        base = [{"categories": [key["category"]]}]

    results = []
    def walk(node_id, filters):
        node = nodes.get(node_id)
        if not node:
            return
        for opt in node.get("options", []):
            fs = filters + ([opt["filter"]] if opt.get("filter") else [])
            nxt = opt.get("nextNodeId")
            if nxt:
                walk(nxt, fs)
            else:
                # hoja terminal
                results.append(combine_filters(fs))
    walk(key["rootNodeId"], base)
    return results

def reachable_plants(key, plants):
    """Optimizado: pre-indexa por familia/género/categoría para no recorrer
    10k plantas en cada filtro."""
    # Índices
    by_family = defaultdict(list)
    by_genus = defaultdict(list)
    by_category = defaultdict(list)
    by_tox = defaultdict(list)
    for p in plants:
        fam = family_of(p).lower()
        if fam:
            by_family[fam].append(p)
        g = genus_of(p).lower()
        if g:
            by_genus[g].append(p)
        c = (p.get("category", "") or "").lower()
        if c:
            by_category[c].append(p)
        t = (p.get("toxicityLevel", "") or "").lower()
        if t:
            by_tox[t].append(p)

    paths = all_paths(key)
    reachable = set()
    for f in paths:
        # Determinar conjunto inicial reducido
        candidates = None
        if f.get("families"):
            cs = set()
            for fam in f["families"]:
                key_fam = fam.lower()
                # prefijo: si "Solanaceae" en filtro, también "Solanaceae XYZ"
                for k_in_idx, lst in by_family.items():
                    if k_in_idx == key_fam or k_in_idx.startswith(key_fam):
                        for p in lst:
                            cs.add(p["id"])
            candidates = cs
        if f.get("genera"):
            cs = set()
            for g in f["genera"]:
                for p in by_genus.get(g.lower(), []):
                    cs.add(p["id"])
            candidates = cs if candidates is None else (candidates & cs)
        if f.get("categories"):
            cs = set()
            for c in f["categories"]:
                for p in by_category.get(c.lower(), []):
                    cs.add(p["id"])
            candidates = cs if candidates is None else (candidates & cs)
        if f.get("toxicityLevels"):
            cs = set()
            for t in f["toxicityLevels"]:
                for p in by_tox.get(t.lower(), []):
                    cs.add(p["id"])
            candidates = cs if candidates is None else (candidates & cs)

        # Si no hay candidato reducido y el filtro tiene keywords, hay que recorrer todo
        pool = plants if candidates is None else [p for p in plants if p["id"] in candidates]

        # Aplicar filtro completo sobre el conjunto reducido
        for p in pool:
            if matches(p, f):
                reachable.add(p["id"])
    return reachable


# ── Main ────────────────────────────────────────────────────────────────

def main():
    plants = load_plants()
    print(f"📚 Catálogo: {len(plants)} plantas")

    fams = Counter(family_of(p) for p in plants if family_of(p))
    big_families = [f for f, n in fams.items() if n >= MIN_PLANTS_FAMILY_KEY]
    print(f"🌿 {len(fams)} familias, {len(big_families)} con >={MIN_PLANTS_FAMILY_KEY} plantas")

    keys = []

    # 1. Clave general
    print("→ Generando clave general…")
    keys.append(build_general_key(plants))

    # 2. Claves curadas (las primeras de cada familia tienen prioridad)
    print(f"→ Añadiendo {len(CURATED_KEYS)} claves curadas a mano…")
    keys.extend(CURATED_KEYS)

    # 3. Claves por familia automáticas (saltando las curadas)
    print(f"→ Generando claves automáticas por familia…")
    auto_fam = 0
    for f, n in sorted(fams.items(), key=lambda x: -x[1]):
        if n < MIN_PLANTS_FAMILY_KEY:
            continue
        if f in CURATED_FAMILIES:
            continue
        plants_f = [p for p in plants if family_of(p) == f]
        keys.append(build_family_key(f, plants_f))
        auto_fam += 1
    print(f"   ✓ {auto_fam} claves automáticas por familia")

    # 4. Clave alfabética
    print("→ Generando clave alfabética A-Z…")
    keys.append(build_alphabetical_key(plants))

    # 5. Clave por toxicidad
    print("→ Generando clave por toxicidad…")
    keys.append(build_toxicity_key(plants))

    # 6. Clave por categoría
    print("→ Generando clave por categoría…")
    keys.append(build_category_key(plants))

    # ── Verificación de cobertura ───────────────────────────────────────
    print("\n🔎 Verificando cobertura: ¿toda planta es alcanzable?")
    all_reachable = set()
    coverage = Counter()
    for k in keys:
        r = reachable_plants(k, plants)
        all_reachable |= r
        coverage[k["id"]] = len(r)
    all_ids = {p["id"] for p in plants}
    missing = all_ids - all_reachable
    print(f"   Plantas alcanzables (alguna ruta): {len(all_reachable)}/{len(all_ids)}")
    if missing:
        print(f"   ⚠️  {len(missing)} plantas NO alcanzables")
        for pid in list(missing)[:10]:
            p = next(x for x in plants if x["id"] == pid)
            print(f"      - id={pid}  {p.get('commonName')} | {p.get('scientificName')} | fam={p.get('family')!r}")
    else:
        print(f"   ✅ Todas las plantas son alcanzables por al menos una ruta.")

    # Top 10 claves por cobertura
    print("\n   Top claves por nº de plantas alcanzables:")
    for kid, n in coverage.most_common(10):
        title = next(k["title"] for k in keys if k["id"] == kid)
        print(f"      {n:5d}  [{kid}]  {title}")

    # ── Escritura ───────────────────────────────────────────────────────
    out_data = {
        "version": 2,
        "_doc": "Generado por generate_dichotomous_keys.py. Mezcla claves curadas a mano (Solanaceae, Apocynaceae, …) con claves automáticas por familia/género/letra/toxicidad/categoría. Cada planta del catálogo es alcanzable por al menos una ruta.",
        "_generated": True,
        "_stats": {
            "plants": len(plants),
            "keys": len(keys),
            "families": len(fams),
            "reachable": len(all_reachable),
        },
        "keys": keys
    }
    OUT.write_text(json.dumps(out_data, ensure_ascii=False, indent=2), encoding="utf-8")
    size_kb = OUT.stat().st_size / 1024
    nodes_total = sum(len(k["nodes"]) for k in keys)
    options_total = sum(sum(len(n.get("options", [])) for n in k["nodes"]) for k in keys)
    print(f"\n✅ Escrito {OUT.relative_to(ROOT)}")
    print(f"   {len(keys)} claves · {nodes_total} nodos · {options_total} opciones · {size_kb:.1f} KB")


if __name__ == "__main__":
    main()
