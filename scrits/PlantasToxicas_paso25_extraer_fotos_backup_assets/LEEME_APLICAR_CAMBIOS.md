# Paso 25: extraer fotos de backup completo a assets

## Qué añade

Nuevo script:

```text
tools/extract_backup_images_to_assets.py
```

Sirve para sacar fotos de una **copia completa** de la app y guardarlas en:

```text
app/src/main/assets/generated_images/
```

También actualiza `imageUrl` en `plants_*.json` a:

```text
file:///android_asset/generated_images/plant_ID.jpg
```

## Importante

Las copias incrementales NO incluyen fotos. Para este script necesitas una **copia completa**.

## Uso recomendado

Primero simular:

```bash
python3 tools/extract_backup_images_to_assets.py backups/backup_completo.json.gz --dry-run --only-missing
```

Aplicar:

```bash
python3 tools/extract_backup_images_to_assets.py backups/backup_completo.json.gz --apply --only-missing
```

Prueba pequeña:

```bash
python3 tools/extract_backup_images_to_assets.py backups/backup_completo.json.gz --apply --only-missing --max-images 20
```

## Opciones

- `--only-missing`: recomendado, solo plantas sin imagen o con ruta local no portable.
- `--overwrite`: sobrescribe aunque ya haya imagen.
- `--only-edited`: solo plantas marcadas como editadas.
- `--max-images N`: limita la cantidad para pruebas.

## Documentación

Ver:

```text
docs/EXTRACT_BACKUP_IMAGES.md
```

## Archivos incluidos

- `tools/extract_backup_images_to_assets.py`
- `docs/EXTRACT_BACKUP_IMAGES.md`
- `docs/SCRIPTS.md`
