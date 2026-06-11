# 🌿 Plantas Tóxicas

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Design-Material%203-757575?logo=materialdesign&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](#-licencia)

> Aplicación Android nativa **divulgativa y educativa** sobre la toxicología de plantas, hongos y líquenes: consulta, identifica por foto, geolocaliza y explora más de **10.000 especies** con sus síntomas, fitoquímica, primeros auxilios y mucho más.

---

## 📑 Índice

- [Características](#-características)
- [Estadísticas del catálogo](#-estadísticas-del-catálogo)
- [Arquitectura y Novedades](#-arquitectura-y-novedades)
- [Stack técnico](#-stack-técnico)
- [Estructura del proyecto](#-estructura-del-proyecto)
- [Instalación y Configuración (API Keys)](#-instalación-y-configuración-api-keys)
- [Cómo actualizar el catálogo](#-cómo-actualizar-el-catálogo)
- [Aviso legal](#%EF%B8%8F-aviso-legal)
- [Contribuir](#-contribuir)
- [Licencia](#-licencia)

---

## ✨ Características

El proyecto ha evolucionado masivamente y ahora incluye:

- 📚 **Macro Catálogo offline** de más de **10.000 plantas**, dividido temáticamente.
- 🍄 **Hongos, Líquenes y Psicotrópicas**: Bases de datos dedicadas a toxicología fúngica, liquénica y botánica psicotrópica.
- 🔬 **Fitoquímica y Compuestos**: Explora más de 240 compuestos tóxicos, sus interacciones químicas y métodos de extracción/identificación.
- 📸 **Identificación Inteligente**: Reconocimiento por foto (cámara o galería) a través de Pl@ntNet API y análisis con IA.
- 🗺️ **Historial y Mapa de Avistamientos**: Geolocalización de hallazgos con OSMDroid y visualización histórica.
- 🧮 **Calculadoras de Riesgo y Dosis Letal**: Herramientas integradas para calcular índices de toxicidad.
- 👨‍👩‍👧‍👦 **Seguridad Específica**: Secciones dedicadas a la prevención y seguridad para niños, mascotas y ganado.
- 🏥 **Buscador de Síntomas y Síndromes Tóxicos**: Diagnóstico diferencial basado en observaciones clínicas.
- 🔎 **Claves Dicotómicas**: Identificación botánica tradicional paso a paso.
- 📱 **Realidad Aumentada (AR)**: Visualización inmersiva usando ARCore.
- 🤖 **Asistente de IA Integrado**: Resuelve dudas sobre botánica y toxicología usando la API de Groq o Gemini.

---

## 📊 Estadísticas del catálogo

> Generadas a partir de la colección en `app/src/main/assets/`.

- **Plantas (General)**: Más de 10.000 especies divididas en 22 archivos JSON.
- **Plantas Psicotrópicas**: 772 especies (`psychotropic_plants.json`).
- **Hongos Tóxicos**: 328 especies (`mushrooms.json`).
- **Líquenes**: 107 especies (`lichens.json`).
- **Compuestos Fitoquímicos**: 246 componentes (`compounds.json`).
- **Claves Dicotómicas & Glosario**: Contenido botánico estructurado y guiado con recursos fotográficos.

---

## 🏗 Arquitectura y Novedades

La aplicación sigue el patrón **MVVM** clásico con Single Source of Truth basado en Room, pero ha expandido su alcance para abarcar más de 45 pantallas creadas en Jetpack Compose (`app/src/main/java/com/toxicplants/database/ui/screens/`).

Nuevas áreas de la arquitectura:
- Integración de **Modelos LLM (IA)**: Módulos configurados para Groq y Gemini.
- Identificación fotográfica no solo para plantas (Pl@ntNet) sino también con utilidades extendidas de Machine Learning / IA para Hongos y Líquenes.
- Integración con **OSMDroid** para la navegación sin depender completamente de Google Maps.
- Visualización de escenas 3D (AR) mediante `arsceneview`.

---

## 🧰 Stack técnico actualizado

| Capa | Herramientas |
|---|---|
| SDK y Lenguaje | **Target SDK 36** / Min SDK 26 · Kotlin 2.x (Java 17) |
| UI | Jetpack Compose + Material 3 + Lottie |
| Navegación | `androidx.navigation:navigation-compose` |
| Persistencia | Room 2.7 + KSP + GSON |
| Imágenes / AR | Coil 2.5, CameraX, ARCore 1.41.0, ARSceneview 2.0.3 |
| Red | OkHttp 4.12 |
| Ubicación / Mapa | Google Play Services Location 21.1, OSMDroid 6.1.18 |
| Integraciones de IA | Pl@ntNet REST API v2, **Groq API**, **Gemini API** |
| Asincronía | Kotlin Coroutines + Flow |

---

## 📁 Estructura del proyecto

El directorio de pantallas (`app/src/main/java/com/toxicplants/database/ui/screens/`) es el corazón de la nueva experiencia. Algunas de las principales:

- `HomeScreen.kt`, `GlobalSearchScreen.kt`, `SettingsScreen.kt`
- `PlantDetailScreen.kt`, `ToxicMushroomsScreen.kt`, `ToxicLichensScreen.kt`
- `PsychotropicPlantsScreen.kt`, `PhytochemistryScreen.kt`, `CompoundDetailScreen.kt`
- `SightingsMapScreen.kt`, `SightingsHistoryScreen.kt`
- `LethalDoseCalculatorScreen.kt`, `RiskCalculatorScreen.kt`
- `ARScreen.kt`, `AssistantScreen.kt`, `DichotomousKeyScreen.kt`
- `SearchBySymptomsScreen.kt`, `ToxicSyndromesScreen.kt`
- `ChildSafetyScreen.kt`, `PetSafetyScreen.kt`, `LivestockSafetyScreen.kt`

---

## ⚡ Instalación y Configuración (API Keys)

### Requisitos
- Android Studio Ladybug (2024.2) o superior.
- Dispositivo físico o emulador con **Android 8.0 (API 26)** o superior.

### Pasos
```bash
git clone https://github.com/feresguer666/PlantasToxicas.git
cd PlantasToxicas
```

### Gestión de Variables de Entorno y API Keys

La aplicación ahora depende de tres servicios externos que requieren sus propias claves API. Para compilar la app correctamente, debes configurarlas.

Crea (o edita) un archivo llamado `local.properties` en la raíz del proyecto (este archivo se ignora en Git por seguridad):

```properties
PLANTNET_API_KEY=tu_api_key_de_plantnet_aqui
GROQ_API_KEY=tu_api_key_de_groq_aqui
GEMINI_API_KEY=tu_api_key_de_gemini_aqui
```

*Si no dispones de todas, puedes dejarlas en blanco, pero las funciones que dependan de ellas (como el asistente IA o la identificación de fotos) no funcionarán.*

El script `build.gradle.kts` inyectará estas variables como `BuildConfig` durante la compilación.

---

## 🛠 Cómo actualizar el catálogo

El inmenso repositorio de datos reside en la carpeta `app/src/main/assets/`. Debido a la escala del proyecto, la información ya no se guarda en un solo JSON. En su lugar se emplean:

- `plants_1.json` a `plants_22.json`
- `mushrooms.json`
- `lichens.json`
- `psychotropic_plants.json`
- `compounds.json`
- `dichotomous_keys.json` y `glossary.json`

Para agregar, modificar o arreglar registros, edita los archivos JSON directamente. 
> Puedes usar los scripts Python (ej. `arreglar_imagenes_json.py`, `rellenar_imagenes_json.py` o `convert_screens.py`) incluidos en la raíz del repositorio para mantenimiento masivo de enlaces e imágenes.

---

## ⚖️ Aviso legal

> Esta aplicación tiene fines **exclusivamente divulgativos y educativos**.
> La información que ofrece **no sustituye al criterio de un profesional sanitario, veterinario ni botánico**.
>
> En caso de sospecha de intoxicación por planta, hongo o liquen:
> - 🇪🇸 **Instituto Nacional de Toxicología (España)**: **91 562 04 20** (24 h, 365 días)
> - 🇪🇺 Acude inmediatamente al servicio de urgencias médicas.
>
> El asistente de IA y las calculadoras integradas **pueden sufrir alucinaciones o errores de estimación**. Los autores no se hacen responsables del uso indebido de esta información o diagnósticos erróneos basados en la app.

---

## 🤝 Contribuir

¡Las contribuciones son siempre bienvenidas! Para el tamaño actual del proyecto, la ayuda revisando los JSONs (nombres científicos, rectificando fotos rotas) o añadiendo tests es de gran valor.

1. Haz un fork.
2. Crea una rama: `git checkout -b feat/mi-mejora`
3. Commitea: `git commit -m "feat: descripción breve"`
4. Push: `git push origin feat/mi-mejora`
5. Abre un Pull Request.

---

## 📄 Licencia

Distribuido bajo licencia **MIT**. Ver [`LICENSE`](LICENSE) para más detalles.

El contenido fotográfico referenciado, en su gran mayoría extraído de Wikimedia Commons o bases de datos libres, conserva las licencias de sus autores originales (CC BY-SA, CC0, etc.). 

<p align="center">
  Hecho con 🌱, 🍄 y Jetpack Compose
</p>
