# Scripts de mantenimiento

Este proyecto incluye varios scripts Python para mantenimiento masivo del catálogo, imágenes
y copias de seguridad. Antes de ejecutarlos sobre datos importantes, se recomienda:

1. Trabajar en una rama nueva.
2. Hacer una copia de seguridad de `app/src/main/assets/`.
3. Revisar el `git diff` antes de confirmar cambios.
4. Ejecutar primero los modos de previsualización cuando existan (`--dry-run`).

## Scripts principales

| Script | Uso aproximado |
|---|---|
| `divide_catalogo.py` | Divide o reorganiza catálogos grandes en varios JSON. |
| `arreglar_imagenes_json.py` | Repara/normaliza campos de imágenes en JSON. |
| `rellenar_imagenes_json.py` | Completa imágenes faltantes cuando encuentra fuentes disponibles. |
| `auto_fotos_reales.py` | Automatiza búsqueda/asignación de fotos reales para un catálogo concreto. |
| `generate_bulk_images.py` | Genera o gestiona imágenes en lote. |
| `generate_dichotomous_keys.py` | Genera claves dicotómicas. |
| `convert_screens.py` | Conversión/ajustes masivos sobre pantallas Compose. |
| `fix_dark_mode_contrast.py` | Ajustes de contraste para modo oscuro. |
| `fix_reagents_local.py` | Correcciones locales de reactivos químicos. |
| `sync_backup_a_proyecto.py` | Sincroniza datos desde una copia de seguridad completa al proyecto. |
| `tools/check_room_schemas.py` | Comprueba que los esquemas Room exportados existan y sean JSON válidos. |
| `tools/check_assets_json.py` | Valida que los JSON de `app/src/main/assets/` carguen correctamente y resume conteos. |

## Ejemplos de comandos que había en la raíz

```bash
python3 auto_fotos_reales.py plants_1.json
python3 auto_fotos_reales.py mushrooms.json
python3 generate_bulk_images.py
python3 generate_dichotomous_keys.py
```

Sincronización desde backup:

```bash
# Vista previa: no escribe cambios
python3 sync_backup_a_proyecto.py "/ruta/PlantasToxicas_Backup_Completo.json" --dry-run

# Aplicar cambios
python3 sync_backup_a_proyecto.py "/ruta/PlantasToxicas_Backup_Completo.json"
```

Opciones útiles de `sync_backup_a_proyecto.py`:

| Flag | Para qué |
|---|---|
| `--dry-run` | Solo informa, no escribe. |
| `--solo-fotos` | Sincroniza únicamente imágenes. |
| `--no-nuevas` | No crea nuevos `plants_N.json` para IDs nuevos. |
| `--proyecto RUTA` | Ejecuta indicando otra carpeta de proyecto. |

## Documentación relacionada

- [`BACKUP_RESTORE.md`](BACKUP_RESTORE.md): notas sobre copias completas/incrementales y restauración segura.
