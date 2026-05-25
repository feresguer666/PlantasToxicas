package com.toxicplants.database

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Carga el catálogo inicial de compuestos desde `assets/compounds.json`. */
object CompoundDataSource {

    private const val ASSET_FILE = "compounds.json"

    fun loadAll(context: Context): List<CompoundEntity> {
        val text = context.assets.open(ASSET_FILE)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<CompoundEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += CompoundEntity(
                id = o.optInt("id", 0),
                commonName = o.optString("commonName", ""),
                iupacName = o.optString("iupacName", ""),
                groupName = o.optString("groupName", ""),
                subgroup = o.optString("subgroup", ""),
                molecularFormula = o.optString("molecularFormula", ""),
                molecularWeight = o.optDoubleOrNull("molecularWeight"),
                sourcePlants = o.optString("sourcePlants", ""),
                concentration = o.optString("concentration", ""),
                mechanism = o.optString("mechanism", ""),
                ld50 = o.optString("ld50", ""),
                toxicDose = o.optString("toxicDose", ""),
                clinicalNeuro = o.optString("clinicalNeuro", ""),
                clinicalCardio = o.optString("clinicalCardio", ""),
                clinicalDigestive = o.optString("clinicalDigestive", ""),
                clinicalRespiratory = o.optString("clinicalRespiratory", ""),
                clinicalDermal = o.optString("clinicalDermal", ""),
                clinicalOther = o.optString("clinicalOther", ""),
                onsetTime = o.optString("onsetTime", ""),
                duration = o.optString("duration", ""),
                treatment = o.optString("treatment", ""),
                notes = o.optString("notes", ""),
                groupColor = o.optString("groupColor", "#7B1FA2"),
                isFavorite = o.optBoolean("isFavorite", false),
            )
        }
        return out
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }
}
