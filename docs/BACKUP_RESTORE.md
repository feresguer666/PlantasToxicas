# Copias de seguridad y restauración

## Política recomendada

La app puede hacer dos tipos de copia:

- **Completa**: todos los datos + todas las fotos.
- **Incremental**: solo datos/textos editables, **sin fotos**.

Esta política evita que la incremental vuelva a ocupar cientos de MB. Las fotos ya quedan
cubiertas por la copia completa.

## Punto crítico corregido

Antes, la copia incremental podía acabar pesando casi lo mismo que la completa porque se
incluían fotos. Además, restaurar un incremental no debe borrar carpetas de imágenes, ya que
un incremental no representa el estado completo de fotos.

## Comportamiento esperado

### Crear copia completa

- Incluye plantas, compuestos, setas, líquenes, avistamientos, calendario, ajustes y fotos.
- Es la copia grande. Úsala periódicamente o antes de cambiar de móvil.

### Crear copia incremental

- Incluye datos/textos editables.
- Incluye la lista de fichas de plantas borradas manualmente.
- No incluye fotos de plantas, setas ni avistamientos.
- Debe pesar mucho menos que la completa.

### Restaurar copia completa

- Reemplaza datos principales.
- Reemplaza carpetas de fotos incluidas en la copia.
- Deja el dispositivo igual que el backup completo.

### Restaurar copia incremental

- Restaura datos textuales incluidos.
- Restaura la lista de plantas borradas para que no reaparezcan al reiniciar.
- No borra fotos existentes.
- Si se importase una incremental antigua que trajera alguna foto, la mezclaría sin borrar el resto.

## Recomendación práctica

1. Haz una **copia completa** cuando quieras respaldar también fotos.
2. Usa **incrementales** para guardar cambios de datos entre copias completas.
3. Si cambias de móvil o reinstalas desde cero:
   - restaura primero una copia completa,
   - después, si procede, restaura la última incremental.

## Nota sobre calendario

Durante la restauración de datos principales se limpia también la tabla de eventos del calendario
para evitar que queden eventos antiguos mezclados con los restaurados.

## Plantas borradas manualmente

Las fichas de plantas eliminadas por el usuario se guardan como una lista de IDs (`deletedPlantIds`)
dentro del backup. Esto es necesario porque el catálogo base vive en `assets/plants_N.json` y la app
puede resembrar datos al arrancar. Al restaurar en otro móvil, esta lista evita que esas fichas
borradas reaparezcan desde los JSON base.

## Restauración fiel de listas vacías

Al importar un backup, los arrays vacíos también se aplican. Por ejemplo:

- `mushrooms: []` limpia favoritos/notas/custom locales de setas y vuelve al catálogo base.
- `lichens: []` limpia favoritos/notas/custom locales de líquenes y vuelve al catálogo base.
- `sightings: []` borra el historial de avistamientos restaurado, dejando la lista vacía.

Esto evita que datos antiguos del dispositivo queden mezclados cuando el backup indica una lista vacía.

## Fotos huérfanas de avistamientos

Después de restaurar la lista de avistamientos, la app limpia las fotos internas de
`files/sighting_photos/` que ya no están referenciadas por ningún avistamiento restaurado.
Esto evita que imágenes antiguas sigan ocupando espacio tras importar un backup con menos
avistamientos o con `sightings: []`.
