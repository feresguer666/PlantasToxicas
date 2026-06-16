# Extraer fotos de un backup completo hacia assets

Las copias incrementales actuales son solo datos y no incluyen fotos. Para pasar fotos del móvil al proyecto necesitas una **copia completa**.

El script es:

```bash
tools/extract_backup_images_to_assets.py
```

## Flujo recomendado

1. En la app, crea una **copia completa**.
2. Copia el `.json.gz` al PC, por ejemplo:

```text
backups/PlantasToxicas_Backup_Completo_20260616_2100.json.gz
```

3. Simula primero:

```bash
python3 tools/extract_backup_images_to_assets.py backups/PlantasToxicas_Backup_Completo_20260616_2100.json.gz --dry-run --only-missing
```

4. Si todo está bien, aplica:

```bash
python3 tools/extract_backup_images_to_assets.py backups/PlantasToxicas_Backup_Completo_20260616_2100.json.gz --apply --only-missing
```

5. Valida:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

6. Revisa cambios:

```bash
git diff app/src/main/assets
```

7. Commit:

```bash
git add app/src/main/assets/plants_*.json app/src/main/assets/generated_images/
git commit -m "data: importa fotos desde backup completo"
git push
```

## Opciones

| Opción | Qué hace |
|---|---|
| `--dry-run` | Simula sin escribir. Es el modo por defecto si no usas `--apply`. |
| `--apply` | Escribe imágenes y actualiza `imageUrl`. |
| `--only-missing` | Solo procesa plantas sin imagen o con ruta local/no portable. Recomendado. |
| `--overwrite` | Sobrescribe imagen/URL aunque ya exista. Úsalo con cuidado. |
| `--only-edited` | Solo procesa plantas incluidas en `editedPlantIds`. |
| `--max-images N` | Limita cuántas imágenes procesa. Útil para probar con 10 o 50. |

## Qué escribe

Guarda imágenes en:

```text
app/src/main/assets/generated_images/plant_ID.jpg
```

Y actualiza el JSON:

```json
"imageUrl": "file:///android_asset/generated_images/plant_ID.jpg"
```

## Seguridad

Antes de sobrescribir imágenes o JSON existentes, crea una copia en:

```text
backups_assets_images_YYYYMMDD_HHMMSS/
```

## Recomendación de prueba pequeña

Antes de importar cientos o miles de fotos:

```bash
python3 tools/extract_backup_images_to_assets.py backups/backup_completo.json.gz --dry-run --only-missing --max-images 20
python3 tools/extract_backup_images_to_assets.py backups/backup_completo.json.gz --apply --only-missing --max-images 20
```

Luego compila/abre la app y confirma que esas fotos se ven bien.
