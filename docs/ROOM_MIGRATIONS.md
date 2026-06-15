# Room y migraciones

## Estado actual

La base de datos principal está definida en:

```text
app/src/main/java/com/toxicplants/database/PlantDatabase.kt
```

Actualmente declara:

```kotlin
@Database(..., version = 9, exportSchema = true)
```

Los esquemas exportados se guardan en:

```text
app/schemas/com.toxicplants.database.PlantDatabase/
```

En una revisión del repositorio se detectó que existen esquemas para varias versiones,
pero falta al menos el esquema `5.json` aunque la base de datos ya va por la versión 9.
Esto no impide necesariamente que la app compile, pero limita la fiabilidad de los tests
de migración con `MigrationTestHelper`.

## Comprobación rápida

Se añade el script:

```text
tools/check_room_schemas.py
```

Uso:

```bash
python3 tools/check_room_schemas.py
```

Si quieres que solo avise y no devuelva error:

```bash
python3 tools/check_room_schemas.py --warn-only
```

El script no modifica archivos. Solo comprueba:

- La versión declarada en `PlantDatabase.kt`.
- Que existan `1.json`, `2.json`, ..., `N.json`.
- Que cada archivo sea JSON válido.
- Que el campo `version` del JSON coincida con el nombre del archivo.

## Cómo regenerar esquemas Room

Desde Android Studio o desde terminal con Android SDK configurado:

```bash
./gradlew :app:compileDebugKotlin
```

También puede servir:

```bash
./gradlew :app:assembleDebug
```

Después revisa:

```bash
git status
```

Si Room/KSP genera o actualiza esquemas, confirma esos JSON en Git.

## Recomendaciones

1. Mantener siempre `exportSchema = true`.
2. Conservar todos los esquemas históricos desde `1.json` hasta la versión actual.
3. Añadir tests de migración para cadenas completas, no solo migraciones individuales.
4. Antes de publicar una versión con cambio de entidades:
   - subir `version` en `@Database`,
   - añadir `Migration(N-1, N)`,
   - compilar para generar `N.json`,
   - probar actualización desde una instalación previa.

## Ejemplo de test futuro recomendado

Cuando estén todos los esquemas disponibles, convendría ampliar los tests instrumentados
para validar rutas como:

- `1 -> 9`
- `2 -> 9`
- `4 -> 9`
- `8 -> 9`

Esto ayuda a detectar problemas en usuarios que actualizan desde versiones antiguas de la app.
