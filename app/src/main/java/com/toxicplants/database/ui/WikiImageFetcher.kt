package com.toxicplants.database.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object WikiImageFetcher {

    suspend fun getImageUrl(name: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val term = name.trim().replace(" ", "_")

                // 1. Buscar en Wikipedia en y es
                for (lang in listOf("en", "es")) {
                    val url = getFromRestApi(lang, term)
                    if (url.isNotBlank()) return@withContext url
                }

                // 2. Buscar con la API de MediaWiki
                for (lang in listOf("en", "es")) {
                    val url = getFromMediaWikiApi(lang, term)
                    if (url.isNotBlank()) return@withContext url
                }

                // 3. Buscar en Wikimedia Commons
                val url = getFromCommons(term)
                if (url.isNotBlank()) return@withContext url

                // 4. Buscar solo el genero (primera palabra)
                val genus = name.trim().split(" ").firstOrNull() ?: ""
                if (genus.isNotBlank() && genus != term) {
                    for (lang in listOf("en", "es")) {
                        val url2 = getFromRestApi(lang, genus)
                        if (url2.isNotBlank()) return@withContext url2
                    }
                }

                ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun getFromRestApi(lang: String, term: String): String {
        return try {
            val apiUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$term"
            val connection = URL(apiUrl).openConnection()
            connection.setRequestProperty("User-Agent", "PlantasToxicasApp/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = connection.getInputStream().bufferedReader().readText()
            val json = JSONObject(response)

            if (json.has("originalimage")) {
                val source = json.getJSONObject("originalimage").getString("source")
                if (source.isNotBlank()) return source
            }
            if (json.has("thumbnail")) {
                val source = json.getJSONObject("thumbnail").getString("source")
                if (source.isNotBlank()) return source
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFromMediaWikiApi(lang: String, term: String): String {
        return try {
            val encoded = URLEncoder.encode(term.replace("_", " "), "UTF-8")
            val apiUrl = "https://$lang.wikipedia.org/w/api.php?" +
                    "action=query&titles=$encoded&prop=pageimages&format=json&pithumbsize=800"
            val connection = URL(apiUrl).openConnection()
            connection.setRequestProperty("User-Agent", "PlantasToxicasApp/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = connection.getInputStream().bufferedReader().readText()
            val json = JSONObject(response)

            val pages = json.getJSONObject("query").getJSONObject("pages")
            val key = pages.keys().next()
            val page = pages.getJSONObject(key)

            if (page.has("thumbnail")) {
                val source = page.getJSONObject("thumbnail").getString("source")
                if (source.isNotBlank()) return source
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFromCommons(term: String): String {
        return try {
            val encoded = URLEncoder.encode(term.replace("_", " "), "UTF-8")
            val apiUrl = "https://commons.wikimedia.org/w/api.php?" +
                    "action=query&list=search&srsearch=$encoded&srnamespace=6&format=json&srlimit=1"
            val connection = URL(apiUrl).openConnection()
            connection.setRequestProperty("User-Agent", "PlantasToxicasApp/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val response = connection.getInputStream().bufferedReader().readText()
            val json = JSONObject(response)

            val results = json.getJSONObject("query").getJSONArray("search")
            if (results.length() > 0) {
                val title = results.getJSONObject(0).getString("title")
                val fileName = title.removePrefix("File:")
                return "https://commons.wikimedia.org/wiki/Special:FilePath/${URLEncoder.encode(fileName, "UTF-8")}"
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }
}