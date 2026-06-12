package com.toxicplants.database.ui.screens

/**
 * PlantImageCard — Componente reutilizable de imagen para plantas
 * ================================================================
 * Coloca este composable donde quieras mostrar la foto de una planta.
 *
 * Incorporar en PlantDetailScreen y en PlantCard (lista).
 *
 * Dependencias (ya las tienes en build.gradle.kts):
 *   implementation("io.coil-kt:coil-compose:2.x")
 *   implementation("com.squareup.okhttp3:okhttp:4.x")
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.ui.PlantImageHelper
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

/**
 * Tarjeta de imagen de planta con carga inteligente.
 *
 * @param plant        Entidad de la planta a mostrar.
 * @param height       Altura de la imagen (por defecto 220dp).
 * @param cornerRadius Radio de esquinas (por defecto 12dp).
 * @param showReload   Si mostrar botón de recarga al usuario (por defecto true).
 * @param modifier     Modifier externo.
 */
@Composable
fun PlantImageCard(
    plant: PlantEntity,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    cornerRadius: Dp = 12.dp,
    showReload: Boolean = true,
    reloadKey: Int = 0,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Estado de la imagen
    var imageUrl      by remember(plant) { mutableStateOf("") }
    var isLoading     by remember(plant) { mutableStateOf(true) }
    var hasError      by remember(plant) { mutableStateOf(false) }
    var loadAttempts  by remember(plant) { mutableIntStateOf(0) }
    var showFullScreen by remember(plant) { mutableStateOf(false) }

    // Cargador HTTP con cabeceras de navegador (evita bloqueos de Wikimedia)
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val req = chain.request().newBuilder()
                            .header("User-Agent",
                                "Mozilla/5.0 (Linux; Android 14) " +
                                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                        "Chrome/124.0 Mobile Safari/537.36")
                            .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                            .header("Referer", "https://en.wikipedia.org/")
                            .build()
                        chain.proceed(req)
                    }
                    .followRedirects(true)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    // Función para buscar / recargar imagen
    fun loadImage() {
        isLoading = true
        hasError  = false
        scope.launch {
            val url = PlantImageHelper.resolveImageUrl(context, plant)
            imageUrl  = url
            isLoading = false
            hasError  = url.isBlank()
        }
    }

    // Cargar imagen al entrar o cuando cambie la planta
    LaunchedEffect(plant.id, plant.imageUrl, loadAttempts, reloadKey) {
        loadImage()
    }

    // ─── UI ──────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFF1F8E9)),      // fondo verde muy claro
        contentAlignment = Alignment.Center,
    ) {
        when {
            // ── Cargando ────────────────────────────────────────────────
            isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Buscando imagen…", fontSize = 12.sp, color = Color(0xFF558B2F))
                }
            }

            // ── Sin imagen ──────────────────────────────────────────────
            hasError || imageUrl.isBlank() -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Imagen no disponible", fontSize = 12.sp, color = Color.Gray)
                    if (showReload) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { loadAttempts++ },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E7D32),
                            ),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reintentar", fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Imagen encontrada ────────────────────────────────────────
            else -> {
                val model: Any = if (imageUrl.startsWith("file://")) {
                    File(imageUrl.removePrefix("file://"))
                } else {
                    imageUrl
                }

                AsyncImage(
                    model         = model,
                    imageLoader   = imageLoader,
                    contentDescription = plant.commonName,
                    modifier      = Modifier
                        .fillMaxSize()
                        .clickable { showFullScreen = true },
                    contentScale  = ContentScale.Crop,
                    onState       = { state ->
                        when (state) {
                            is AsyncImagePainter.State.Loading -> { /* ya manejado arriba */ }
                            is AsyncImagePainter.State.Error   -> { hasError = true }
                            is AsyncImagePainter.State.Success -> { hasError = false }
                            else -> {}
                        }
                    }
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "🔍 Tocar para ampliar",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Botón de recarga flotante (esquina superior derecha)
                if (showReload) {
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(),
                        exit    = fadeOut(),
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    ) {
                        SmallFloatingActionButton(
                            onClick            = { loadAttempts++ },
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            contentColor       = Color(0xFF2E7D32),
                            modifier           = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Refresh,
                                contentDescription = "Recargar imagen",
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFullScreen && imageUrl.isNotBlank() && !hasError) {
        FullScreenPlantImage(
            imageUrl = imageUrl,
            title = plant.commonName.ifBlank { plant.scientificName },
            onDismiss = { showFullScreen = false }
        )
    }
}

@Composable
private fun FullScreenPlantImage(
    imageUrl: String,
    title: String,
    onDismiss: () -> Unit
) {
    val model: Any = if (imageUrl.startsWith("file://")) {
        File(imageUrl.removePrefix("file://"))
    } else {
        imageUrl
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = model,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1f) offset + pan else Offset.Zero
                        }
                    },
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pellizca para ampliar · arrastra para mover",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
