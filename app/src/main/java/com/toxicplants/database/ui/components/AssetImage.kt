package com.toxicplants.database.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Carga una imagen desde `app/src/main/assets/<path>` y la muestra.
 * No depende de Coil ni de URIs file:///android_asset/...
 */
@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    var painter by remember(assetPath) { mutableStateOf<BitmapPainter?>(null) }

    LaunchedEffect(assetPath) {
        painter = loadAssetPainter(context, assetPath)
    }

    val p = painter
    if (p != null) {
        Image(
            painter = p,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

private val cache = ConcurrentHashMap<String, BitmapPainter>()

private suspend fun loadAssetPainter(context: Context, path: String): BitmapPainter? {
    if (path.isBlank()) return null
    cache[path]?.let { return it }
    return withContext(Dispatchers.IO) {
        try {
            context.assets.open(path).use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return@withContext null
                val p = BitmapPainter(bmp.asImageBitmap())
                cache[path] = p
                p
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}