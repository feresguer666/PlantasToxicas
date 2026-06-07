package com.toxicplants.database.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

@Composable
fun MoleculeViewer(moleculeName: String, iupacName: String = "", modifier: Modifier = Modifier) {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(moleculeName, iupacName) {
        isLoading = true
        isError = false
        imageUrl = null

        withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()

                // Intentamos buscar primero por nombre común, y si falla, por nombre IUPAC
                val namesToTry = listOf(moleculeName, iupacName).filter { it.isNotBlank() }
                var foundCid: String? = null

                for (name in namesToTry) {
                    val nameEncoded = java.net.URLEncoder.encode(name, "UTF-8")
                    val request = Request.Builder()
                        .url("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/$nameEncoded/cids/JSON")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val json = JSONObject(body)
                            val cids = json.optJSONArray("IdentifierList")
                            if (cids != null && cids.length() > 0) {
                                foundCid = cids.getString(0)
                            }
                        }
                    }
                    if (foundCid != null) break
                }

                if (foundCid != null) {
                    imageUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/$foundCid/PNG"
                } else {
                    isError = true
                }
                isLoading = false
            } catch (e: Exception) {
                Log.e("MoleculeViewer", "Error fetching molecule: ${e.message}")
                isError = true
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (isError || imageUrl == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🧪", fontSize = 40.sp)
                Text(
                    "Estructura no disponible",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Estructura química de $moleculeName",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}
