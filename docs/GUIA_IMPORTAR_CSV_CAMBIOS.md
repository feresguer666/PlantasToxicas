# Guía: importar cambios desde CSV al catálogo JSON

Esta guía explica cómo usar el script:

```text
tools/import_csv_changes_to_assets.py
```

para aplicar cambios desde un archivo CSV a los JSON del catálogo:

```text
app/src/main/assets/plants_*.json
```

---

## 1. ¿Para qué sirve?

Sirve para editar datos de plantas en LibreOffice, Excel u otro editor de CSV y luego pasar esos cambios al catálogo base del proyecto.

Por ejemplo, puedes cambiar:

- descripción,
- síntomas,
- partes tóxicas,
- primeros auxilios,
- familia,
- categoría,
- imagen,
- notas,
- ubicación,
- favoritos.

---

## 2. Preparar el proyecto

Desde la raíz del proyecto:

```bash
cd ~/AndroidStudioProjects/PlantasToxicas
```

Asegúrate de que existe el script:

```bash
ls tools/import_csv_changes_to_assets.py
```

Dale permisos de ejecución:

```bash
chmod +x tools/import_csv_changes_to_assets.py
```

---

## 3. Crear carpeta para CSV

Recomendado:

```bash
mkdir -p imports
```

Ahí puedes guardar tus CSV de cambios.

---

## 4. Formato CSV largo

Este formato usa tres columnas:

```csv
id,field,value
```

Ejemplo:

```csv
id,field,value
123,description,"Nueva descripción botánica revisada."
123,symptoms,"Náuseas, vómitos, diarrea y posible afectación neurológica."
123,toxicParts,"Semillas y raíz."
```

Guárdalo, por ejemplo, como:

```text
imports/cambios.csv
```

---

## 5. Formato CSV ancho

También puedes usar un CSV con columnas de campos:

```csv
id,description,symptoms,toxicParts
123,"Nueva descripción","Nuevos síntomas","Semillas y raíz"
456,"Otra descripción","Otros síntomas","Hojas y frutos"
```

Este formato es cómodo para editar varias plantas en LibreOffice/Excel.

---

## 6. Campos permitidos de catálogo

Estos campos se pueden modificar sin opciones extra:

```text
commonName
commonNames
scientificName
family
toxicityLevel
toxicParts
symptoms
description
habitat
geographicDistribution
firstAid
imageUrl
category
floweringMonths
fruitingMonths
maxToxicityMonths
mythsAndLegends
```

---

## 7. Campos personales

Como la app es privada, también puedes modificar campos personales usando `--include-personal`:

```text
isFavorite
latitude
longitude
locationName
foundDate
notes
```

Ejemplo CSV:

```csv
id,field,value
123,notes,"Vista en el parque cercano. Revisar foto."
123,isFavorite,true
123,latitude,40.4168
123,longitude,-3.7038
123,locationName,"Madrid"
```

Para aplicar esos campos hay que usar:

```bash
--include-personal
```

---

## 8. Simular antes de aplicar

Siempre ejecuta primero en modo simulación:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --dry-run
```

Si tu CSV incluye notas, ubicación o favoritos:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --dry-run --include-personal
```

Esto **no modifica nada**. Solo muestra qué cambiaría.

---

## 9. Aplicar cambios

Si el `--dry-run` se ve correcto:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --apply
```

Con campos personales:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --apply --include-personal
```

---

## 10. Backup automático

Antes de escribir, el script crea una copia automática de los JSON que va a modificar en una carpeta como:

```text
backups_assets_csv_YYYYMMDD_HHMMSS/
```

Aun así, lo más importante es revisar siempre con Git.

---

## 11. Validar después de aplicar

Después de aplicar cambios, ejecuta:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

Debe terminar con algo como:

```text
Resultado: OK. Todos los JSON encontrados son válidos.
```

Puede mostrar avisos no bloqueantes. Eso es normal si hay plantas con campos vacíos.

---

## 12. Revisar cambios con Git

Mira qué se modificó:

```bash
git diff app/src/main/assets
```

Si quieres ver solo nombres de archivos:

```bash
git diff --name-only app/src/main/assets
```

---

## 13. Guardar cambios en Git

Si todo está bien:

```bash
git add app/src/main/assets/plants_*.json

git commit -m "data: importa cambios desde csv"

git push
```

Si también acabas de añadir el script/documentación:

```bash
git add tools/import_csv_changes_to_assets.py docs/IMPORT_CSV_CHANGES.md docs/SCRIPTS.md app/src/main/assets/plants_*.json

git commit -m "tools: importa cambios desde csv al catalogo"

git push
```

---

## 14. Si no te gustan los cambios

Puedes deshacer los cambios en los JSON con:

```bash
git restore app/src/main/assets/plants_*.json
```

También tienes la copia automática creada por el script en:

```text
backups_assets_csv_YYYYMMDD_HHMMSS/
```

---

## 15. Ejemplo completo

Crear CSV:

```bash
mkdir -p imports
nano imports/cambios.csv
```

Contenido:

```csv
id,field,value
1,notes,"Nota importada desde CSV."
1,isFavorite,true
1,description,"Descripción revisada desde CSV."
```

Simular:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --dry-run --include-personal
```

Aplicar:

```bash
python3 tools/import_csv_changes_to_assets.py imports/cambios.csv --apply --include-personal
```

Validar:

```bash
python3 tools/check_assets_json.py --top-level-only --validate-plants
```

Revisar:

```bash
git diff app/src/main/assets
```

Commit:

```bash
git add app/src/main/assets/plants_*.json
git commit -m "data: importa cambios desde csv"
git push
```

---

## 16. Notas sobre imágenes

El script ignora rutas de imagen locales no portables, por ejemplo:

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

Para pasar fotos del móvil al proyecto usa otro script:

```text
tools/extract_backup_images_to_assets.py
```

ese se usa con una **copia completa**, no con la incremental.
