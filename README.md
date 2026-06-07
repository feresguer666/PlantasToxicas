# 🌿 Plantas Tóxicas

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](#-licencia)

> Aplicación Android nativa **divulgativa y educativa** sobre plantas tóxicas:
> consulta, identifica por foto y geolocaliza más de **1.800 especies** con sus
> síntomas, partes tóxicas, primeros auxilios y distribución geográfica.

---

## 📑 Índice

- [Características](#-características)
- [Capturas](#-capturas)
- [Estadísticas del catálogo](#-estadísticas-del-catálogo)
- [Arquitectura](#-arquitectura)
- [Stack técnico](#-stack-técnico)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Instalación](#-instalación)
- [Configuración de la API key de Pl@ntNet](#-configuración-de-la-api-key-de-plantnet)
- [Cómo actualizar el catálogo](#-cómo-actualizar-el-catálogo)
- [Roadmap](#-roadmap)
- [Aviso legal](#%EF%B8%8F-aviso-legal)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)

---

## ✨ Características

- 📚 **Catálogo offline** de 1.803 plantas con ficha completa: nombre común y
  científico, familia, nivel de toxicidad, partes tóxicas, síntomas,
  descripción, hábitat, distribución geográfica y primeros auxilios.
- 🔎 **Búsqueda** por nombre común, científico o categoría.
- 🗂 **Filtros** por categoría (interior, exterior, silvestre, jardín…),
  familia botánica y nivel de toxicidad.
- 📷 **Identificación por foto** mediante la API de
  [Pl@ntNet](https://my.plantnet.org/) (cámara o galería).
- 📍 **Geolocalización** de cada avistamiento con `FusedLocationProvider` +
  `Geocoder` para nombrar la ubicación.
- ⭐ **Favoritos** persistentes.
- ✏️ **Añadir / editar / borrar** plantas propias.
- 🆘 **Acceso rápido a emergencias toxicológicas** (España: 91 562 04 20).
- 🌐 Resolución automática de imágenes desde múltiples fuentes con _fallback_:
  Wikipedia (REST + MediaWiki), Wikimedia Commons, Encyclopedia of Life,
  iNaturalist y BioStor.
- 🖼 **Caché local** de imágenes (sobrevive a la falta de red).

---

## 📱 Capturas

> _Pendiente_. Lanza la app desde Android Studio y reemplaza estos placeholders.

| Inicio | Detalle | Categorías |
|---|---|---|
| _screenshot_home.png_ | _screenshot_detail.png_ | _screenshot_categories.png_ |

---

## 📊 Estadísticas del catálogo

> Generadas a partir de `app/src/main/assets/plants.json`.

- **Total**: 1.803 plantas
- **Por nivel de toxicidad** (top):
  - Alto: 538
  - Bajo: 469
  - Moderado: 248
  - Mortal: 161
  - Muy alto: 49
- **Por categoría** (top): Exterior (572), Silvestre (351), Jardín (229),
  Tropical (118), Interior (79), Montaña (68), Huerto (45).
- **Familias mejor representadas**: Fabaceae (121), Asteraceae (113),
  Ranunculaceae (105), Solanaceae (101), Euphorbiaceae (93), Apiaceae (87),
  Araceae (55), Asparagaceae (53), Lamiaceae (51), Rosaceae (49).

> ⚠️ Hay inconsistencias menores en el campo `toxicityLevel` (`"Alto"` vs
> `"Alta"`, `"Bajo"` vs `"Baja"`, `"Media"` vs `"Moderada"`). Pendiente de
> normalización — ver [Roadmap](#-roadmap).

---

## 🏗 Arquitectura

```
┌──────────────────────────────────────────────────────┐
│                Compose UI (screens/)                 │
│   HomeScreen · PlantList · Detail · Search · …       │
└────────────────────┬─────────────────────────────────┘
                     │ observeAsState / collectAsState
┌────────────────────▼─────────────────────────────────┐
│       PlantViewModel (AndroidViewModel)              │
│        StateFlow + LiveData hacia la UI              │
└────────────────────┬─────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────┐
│              PlantRepository                         │
└────────────────────┬─────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────┐
│         Room: PlantDatabase + PlantDao               │
│                                                      │
│   Seed inicial ← assets/plants.json (Callback.onCreate)
└──────────────────────────────────────────────────────┘
```

Patrón **MVVM** con _single source of truth_ en Room. La precarga de las 1.803
plantas se hace una sola vez, al crear la base de datos, leyendo
`app/src/main/assets/plants.json`.

### Identificación por foto

```
CameraIdentifyScreen ─POST multipart─▶ Pl@ntNet API ─JSON─▶ PlantNetResultScreen
                       (OkHttp)                              + match con catálogo local
```

### Resolución de imágenes (cascada)

```
LocalCache → Wiki (sci) → Wiki (común) → Commons (sci) → Commons (común)
            → BioStor → EOL → iNaturalist
```

---

## 🧰 Stack técnico

| Capa | Herramientas |
|---|---|
| Lenguaje | Kotlin 2.x |
| UI | Jetpack Compose + Material 3 |
| Navegación | `androidx.navigation:navigation-compose` |
| Persistencia | Room 2.7 + KSP |
| Imágenes | Coil 2.5 |
| Red | OkHttp 4.12 |
| Ubicación | Google Play Services Location 21.1 |
| Identificación | [Pl@ntNet REST API v2](https://my.plantnet.org/doc) |
| Asincronía | Kotlin Coroutines + Flow |
| Build | Gradle Kotlin DSL + Version Catalog (`libs.versions.toml`) |
| `minSdk` / `targetSdk` | 26 / 34 |

---

## 📁 Estructura del proyecto

```
PlantasToxicas/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── plants.json              ← Catálogo (1.803 especies)
│       ├── java/com/toxicplants/database/
│       │   ├── PlantApp.kt              ← Application + ImageLoaderFactory (Coil)
│       │   ├── PlantEntity.kt           ← @Entity Room
│       │   ├── PlantDao.kt              ← @Dao
│       │   ├── PlantDatabase.kt         ← @Database + seed desde assets
│       │   ├── PlantDataSource.kt       ← Loader del JSON (54 líneas)
│       │   ├── data/repository/
│       │   │   └── PlantRepository.kt
│       │   └── ui/
│       │       ├── MainActivity.kt
│       │       ├── ImageDownloader.kt   ← Cascada de fuentes
│       │       ├── LocalImageCache.kt
│       │       ├── WikiImageFetcher.kt
│       │       ├── navigation/PlantNavGraph.kt
│       │       ├── screens/             ← Compose screens
│       │       │   ├── HomeScreen.kt
│       │       │   ├── PlantListScreen.kt
│       │       │   ├── PlantDetailScreen.kt
│       │       │   ├── SearchScreen.kt
│       │       │   ├── CategoriesScreen.kt
│       │       │   ├── EmergencyScreen.kt
│       │       │   ├── CameraIdentifyScreen.kt
│       │       │   ├── PlantNetResultScreen.kt
│       │       │   ├── LocationScreen.kt
│       │       │   ├── DownloadImagesScreen.kt
│       │       │   ├── EditPlantScreen.kt
│       │       │   └── OnlineDatabasesScreen.kt
│       │       ├── theme/               ← ToxicPlantsTheme (verde)
│       │       └── viewmodel/PlantViewModel.kt
│       └── res/                         ← Drawables, strings, themes XML
├── gradle/
│   └── libs.versions.toml               ← Version catalog
├── build.gradle.kts
├── settings.gradle.kts
├── arreglar_imagenes.py                 ← Script para reparar URLs de imágenes
└── README.md
```

---

## ⚡ Instalación

### Requisitos

- **Android Studio** Ladybug (2024.2) o superior
- **JDK 17**
- **Android SDK** 34
- Dispositivo o emulador con **Android 8.0 (API 26)** o superior

### Pasos

```bash
git clone https://github.com/feresguer666/PlantasToxicas.git
cd PlantasToxicas
```

1. Abre el proyecto en Android Studio.
2. Espera a que Gradle sincronice.
3. (Opcional) Configura tu API key de Pl@ntNet — ver siguiente sección.
4. Pulsa ▶️ **Run** sobre el módulo `app`.

> La primera vez que se ejecuta, Room crea la base de datos e importa el
> catálogo desde `assets/plants.json` (puede tardar 1-2 s).

---

## 🔑 Configuración de la API key de Pl@ntNet

La función "Identificar con foto" necesita una **API key gratuita** de
[my.plantnet.org](https://my.plantnet.org/).

> 🚨 **No commitees tu API key**. Si la tuya estuvo expuesta antes en el
> historial de Git, revócala desde el panel de Pl@ntNet y genera una nueva.

### Forma recomendada (`local.properties` + `BuildConfig`)

1. Edita `local.properties` (este archivo está en `.gitignore`):

   ```properties
   PLANTNET_API_KEY=tu_api_key_aqui
   ```

2. Añade en `app/build.gradle.kts`:

   ```kotlin
   android {
       defaultConfig {
           buildConfigField(
               "String",
               "PLANTNET_API_KEY",
               "\"${project.findProperty("PLANTNET_API_KEY") ?: ""}\""
           )
       }
       buildFeatures {
           buildConfig = true
           compose = true
       }
   }
   ```

3. En `CameraIdentifyScreen.kt` sustituye la constante por:

   ```kotlin
   private val PLANTNET_API_KEY = BuildConfig.PLANTNET_API_KEY
   ```

---

## 🛠 Cómo actualizar el catálogo

El catálogo vive en `app/src/main/assets/plants.json`. Para añadir o modificar
plantas no hace falta tocar Kotlin: edita el JSON directamente.

Esquema de cada entrada:

```json
{
  "id": 1,
  "commonName": "Dieffenbachia",
  "scientificName": "Dieffenbachia seguine",
  "family": "Araceae",
  "toxicityLevel": "Alta",
  "toxicParts": "Todas las partes",
  "symptoms": "Irritación severa de boca…",
  "description": "Planta herbácea perenne…",
  "habitat": "Interior de hogares, oficinas…",
  "geographicDistribution": "Nativa de América Central y del Sur…",
  "firstAid": "Lavar la boca con agua…",
  "imageUrl": "https://upload.wikimedia.org/…",
  "category": "Doméstica",
  "isFavorite": false,
  "latitude": null,
  "longitude": null,
  "locationName": null,
  "foundDate": null,
  "notes": null
}
```

Para **reparar URLs de imágenes rotas** existe un script Python:

```bash
python3 arreglar_imagenes.py
```

Busca cada planta en Wikipedia y Commons y sustituye las URLs vacías o
inválidas por una imagen válida en `upload.wikimedia.org`.

---

## 🗺 Roadmap

- [ ] **Normalizar** `toxicityLevel` a un enum cerrado
      (`MORTAL · ALTO · MODERADO · BAJO · DESCONOCIDO`).
- [ ] **Migración Room v1 → v2** con los campos de ubicación (actualmente
      añadidos sin bump de versión).
- [ ] **Tests unitarios** del DAO con `Room.inMemoryDatabaseBuilder`.
- [ ] **Mapa** con los avistamientos marcados (Maps Compose).
- [ ] Modo **dark** completo (la app ya soporta `ToxicPlantsTheme`, falta la
      variante oscura).
- [ ] **Internacionalización** (EN/FR/PT — el dataset ya tiene nombres
      científicos universales).
- [ ] **Lint / Detekt** + CI con GitHub Actions.
- [ ] Reemplazar `org.json` por **kotlinx.serialization** para parsear el JSON.

---

## ⚖️ Aviso legal

> Esta aplicación tiene fines **exclusivamente divulgativos y educativos**.
> La información que ofrece **no sustituye al criterio de un profesional
> sanitario, veterinario ni botánico**.
>
> En caso de sospecha de intoxicación por planta:
>
> - 🇪🇸 **Instituto Nacional de Toxicología (España)**: **91 562 04 20**
>   (24 h, 365 días)
> - 🇪🇺 Acude inmediatamente al servicio de urgencias más cercano.
>
> Los autores no se hacen responsables del uso indebido de esta información.
> Las identificaciones por foto mediante Pl@ntNet son **orientativas** y
> pueden contener errores: nunca consumas una planta basándote únicamente
> en esta app.

---

## 🤝 Contribuir

Las contribuciones son bienvenidas:

1. Haz un fork
2. Crea una rama: `git checkout -b feat/mi-mejora`
3. Commitea: `git commit -m "feat: descripción breve"`
4. Push: `git push origin feat/mi-mejora`
5. Abre un Pull Request

Buenas adiciones especialmente útiles:

- Nuevas especies (sobre todo nativas no europeas).
- Correcciones de fichas (síntomas, primeros auxilios, sinónimos).
- Mejores fotos en `upload.wikimedia.org` (CC-compatible).
- Tests y mejoras de arquitectura.

---

## 📄 Licencia

Distribuido bajo licencia **MIT**. Ver [`LICENSE`](LICENSE) para más detalles.

El dataset de plantas integrado en `assets/plants.json` se ofrece con la misma
licencia. Las imágenes referenciadas vía URL pertenecen a sus respectivos
autores y se sirven mayoritariamente desde Wikimedia Commons bajo licencias
libres (CC BY-SA, CC0, etc.).

---

<p align="center">
  Hecho con 🌱 y Jetpack Compose
</p>
