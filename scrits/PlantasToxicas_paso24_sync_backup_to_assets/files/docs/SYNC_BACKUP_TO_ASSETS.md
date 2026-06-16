# Sincronizar cambios de la app hacia `assets/plants_*.json`

La app guarda tus ediciones en la base Room del móvil. Para pasar esas ediciones al catálogo fuente del proyecto (`app/src/main/assets/plants_*.json`) usa:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz --dry-run
```

## Flujo recomendado

1. Edita plantas en la app.
2. Haz una copia incremental desde Ajustes.
3. Copia el `.json.gz` al PC, por ejemplo a `backups/`.
4. Ejecuta primero en modo simulación:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz \
  --dry-run \
  --include-personal \
  --only-edited \
  --create-new \
  --delete-removed
```

5. Si el resumen es correcto, aplica:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz \
  --apply \
  --include-personal \
  --only-edited \
  --create-new \
  --delete-removed
```

6. Valida los JSON:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

7. Revisa cambios:

```bash
git diff app/src/main/assets
```

8. Haz commit:

```bash
git add app/src/main/assets/plants_*.json
git commit -m "data: sincroniza catálogo desde backup"
git push
```

## Opciones

| Opción | Qué hace |
|---|---|
| `--dry-run` | Simula y muestra cambios. No escribe nada. Es el modo recomendado primero. |
| `--apply` | Aplica cambios a los JSON. |
| `--include-personal` | Incluye `isFavorite`, ubicación y `notes`. Útil si el catálogo es privado. |
| `--only-edited` | Solo actualiza IDs incluidos en `editedPlantIds`. Evita reescribir cambios no editados. |
| `--create-new` | Crea en `plants_*.json` plantas que existen en backup pero no en assets. |
| `--delete-removed` | Borra de `plants_*.json` IDs incluidos en `deletedPlantIds`. |
| `--max-details N` | Controla cuántos cambios detallados muestra. |

## Campos sincronizados

Campos de catálogo:

```text
commonName, commonNames, scientificName, family, toxicityLevel, toxicParts,
symptoms, description, habitat, geographicDistribution, firstAid, imageUrl,
category, floweringMonths, fruitingMonths, maxToxicityMonths, mythsAndLegends
```

Con `--include-personal` también:

```text
isFavorite, latitude, longitude, locationName, foundDate, notes
```

## Imágenes locales

El script ignora rutas de imagen locales del móvil, porque no sirven en el proyecto:

```text
content://...
file:///data/...
file:///storage/...
file:///sdcard/...
```

Sí permite:

```text
https://...
http://...
file:///android_asset/...
```

## Seguridad

Antes de escribir, el script crea una copia automática de los JSON candidatos en una carpeta:

```text
backups_assets_YYYYMMDD_HHMMSS/
```

Aun así, usa siempre primero `--dry-run` y revisa `git diff` después de `--apply`.
