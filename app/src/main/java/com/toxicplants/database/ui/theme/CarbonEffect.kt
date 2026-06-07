package com.toxicplants.database.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Modifier de fibra de carbono de luxe.
 * Añade líneas diagonales + brillo/sombra a cualquier composable.
 *
 * Usar: Modifier.carbonEffect()
 */
fun Modifier.carbonEffect(
    lineLight: Color = Color.White.copy(alpha = 0.12f),
    lineDark: Color = Color.Black.copy(alpha = 0.40f),
    lineWidth: Float = 2.5f,
    shimTop: Float = 0.22f,
    shimBottom: Float = 0.40f
): Modifier = this
    .drawBehind {
        val s = 12.dp.toPx()
        var x = 0f
        while (x < size.width + size.height) {
            drawLine(lineLight, Offset(x, 0f), Offset(x - size.height, size.height), lineWidth)
            drawLine(lineDark, Offset(x + s / 2, 0f), Offset(x + s / 2 - size.height, size.height), lineWidth)
            x += s
        }
    }
    .drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = shimTop),
                    Color.Transparent,
                    Color.Black.copy(alpha = shimBottom)
                )
            )
        )
    }

/**
 * Versión suave para cards y superficies pequeñas.
 */
fun Modifier.carbonEffectSubtle(): Modifier = carbonEffect(
    lineLight = Color.White.copy(alpha = 0.06f),
    lineDark = Color.Black.copy(alpha = 0.18f),
    lineWidth = 1.5f,
    shimTop = 0.10f,
    shimBottom = 0.18f
)
