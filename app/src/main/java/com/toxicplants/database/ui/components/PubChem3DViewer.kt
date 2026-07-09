package com.toxicplants.database.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Visor 3D de moléculas de PubChem integrado en un WebView.
 *
 * Utiliza la página de PubChem Compound con vista 3D interactiva.
 * El usuario puede rotar, hacer zoom y explorar la molécula en 3D.
 *
 * @param pubchemCid PubChem Compound ID (CID). Si es 0, no se muestra nada.
 * @param compoundName Nombre del compuesto para el título.
 * @param accentColor Color de acento para la UI.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PubChem3DViewer(
    pubchemCid: Int,
    compoundName: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (pubchemCid == 0) return

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Título de la sección
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, tint = accentColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Estructura 3D",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accentColor,
                    modifier = Modifier.weight(1f),
                )
                // Botón para expandir/contraer
                TextButton(onClick = { expanded = !expanded }) {
                    Text(
                        if (expanded) "Ocultar" else "Ver molécula",
                        fontSize = 12.sp,
                        color = accentColor,
                    )
                }
            }

            // Info CID
            Text(
                "PubChem CID: $pubchemCid",
                fontSize = 11.sp,
                color = Color.Gray,
            )

            // Visor WebView
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5)),
                    ) {
                        // WebView con el visor 3D
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowContentAccess = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.setSupportZoom(true)

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?,
                                        ) {
                                            isLoading = true
                                            hasError = false
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            isLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            errorCode: Int,
                                            description: String?,
                                            failingUrl: String?,
                                        ) {
                                            hasError = true
                                            isLoading = false
                                        }
                                    }

                                    webChromeClient = WebChromeClient()

                                    // Carga el visor 3D embebido de PubChem
                                    // Usamos la página 3D conformer viewer de PubChem
                                    val html = build3DViewerHtml(pubchemCid, compoundName)
                                    loadDataWithBaseURL(
                                        "https://pubchem.ncbi.nlm.nih.gov",
                                        html,
                                        "text/html",
                                        "UTF-8",
                                        null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Indicador de carga
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = accentColor)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Cargando molécula 3D…",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                    )
                                }
                            }
                        }

                        // Error
                        if (hasError) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.9f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⚠️", fontSize = 32.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "No se pudo cargar la molécula",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                    )
                                    Text(
                                        "Comprueba tu conexión a internet",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Botón para abrir en PubChem
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://pubchem.ncbi.nlm.nih.gov/compound/$pubchemCid#section=3D-Conformer"),
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Filled.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir en PubChem", fontSize = 13.sp)
                    }

                    // Instrucciones
                    Text(
                        "🔄 Arrastra para rotar · 🔍 Pellizca para zoom",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Genera el HTML con el visor 3D usando 3Dmol.js,
 * que carga la estructura SDF desde PubChem PUG-REST.
 */
private fun build3DViewerHtml(cid: Int, name: String): String {
    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                background: #f5f5f5;
                font-family: -apple-system, sans-serif;
                overflow: hidden;
            }
            #viewer {
                width: 100vw;
                height: 85vh;
                position: relative;
                background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
                border-radius: 8px;
            }
            #info {
                text-align: center;
                padding: 6px;
                color: #666;
                font-size: 11px;
                background: #f5f5f5;
            }
            #loading {
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                color: white;
                text-align: center;
                font-size: 14px;
                z-index: 10;
            }
            .spinner {
                border: 3px solid rgba(255,255,255,0.2);
                border-top: 3px solid white;
                border-radius: 50%;
                width: 36px;
                height: 36px;
                animation: spin 1s linear infinite;
                margin: 0 auto 10px;
            }
            @keyframes spin {
                to { transform: rotate(360deg); }
            }
            #error {
                display: none;
                position: absolute;
                top: 50%;
                left: 50%;
                transform: translate(-50%, -50%);
                color: white;
                text-align: center;
                font-size: 14px;
                z-index: 10;
            }
        </style>
        <script src="https://3Dmol.org/build/3Dmol-min.js"></script>
    </head>
    <body>
        <div id="viewer">
            <div id="loading">
                <div class="spinner"></div>
                Cargando $name…
            </div>
            <div id="error">
                ⚠️<br>No se pudo cargar<br>
                <span style="font-size:11px;opacity:0.7">Comprueba tu conexión</span>
            </div>
        </div>
        <div id="info">$name · PubChem CID $cid</div>

        <script>
            (function() {
                var viewer = ${'$'}3Dmol.createViewer("viewer", {
                    backgroundColor: "0x1a1a2e"
                });

                var sdfUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/$cid/record/SDF/?record_type=3d&response_type=display";

                fetch(sdfUrl)
                    .then(function(r) {
                        if (!r.ok) throw new Error("HTTP " + r.status);
                        return r.text();
                    })
                    .then(function(data) {
                        document.getElementById("loading").style.display = "none";
                        viewer.addModel(data, "sdf");
                        viewer.setStyle({}, {stick: {radius: 0.15, colorscheme: "Jmol"}, sphere: {scale: 0.25, colorscheme: "Jmol"}});
                        viewer.zoomTo();
                        viewer.spin("y", 0.5);
                        viewer.render();
                    })
                    .catch(function(err) {
                        // Fallback: try 2D SDF
                        var sdfUrl2d = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/$cid/record/SDF/?response_type=display";
                        fetch(sdfUrl2d)
                            .then(function(r) {
                                if (!r.ok) throw new Error("HTTP " + r.status);
                                return r.text();
                            })
                            .then(function(data) {
                                document.getElementById("loading").style.display = "none";
                                viewer.addModel(data, "sdf");
                                viewer.setStyle({}, {stick: {radius: 0.15, colorscheme: "Jmol"}, sphere: {scale: 0.25, colorscheme: "Jmol"}});
                                viewer.zoomTo();
                                viewer.spin("y", 0.5);
                                viewer.render();
                            })
                            .catch(function() {
                                document.getElementById("loading").style.display = "none";
                                document.getElementById("error").style.display = "block";
                            });
                    });
            })();
        </script>
    </body>
    </html>
    """.trimIndent()
}
