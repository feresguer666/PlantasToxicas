#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
sync_backup_a_proyecto.py
=========================
Sincroniza un backup exportado desde la app PlantasToxicas (móvil)
con los JSON de catálogo del proyecto Android Studio.

QUÉ HACE
--------
1. Lee un fichero de backup .json generado por la opción "Exportar backup"
   de la app (formato BackupRepository.BackupData, backupVersion >= 2).
2. Sobrescribe en los `app/src/main/assets/plants_*.json` TODOS los campos
   editables de cada planta (commonName, scientificName, family, toxicityLevel,
   toxicParts, symptoms, description, habitat, geographicDistribution,
   firstAid, category, imageUrl). NO toca campos personales (latitude,
   longitude, locationName, foundDate, notes, isFavorite), que vuelven a
   sus valores neutros para que el catálogo siga siendo "limpio".
3. Decodifica las fotos en base64 (`plantImages`) y las copia a
   `app/src/main/assets/generated_images/plant_<id>.jpg`.
4. Ajusta el `imageUrl` de cada planta editada a
   `file:///android_asset/generated_images/plant_<id>.jpg`.
5. (Opcional) Hace lo mismo con setas y líquenes si están en el backup
   y existen sus assets correspondientes.
6. Crea una copia de seguridad de cada JSON modificado con sufijo .bak
   (solo la primera vez, para no machacar copias buenas).

USO
---
    python sync_backup_a_proyecto.py ruta/al/PlantasToxicas_Backup_Completo_XXXX.json

Opciones útiles:
    --dry-run         No escribe nada, solo muestra qué cambiaría.
    --solo-fotos      Actualiza solo imágenes (no toca textos).
    --proyecto RUTA   Raíz del proyecto Android (por defecto: carpeta del script).
    --no-nuevas       NO crea fichero nuevo para plantas con IDs no existentes.
                      (por defecto, las plantas nuevas se vuelcan en
                       plants_<siguienteLibre>.json automáticamente).
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import shutil
import sys
from pathlib import Path
from typing import Any

# Campos que SÍ sobrescribimos del catálogo (los "editables")
CAMPOS_CATALOGO = [
    "commonName",
    "commonNames",
    "scientificName",
    "family",
    "toxicityLevel",
    "toxicParts",
    "symptoms",
    "description",
    "habitat",
    "geographicDistribution",
    "firstAid",
    "category",
    "imageUrl",
]

# Campos personales: NUNCA los pasamos al catálogo público
CAMPOS_PERSONALES_NEUTROS = {
    "isFavorite": False,
    "latitude": None,
    "longitude": None,
    "locationName": None,
    "foundDate": None,
    "notes": None,
}


def log(msg: str) -> None:
    print(msg, flush=True)


def cargar_json(path: Path) -> Any:
    """Carga un .json o un .json.gz transparentemente."""
    # Detectar GZIP por los primeros dos bytes mágicos (1F 8B)
    with path.open("rb") as f:
        magic = f.read(2)
    if magic == b"\x1f\x8b":
        import gzip
        with gzip.open(path, "rt", encoding="utf-8") as f:
            return json.load(f)
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def guardar_json(path: Path, data: Any) -> None:
    with path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hacer_backup(path: Path) -> None:
    bak = path.with_suffix(path.suffix + ".bak")
    if not bak.exists():
        shutil.copy2(path, bak)
        log(f"   📦 backup creado: {bak.name}")


def cargar_plantas_backup(backup: dict) -> dict[int, dict]:
    plantas = backup.get("plants") or []
    return {int(p["id"]): p for p in plantas if "id" in p}


def cargar_imagenes_backup(backup: dict) -> dict[str, str]:
    """Devuelve dict {relativePath: base64}."""
    out = {}
    for img in (backup.get("plantImages") or []):
        rp = img.get("relativePath")
        b64 = img.get("base64")
        if rp and b64:
            # nos quedamos solo con el nombre del fichero por seguridad
            nombre = os.path.basename(rp.replace("\\", "/"))
            out[nombre] = b64
    return out


def id_desde_nombre_foto(nombre: str) -> int | None:
    """plant_123.jpg -> 123. Devuelve None si el patrón no encaja."""
    m = re.match(r"^plant_(\d+)\.(jpg|jpeg|png|webp)$", nombre, re.IGNORECASE)
    return int(m.group(1)) if m else None


# Orden canónico de claves en una ficha del catálogo (igual que plants_1.json)
ORDEN_CAMPOS_FICHA = [
    "id",
    "isFavorite",
    "latitude",
    "longitude",
    "locationName",
    "foundDate",
    "notes",
    "commonName",
    "commonNames",
    "scientificName",
    "family",
    "toxicityLevel",
    "toxicParts",
    "symptoms",
    "description",
    "habitat",
    "geographicDistribution",
    "firstAid",
    "imageUrl",
    "category",
]


def construir_ficha_nueva(ficha_backup: dict, tiene_foto_local: bool) -> dict:
    """
    Crea una ficha nueva (para volcar a plants_N.json) a partir de los datos
    del backup, dejando los campos personales en valores neutros y forzando el
    imageUrl al asset local si hay foto nueva en el backup.
    """
    pid = int(ficha_backup["id"])
    ficha: dict = {"id": pid}

    # Campos personales neutros
    for k, v in CAMPOS_PERSONALES_NEUTROS.items():
        ficha[k] = v

    # Campos del catálogo: usamos los del backup, con defaults razonables
    defaults = {
        "commonName": "",
        "commonNames": "",
        "scientificName": "",
        "family": "",
        "toxicityLevel": "Desconocido",
        "toxicParts": "",
        "symptoms": "",
        "description": "",
        "habitat": "",
        "geographicDistribution": "",
        "firstAid": "",
        "category": "Sin categoría",
        "imageUrl": "",
    }
    for campo, default in defaults.items():
        val = ficha_backup.get(campo, default)
        ficha[campo] = val if val is not None else default

    # Si en el backup hay foto, apuntar al asset local generado
    if tiene_foto_local:
        ficha["imageUrl"] = f"file:///android_asset/generated_images/plant_{pid}.jpg"

    # Reordenar a orden canónico (más legible en el repo)
    return {k: ficha[k] for k in ORDEN_CAMPOS_FICHA if k in ficha}


def siguiente_numero_plants(plants_files: list[Path]) -> int:
    """Devuelve el siguiente N libre para plants_N.json."""
    usados = []
    rx = re.compile(r"^plants_(\d+)\.json$")
    for p in plants_files:
        m = rx.match(p.name)
        if m:
            usados.append(int(m.group(1)))
    return (max(usados) + 1) if usados else 1


def actualizar_planta(ficha_actual: dict, ficha_backup: dict, *, solo_fotos: bool) -> bool:
    """Aplica cambios sobre ficha_actual in-place. Devuelve True si hubo cambios."""
    cambios = False

    if solo_fotos:
        campos = ["imageUrl"]
    else:
        campos = CAMPOS_CATALOGO

    for campo in campos:
        if campo not in ficha_backup:
            continue
        nuevo = ficha_backup.get(campo)
        # imageUrl la gestionamos después según si hay foto local
        if campo == "imageUrl":
            continue
        if ficha_actual.get(campo) != nuevo:
            ficha_actual[campo] = nuevo if nuevo is not None else ""
            cambios = True

    # Aseguramos campos personales neutros (no contaminar catálogo)
    for k, v in CAMPOS_PERSONALES_NEUTROS.items():
        if ficha_actual.get(k) != v:
            ficha_actual[k] = v
            cambios = True

    return cambios


def main() -> int:
    aquí = Path(__file__).resolve().parent

    parser = argparse.ArgumentParser(description="Sincroniza backup móvil → JSON del proyecto.")
    parser.add_argument("backup", help="Ruta al backup .json exportado desde la app.")
    parser.add_argument("--proyecto", default=str(aquí),
                        help="Raíz del proyecto Android (default: carpeta del script).")
    parser.add_argument("--dry-run", action="store_true",
                        help="No escribe nada, solo informa.")
    parser.add_argument("--solo-fotos", action="store_true",
                        help="Solo actualiza imágenes (no toca textos).")
    parser.add_argument("--no-nuevas", action="store_true",
                        help="No crear plants_N.json para IDs nuevos.")
    args = parser.parse_args()

    backup_path = Path(args.backup).expanduser().resolve()
    proyecto = Path(args.proyecto).expanduser().resolve()
    assets = proyecto / "app" / "src" / "main" / "assets"
    img_dir = assets / "generated_images"

    if not backup_path.exists():
        log(f"❌ No existe el backup: {backup_path}")
        return 1
    if not assets.exists():
        log(f"❌ No encuentro la carpeta assets: {assets}")
        return 1

    log(f"📂 Proyecto: {proyecto}")
    log(f"📥 Backup:   {backup_path.name}")
    if args.dry_run:
        log("🧪 MODO DRY-RUN (no se escribirá nada)")

    backup = cargar_json(backup_path)
    bv = backup.get("backupVersion")
    log(f"🔖 backupVersion = {bv}  exportado {backup.get('exportedAt','?')}")

    plantas_backup = cargar_plantas_backup(backup)
    imagenes_backup = cargar_imagenes_backup(backup)
    log(f"🌿 {len(plantas_backup)} plantas en backup, "
        f"🖼️ {len(imagenes_backup)} fotos en backup")

    # ── 1. Guardar imágenes ──────────────────────────────────────────────
    if not args.dry_run:
        img_dir.mkdir(parents=True, exist_ok=True)

    fotos_escritas = 0
    ids_con_foto_nueva: set[int] = set()
    for nombre, b64 in imagenes_backup.items():
        pid = id_desde_nombre_foto(nombre)
        if pid is None:
            log(f"   ⚠️  Foto con nombre no estándar, la ignoro: {nombre}")
            continue
        destino = img_dir / nombre
        try:
            data = base64.b64decode(b64)
        except Exception as e:
            log(f"   ⚠️  No se pudo decodificar {nombre}: {e}")
            continue
        if args.dry_run:
            log(f"   [dry] escribiría {destino.relative_to(proyecto)} ({len(data)//1024} KB)")
        else:
            destino.write_bytes(data)
        ids_con_foto_nueva.add(pid)
        fotos_escritas += 1
    log(f"🖼️ Fotos {'(simuladas)' if args.dry_run else 'escritas'}: {fotos_escritas}")

    # ── 2. Recorrer todos los plants_*.json y aplicar cambios ────────────
    plants_files = sorted(assets.glob("plants_*.json"))
    if not plants_files:
        log("❌ No encuentro ningún plants_*.json en assets.")
        return 1

    total_modificadas = 0
    total_no_encontradas = set(plantas_backup.keys())

    for jf in plants_files:
        try:
            data = cargar_json(jf)
        except Exception as e:
            log(f"❌ Error leyendo {jf.name}: {e}")
            continue
        if not isinstance(data, list):
            continue

        cambios_en_fichero = 0
        for ficha in data:
            pid = ficha.get("id")
            if pid is None:
                continue
            pid = int(pid)
            ficha_bk = plantas_backup.get(pid)
            if ficha_bk is None:
                continue
            total_no_encontradas.discard(pid)

            cambió = actualizar_planta(ficha, ficha_bk, solo_fotos=args.solo_fotos)

            # imageUrl: si hay foto nueva, apuntar a generated_images;
            # si no, dejar la que ya tuviera (no la borramos).
            if pid in ids_con_foto_nueva:
                nueva_url = f"file:///android_asset/generated_images/plant_{pid}.jpg"
                if ficha.get("imageUrl") != nueva_url:
                    ficha["imageUrl"] = nueva_url
                    cambió = True

            if cambió:
                cambios_en_fichero += 1

        if cambios_en_fichero:
            total_modificadas += cambios_en_fichero
            if args.dry_run:
                log(f"   [dry] {jf.name}: {cambios_en_fichero} fichas cambiarían")
            else:
                hacer_backup(jf)
                guardar_json(jf, data)
                log(f"   ✅ {jf.name}: {cambios_en_fichero} fichas actualizadas")

    log(f"\n🌿 Total fichas {'que cambiarían' if args.dry_run else 'actualizadas'}: {total_modificadas}")

    # ── 3. Plantas NUEVAS (IDs no presentes en ningún plants_*.json) ─────
    if total_no_encontradas:
        ids_nuevos = sorted(total_no_encontradas)
        muestra = ids_nuevos[:15]
        log(f"\n🆕 {len(ids_nuevos)} plantas nuevas detectadas (IDs creados en el móvil): "
            f"{muestra}{'...' if len(ids_nuevos) > 15 else ''}")

        if args.solo_fotos:
            log("   ⏭️  --solo-fotos activo: no se crearán fichas nuevas.")
        elif args.no_nuevas:
            log("   ⏭️  --no-nuevas activo: no se creará fichero para las plantas nuevas.")
        else:
            n_libre = siguiente_numero_plants(plants_files)
            destino = assets / f"plants_{n_libre}.json"
            nuevas_fichas = []
            for pid in ids_nuevos:
                tiene_foto = pid in ids_con_foto_nueva
                nuevas_fichas.append(construir_ficha_nueva(plantas_backup[pid], tiene_foto))

            if args.dry_run:
                log(f"   [dry] crearía {destino.relative_to(proyecto)} con {len(nuevas_fichas)} fichas")
                log(f"   [dry] primer ejemplo:")
                ejemplo = nuevas_fichas[0]
                log(f"          id={ejemplo['id']}  commonName={ejemplo['commonName']!r}  "
                    f"imageUrl={ejemplo['imageUrl']!r}")
            else:
                if destino.exists():
                    # Por si por lo que sea ya existiera (no debería), no machacamos
                    log(f"   ⚠️  {destino.name} ya existe — no lo sobrescribo. "
                        f"Renombra/elimina manualmente y vuelve a ejecutar.")
                else:
                    guardar_json(destino, nuevas_fichas)
                    log(f"   ✅ Creado {destino.relative_to(proyecto)} con {len(nuevas_fichas)} fichas nuevas.")
                    log("      ℹ️  PlantDataSource.loadAll() carga los plants_N.json de forma")
                    log("          incremental (1, 2, 3…) hasta encontrar un hueco. Como el nombre")
                    log(f"          es consecutivo ({destino.name}), la app lo detectará sola.")

    log("\n✅ Hecho." if not args.dry_run else "\n🧪 Dry-run terminado (sin cambios en disco).")
    return 0


if __name__ == "__main__":
    sys.exit(main())

