# Importar cambios desde CSV a `assets/plants_*.json`

Script:

```bash
tools/import_csv_changes_to_assets.py
```

Permite editar datos en LibreOffice/Excel y aplicar cambios al catálogo JSON.

## Formato largo

```csv
id,field,value
123,description,"Nueva descripción"
123,symptoms,"Nuevos síntomas"
456,toxicityLevel,"Alto"
```

## Formato ancho

```csv
id,commonName,scientificName,description,symptoms
123,"Abrus","Abrus precatorius","Descripción...","Síntomas..."
```

## Simular primero

```bash
python3 tools/import_csv_changes_to_assets.py cambios.csv --dry-run
```

## Aplicar

```bash
python3 tools/import_csv_changes_to_assets.py cambios.csv --apply
```

## Incluir campos personales

Para app privada puedes incluir favoritos, ubicación y notas:

```bash
python3 tools/import_csv_changes_to_assets.py cambios.csv --dry-run --include-personal
python3 tools/import_csv_changes_to_assets.py cambios.csv --apply --include-personal
```

Campos personales permitidos con `--include-personal`:

```text
isFavorite, latitude, longitude, locationName, foundDate, notes
```

## Campos de catálogo permitidos

```text
commonName, commonNames, scientificName, family, toxicityLevel, toxicParts,
symptoms, description, habitat, geographicDistribution, firstAid, imageUrl,
category, floweringMonths, fruitingMonths, maxToxicityMonths, mythsAndLegends
```

## Seguridad

- Por defecto no escribe nada si no usas `--apply`.
- Crea backup automático de los JSON modificados en `backups_assets_csv_YYYYMMDD_HHMMSS/`.
- Ignora `imageUrl` locales no portables como `content://`, `file:///data/`, `file:///storage/`.

## Después de aplicar

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
git diff app/src/main/assets
```

Si todo está bien:

```bash
git add app/src/main/assets/plants_*.json
git commit -m "data: importa cambios desde csv"
git push
```
