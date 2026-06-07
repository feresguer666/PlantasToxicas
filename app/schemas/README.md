# Esquemas Room

Este directorio guarda el JSON de cada versión del esquema de
[`PlantDatabase`](../src/main/java/com/toxicplants/database/PlantDatabase.kt).

KSP los genera automáticamente en cada build gracias a la configuración de
`app/build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

**Estos archivos DEBEN commitearse**: `MigrationTestHelper` los necesita para
poder crear una BD en una versión anterior y replicar la migración.

## ⚠️ Nota sobre `1.json`

El `1.json` incluido inicialmente en este repo es un **placeholder** con la
estructura correcta del esquema v1, pero con `identityHash` ficticio. La
primera vez que ejecutes:

```bash
./gradlew :app:compileDebugKotlin
```

KSP **regenerará el archivo** con el `identityHash` real calculado a partir
del `@Entity`. Sustituye el archivo placeholder por el regenerado y
commitéalo.

Si modificas `PlantEntity`, recuerda:

1. Subir `version` en `@Database(version = N)`.
2. Añadir un `Migration(N-1, N)` en `PlantDatabase.kt`.
3. Buildear → KSP genera `N.json`.
4. Añadir un test en `MigrationTest.kt`.
