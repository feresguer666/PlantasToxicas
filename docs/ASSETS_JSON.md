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

Para revisar además campos mínimos de las plantas:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

Por defecto, los campos presentes pero vacíos se informan como **avisos** para no bloquear
catálogos en curso de mejora. Si quieres que esos avisos hagan fallar el comando, usa:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants --strict-plants
```

## Qué comprueba

- Que todos los `.json` encontrados sean JSON válidos.
- Que puedan leerse como UTF-8.
- El tipo de dato principal de cada archivo: lista, diccionario, etc.
- El número de elementos de cada catálogo principal.
- La secuencia `plants_N.json`, avisando si falta algún número intermedio.
- Con `--validate-plants`, campos mínimos de cada planta, IDs duplicados y niveles de toxicidad conocidos.

## Qué no hace

El script **no modifica archivos**. Solo lee y muestra un informe.

La validación interna de plantas es básica: comprueba presencia de campos obligatorios,
IDs y campos vacíos. No corrige datos ni valida todavía todos los campos específicos de
cada pantalla.

## Cuándo ejecutarlo

Recomendado después de:

- Editar manualmente cualquier JSON de `app/src/main/assets/`.
- Ejecutar scripts de mantenimiento como `rellenar_imagenes_json.py` o `sync_backup_a_proyecto.py`.
- Antes de compilar una versión que quieras conservar.
- Antes de subir cambios de catálogo a GitHub.
