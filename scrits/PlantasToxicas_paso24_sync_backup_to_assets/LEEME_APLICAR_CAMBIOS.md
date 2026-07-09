# Paso 24: sincronizar backup de la app hacia assets/plants_*.json

## Qué añade

Nuevo script:

```text
tools/sync_backup_to_assets.py
```

Sirve para pasar cambios hechos desde la app Android al catálogo JSON del proyecto:

```text
app/src/main/assets/plants_*.json
```

## Uso básico

Primero siempre simula:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz --dry-run
```

Para tu caso privado, con todo activado:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz \
  --dry-run \
  --include-personal \
  --only-edited \
  --create-new \
  --delete-removed
```

Si el resumen te gusta, aplica:

```bash
python3 tools/sync_backup_to_assets.py backups/mi_backup.json.gz \
  --apply \
  --include-personal \
  --only-edited \
  --create-new \
  --delete-removed
```

Luego valida:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

Y revisa:

```bash
git diff app/src/main/assets
```

## Opciones importantes

- `--dry-run`: no escribe nada.
- `--apply`: escribe cambios.
- `--include-personal`: incluye favoritos, notas y ubicaciones.
- `--only-edited`: solo actualiza plantas marcadas como editadas.
- `--create-new`: añade plantas nuevas del backup a `plants_*.json`.
- `--delete-removed`: borra de los JSON plantas incluidas en `deletedPlantIds`.

## Seguridad

Antes de escribir, crea copia automática de los JSON candidatos en:

```text
backups_assets_YYYYMMDD_HHMMSS/
```

También ignora rutas de imagen locales del móvil como `content://` o `file:///data/...`.

## Documentación

Ver:

```text
docs/SYNC_BACKUP_TO_ASSETS.md
```

## Archivos incluidos

- `tools/sync_backup_to_assets.py`
- `docs/SYNC_BACKUP_TO_ASSETS.md`
- `docs/SCRIPTS.md`
