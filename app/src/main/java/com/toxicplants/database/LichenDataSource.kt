package com.toxicplants.database

import android.content.Context
import org.json.JSONArray

/** Carga el catálogo inicial de líquenes tóxicos desde `assets/lichens.json`. */
object LichenDataSource {

    private const val ASSET_FILE = "lichens.json"

    fun loadAll(context: Context): List<LichenEntity> {
        return try {
            loadFromAssets(context)
        } catch (_: Exception) {
            // Respaldo mínimo para que compile y la sección no se caiga si el asset
            // no está empaquetado. El catálogo completo vive en assets/lichens.json.
            fallbackItems
        }
    }

    private fun loadFromAssets(context: Context): List<LichenEntity> {
        val text = context.assets.open(ASSET_FILE)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<LichenEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += LichenEntity(
                id = o.optInt("id", 0),
                commonName = o.optString("commonName", ""),
                scientificName = o.optString("scientificName", ""),
                family = o.optString("family", ""),
                toxicityLevel = o.optString("toxicityLevel", ""),
                syndrome = o.optString("syndrome", ""),
                toxicCompounds = o.optString("toxicCompounds", ""),
                onsetTime = o.optString("onsetTime", ""),
                symptoms = o.optString("symptoms", ""),
                description = o.optString("description", ""),
                habitat = o.optString("habitat", ""),
                geographicDistribution = o.optString("geographicDistribution", ""),
                confusions = o.optString("confusions", ""),
                firstAid = o.optString("firstAid", ""),
                treatment = o.optString("treatment", ""),
                notes = o.optString("notes", ""),
                imageUrl = o.optString("imageUrl", ""),
                isHighRisk = o.optBoolean("isHighRisk", false),
                isFavorite = o.optBoolean("isFavorite", false),
            )
        }
        return out.ifEmpty { fallbackItems }
    }

    private val fallbackItems: List<LichenEntity> = listOf(
        LichenEntity(
            id = 1,
            commonName = "Liquen de los lobos",
            scientificName = "Letharia vulpina",
            family = "Parmeliaceae",
            toxicityLevel = "Alta",
            syndrome = "Vulpínico",
            toxicCompounds = "Ácido vulpínico y derivados pulvinicos",
            onsetTime = "30 min-6 h",
            symptoms = "Náuseas, vómitos, diarrea y dolor abdominal; en animales se describen cuadros graves tras ingestión.",
            description = "Liquen amarillo verdoso históricamente asociado a cebos tóxicos. No debe ingerirse.",
            habitat = "Coníferas de montaña, ramas y cortezas ácidas.",
            geographicDistribution = "Europa, Norteamérica y regiones montanas del hemisferio norte.",
            confusions = "Usnea amarillentas, Vulpicida pinastri.",
            firstAid = "Evitar consumo; si hay ingesta accidental, conservar muestra y consultar toxicología.",
            treatment = "Tratamiento sintomático, hidratación y vigilancia clínica.",
            notes = "Información divulgativa. No usar líquenes en remedios caseros.",
            isHighRisk = true
        ),
        LichenEntity(
            id = 2,
            commonName = "Vulpicida de pino",
            scientificName = "Vulpicida pinastri",
            family = "Parmeliaceae",
            toxicityLevel = "Alta",
            syndrome = "Vulpínico",
            toxicCompounds = "Ácido vulpínico",
            onsetTime = "30 min-6 h",
            symptoms = "Irritación gastrointestinal con náuseas, vómitos y diarrea.",
            description = "Liquen amarillo de coníferas con compuestos pulvinicos potencialmente tóxicos.",
            habitat = "Cortezas de coníferas y madera muerta en zonas frías.",
            geographicDistribution = "Europa, Asia y Norteamérica boreal.",
            confusions = "Cetraria y otros líquenes amarillos.",
            firstAid = "No ingerir; consultar si hay síntomas.",
            treatment = "Soporte e hidratación.",
            isHighRisk = true
        ),
        LichenEntity(
            id = 3,
            commonName = "Usnea común",
            scientificName = "Usnea spp.",
            family = "Parmeliaceae",
            toxicityLevel = "Moderada",
            syndrome = "Úsnico/hepatotóxico",
            toxicCompounds = "Ácido úsnico",
            onsetTime = "Horas-días",
            symptoms = "Molestias digestivas; preparados concentrados con ácido úsnico se han asociado a daño hepático.",
            description = "Grupo de líquenes fruticulosos conocidos como barbas de viejo. Evitar su uso interno no controlado.",
            habitat = "Ramas de árboles en ambientes limpios y húmedos.",
            geographicDistribution = "Cosmopolita.",
            confusions = "Ramalina, Alectoria y Bryoria.",
            firstAid = "Consultar si hay dolor abdominal, ictericia, orina oscura o malestar persistente.",
            treatment = "Soporte médico y control hepático si hay ingesta significativa."
        ),
        LichenEntity(
            id = 4,
            commonName = "Xanthoria parietina",
            scientificName = "Xanthoria parietina",
            family = "Teloschistaceae",
            toxicityLevel = "Baja",
            syndrome = "Fototóxico",
            toxicCompounds = "Antraquinonas y pigmentos fenólicos",
            onsetTime = "Horas tras contacto/luz",
            symptoms = "Posible irritación cutánea y fotosensibilidad en personas susceptibles.",
            description = "Liquen anaranjado frecuente en cortezas enriquecidas en nutrientes y rocas costeras.",
            habitat = "Cortezas, rocas y zonas nitrificadas.",
            geographicDistribution = "Cosmopolita.",
            confusions = "Caloplaca y otros Teloschistaceae.",
            firstAid = "Lavar la zona y evitar sol si hay irritación.",
            treatment = "Tratamiento sintomático de dermatitis."
        ),
        LichenEntity(
            id = 5,
            commonName = "Cetraria islandica",
            scientificName = "Cetraria islandica",
            family = "Parmeliaceae",
            toxicityLevel = "Moderada",
            syndrome = "Gastrointestinal",
            toxicCompounds = "Ácidos liquénicos amargos e irritantes",
            onsetTime = "1-8 h",
            symptoms = "Náuseas, vómitos, diarrea, dolor abdominal y malestar si se consume sin procesado adecuado.",
            description = "Liquen ártico-alpino. Aunque ha tenido usos tradicionales, no se recomienda el consumo sin control experto.",
            habitat = "Suelos fríos de montaña y tundra.",
            geographicDistribution = "Hemisferio norte.",
            confusions = "Cetraria y Flavocetraria.",
            firstAid = "No inducir vómito; hidratar y consultar si síntomas intensos.",
            treatment = "Rehidratación y control sintomático."
        )
    )
}
