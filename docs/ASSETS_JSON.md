# Validación de JSON del catálogo

La app depende mucho de los datos offline guardados en:

```text
app/src/main/assets/
```

Para evitar fallos por una coma mal puesta, codificación incorrecta o un archivo de plantas
fuera de secuencia, se añade el script:

```text
tools/check_assets_json.py
```

## Uso

Desde la raíz del proyecto:

```bash
python3 tools/check_assets_json.py
```

También puedes validar solo los JSON directamente dentro de `assets/`, ignorando subcarpetas
como manifiestos de fotos:

```bash
python3 tools/check_assets_json.py --top-level-only
```

## Qué comprueba

- Que todos los `.json` encontrados sean JSON válidos.
- Que puedan leerse como UTF-8.
- El tipo de dato principal de cada archivo: lista, diccionario, etc.
- El número de elementos de cada catálogo principal.
- La secuencia `plants_N.json`, avisando si falta algún número intermedio.

## Qué no hace

El script **no modifica archivos**. Solo lee y muestra un informe.

Tampoco valida todavía el esquema interno de cada planta/campo toxicológico. Eso podría añadirse
en un paso posterior si se quiere comprobar campos obligatorios como `commonName`,
`scientificName`, `toxicityLevel`, `imageUrl`, etc.

## Cuándo ejecutarlo

Recomendado después de:

- Editar manualmente cualquier JSON de `app/src/main/assets/`.
- Ejecutar scripts de mantenimiento como `rellenar_imagenes_json.py` o `sync_backup_a_proyecto.py`.
- Antes de compilar una versión que quieras conservar.
- Antes de subir cambios de catálogo a GitHub.
