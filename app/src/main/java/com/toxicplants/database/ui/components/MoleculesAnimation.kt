package com.toxicplants.database.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Cabecera animada con estructuras moleculares flotantes.
 *
 * Genera N "moléculas" (anillos de N átomos unidos con enlaces) que rotan
 * y derivan suavemente por el lienzo. Todo dibujado con Canvas nativo:
 * sin GIF, sin assets, ~5 KB, escala perfecta.
 */
@Composable
fun MoleculesHeader(
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    moleculeCount: Int = 6,
    backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A237E), Color(0xFF512DA8), Color(0xFF7B1FA2))
    ),
    atomColor: Color = Color(0xFF80DEEA),
    bondColor: Color = Color(0xFFE1BEE7),
) {
    val molecules = remember {
        List(moleculeCount) { idx ->
            val rng = Random(idx * 7919L)
            MoleculeSeed(
                atoms = 5 + rng.nextInt(3),                  // 5..7 átomos
                radius = 22f + rng.nextFloat() * 18f,        // 22..40 px
                centerStart = Offset(rng.nextFloat(), rng.nextFloat()),
                speed = 0.10f + rng.nextFloat() * 0.20f,     // velocidad de deriva
                angleOffset = rng.nextFloat() * 2f * PI.toFloat(),
                rotationSign = if (rng.nextBoolean()) 1f else -1f,
                phaseX = rng.nextFloat() * 2f * PI.toFloat(),
                phaseY = rng.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "molecules")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            molecules.forEach { mol ->
                // Posición flotante (deriva senoidal para que parezca natural)
                val cx: Float = ((mol.centerStart.x + 0.15f * sin(mol.phaseX + t * 2f * PI.toFloat())) * w).toFloat()
                val cy: Float = ((mol.centerStart.y + 0.15f * cos(mol.phaseY + t * 2f * PI.toFloat())) * h).toFloat()
                val rotation = mol.rotationSign * t * 2f * PI.toFloat() + mol.angleOffset

                // Vértices del polígono (anillo molecular)
                val verts = List(mol.atoms) { i ->
                    val ang = rotation + i * 2f * PI.toFloat() / mol.atoms
                    Offset(
                        x = cx + mol.radius * cos(ang),
                        y = cy + mol.radius * sin(ang),
                    )
                }

                // Enlaces (polígono cerrado)
                verts.forEachIndexed { i, p ->
                    val q = verts[(i + 1) % verts.size]
                    drawLine(
                        color = bondColor.copy(alpha = 0.55f),
                        start = p,
                        end = q,
                        strokeWidth = 2.5f,
                    )
                }
                // Algunos dobles enlaces para que parezca química real
                for (i in 0 until verts.size step 2) {
                    val a = verts[i]
                    val b = verts[(i + 1) % verts.size]
                    val dx = (b.x - a.x)
                    val dy = (b.y - a.y)
                    val nx = -dy * 0.12f
                    val ny = dx * 0.12f
                    drawLine(
                        color = bondColor.copy(alpha = 0.35f),
                        start = Offset(a.x + nx, a.y + ny),
                        end = Offset(b.x + nx, b.y + ny),
                        strokeWidth = 1.8f,
                    )
                }
                // Átomos
                verts.forEach { p ->
                    drawCircle(color = atomColor.copy(alpha = 0.85f), radius = 5.5f, center = p)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.6f),
                        radius = 5.5f,
                        center = p,
                        style = Stroke(width = 1f),
                    )
                }
            }
        }
    }
}

private data class MoleculeSeed(
    val atoms: Int,
    val radius: Float,
    val centerStart: Offset,
    val speed: Float,
    val angleOffset: Float,
    val rotationSign: Float,
    val phaseX: Float,
    val phaseY: Float,
)
