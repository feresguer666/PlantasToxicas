package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.PlantEntity
import com.toxicplants.database.CompoundEntity
import coil.compose.AsyncImage
import com.toxicplants.database.ui.LocalImageCache
import com.toxicplants.database.ui.theme.carbonEffectSubtle
import java.io.File

// ==================== PLANT CARD ====================
@Composable
fun PlantCard(
    plant: PlantEntity,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    val toxicityColor = when (plant.toxicityLevel) {
        "Mortal" -> Color(0xFFB71C1C)
        "Muy alto" -> Color(0xFFFF5722)
        "Alto" -> Color(0xFFE65100)
        "Moderado" -> Color(0xFFF57C00)
        "Bajo" -> Color(0xFF388E3C)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .carbonEffectSubtle()
            .clickable {
                if (selectionMode) onSelectionChange(!selected) else onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onSelectionChange(it) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            PlantThumbnail(
                plant = plant,
                toxicityColor = toxicityColor,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(plant.commonName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    plant.scientificName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = toxicityColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            plant.toxicityLevel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = toxicityColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            plant.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
            if (selectionMode) {
                Text(
                    if (selected) "✓" else "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun PlantThumbnail(
    plant: PlantEntity,
    toxicityColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageModel: Any? = remember(plant.id, plant.imageUrl, context) {
        val firstUrl = plant.imageUrl.split("|").map { it.trim() }.firstOrNull { it.isNotBlank() }
        if (firstUrl != null) {
            com.toxicplants.database.ui.PlantImageHelper.getModelForUrl(context, firstUrl)
        } else if (LocalImageCache.hasLocalImage(context, plant.id)) {
            android.net.Uri.fromFile(File(LocalImageCache.getLocalImagePath(context, plant.id)))
        } else null
    }
    var imageFailed by remember(plant.id, imageModel) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(toxicityColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageModel != null && !imageFailed) {
            AsyncImage(
                model = imageModel,
                contentDescription = plant.commonName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageFailed = true }
            )
        } else {
            Text(
                when (plant.toxicityLevel) {
                    "Mortal" -> "☠️"
                    "Muy alto" -> "💀"
                    "Alto" -> "⚠️"
                    "Moderado" -> "⚡"
                    "Bajo" -> "🟢"
                    else -> "ℹ️"
                },
                fontSize = 24.sp,
            )
        }
    }
}

// ==================== COMPOUND ROW ====================
@Suppress("unused")
@Composable
fun CompoundRow(compound: CompoundEntity, onClick: () -> Unit) {
    val color = parseColor(compound.groupColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Science,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    compound.commonName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (compound.iupacName.isNotBlank() && compound.iupacName != compound.commonName) {
                    Text(
                        compound.iupacName,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        color = color.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            compound.groupName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = color,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (compound.molecularFormula.isNotBlank()) {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                compound.molecularFormula,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

fun parseColor(hex: String): Color = try {
    val clean = hex.trim().removePrefix("#")
    Color(("FF$clean".toLong(16)).toInt())
} catch (_: Exception) {
    Color(0xFF7B1FA2)
}