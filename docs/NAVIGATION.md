# Navegación de la app

## Estado actual

La app arranca desde `MainActivity`:

```kotlin
setContent {
    MainApp()
}
```

El grafo de navegación activo está definido directamente en:

```text
app/src/main/java/com/toxicplants/database/ui/MainActivity.kt
```

y, en concreto, en la función:

```kotlin
@Composable
fun MainApp()
```

También existe este archivo:

```text
app/src/main/java/com/toxicplants/database/ui/navigation/PlantNavGraph.kt
```

Ese `PlantNavGraph` parece una versión anterior o parcial del grafo de navegación. En la
revisión actual no se observa que `MainActivity` lo invoque directamente.

## Cómo comprobar referencias

Desde la raíz del proyecto:

```bash
grep -RIn "PlantNavGraph\|MainApp()" app/src/main/java/com/toxicplants/database --include='*.kt'
```

Resultado esperado actualmente:

```text
app/src/main/java/com/toxicplants/database/ui/MainActivity.kt: MainApp()
app/src/main/java/com/toxicplants/database/ui/MainActivity.kt: fun MainApp()
app/src/main/java/com/toxicplants/database/ui/navigation/PlantNavGraph.kt: fun PlantNavGraph(...)
```

## Riesgo de mantener dos grafos

Tener rutas en dos sitios aumenta el riesgo de:

- Pantallas añadidas en `MainActivity.kt` pero no en `PlantNavGraph.kt`.
- Firmas de callbacks que cambian en una pantalla y rompen el grafo alternativo.
- Rutas con nombres diferentes para la misma pantalla.
- Dificultad para saber cuál es la fuente de verdad.

## Plan seguro recomendado

No borrar `PlantNavGraph.kt` de golpe. Hacerlo por pasos:

1. Mantener `MainActivity.MainApp()` como fuente de verdad mientras la app compila.
2. Comparar rutas de `MainActivity.kt` y `PlantNavGraph.kt`.
3. Decidir una arquitectura final:
   - Opción A: mover gradualmente el grafo activo a `PlantNavGraph.kt`.
   - Opción B: eliminar `PlantNavGraph.kt` si se confirma que no se usa y no aporta rutas únicas.
4. En cualquiera de las dos opciones, hacer un commit pequeño y compilar.
5. Probar navegación principal:
   - Home.
   - Lista y detalle de plantas.
   - Categorías y familias.
   - Búsqueda global.
   - Fitoquímica y compuestos.
   - Cámara / Pl@ntNet.
   - Setas, líquenes y psicotrópicas.
   - Ajustes.

## Cambio aplicado en este paso

Solo se añade documentación y un comentario de mantenimiento. No se elimina código ni se cambia
la lógica de navegación activa.
