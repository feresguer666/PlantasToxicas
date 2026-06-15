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
- No incluye fotos de plantas, setas ni avistamientos.
- Debe pesar mucho menos que la completa.

### Restaurar copia completa

- Reemplaza datos principales.
- Reemplaza carpetas de fotos incluidas en la copia.
- Deja el dispositivo igual que el backup completo.

### Restaurar copia incremental

- Restaura datos textuales incluidos.
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
