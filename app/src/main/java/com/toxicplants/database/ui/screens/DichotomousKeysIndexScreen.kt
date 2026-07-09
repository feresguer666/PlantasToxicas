package com.toxicplants.database.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.DichotomousKeyEntity
import com.toxicplants.database.ui.viewmodel.DichotomousKeyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DichotomousKeysIndexScreen(
    viewModel: DichotomousKeyViewModel,
    plantViewModel: com.toxicplants.database.ui.viewmodel.PlantViewModel,
    onBack: () -> Unit,
    onKeyClick: (String) -> Unit
) {
    val keys by viewModel.allKeys.collectAsState()
    val allPlants by plantViewModel.plantsData.collectAsState()
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }

    // Precargar plantas en el VM para que el contador inicial sea correcto
    LaunchedEffect(allPlants) {
        if (allPlants.isNotEmpty()) viewModel.setPlants(allPlants)
    }

    // Separar curadas (las que llevan "guiada" en el título) del resto auto
    val curated = remember(keys) {
        keys.filter { it.scope == "family" && it.title.contains("guiada", ignoreCase = true) }
    }
    val curatedFamilies = remember(curated) { curated.mapNotNull { it.family.takeIf { f -> f.isNotBlank() } }.toSet() }

    val general = remember(keys) {
        keys.filter { it.scope == "general" && it.id !in listOf("alphabetical", "by_toxicity", "by_category") }
    }
    val atajosEspeciales = remember(keys) {
        keys.filter { it.id in listOf("alphabetical", "by_toxicity", "by_category") }
    }
    val familiasAuto = remember(keys, curatedFamilies) {
        keys.filter { it.scope == "family" && it.family !in curatedFamilies }
            .sortedBy { it.title }
    }
    val porCategoria = remember(keys) { keys.filter { it.scope == "category" } }

    // Filtro por texto (solo aplica a familias automáticas que son las muchas)
    val filteredFamilias = remember(familiasAuto, query) {
        if (query.isBlank()) familiasAuto
        else familiasAuto.filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true) ||
                    it.family.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claves dicotómicas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.primary,
                    titleContentColor = colors.onPrimary,
                    navigationIconContentColor = colors.onPrimary
                )
            )
        }
    ) { padding ->
        if (keys.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item { HeaderHelp(totalKeys = keys.size) }

            // 1. Clave general
            if (general.isNotEmpty()) {
                item { SectionTitle("🌿 Recomendado") }
                items(general) { key ->
                    KeyCard(key = key, icon = iconFor(key.icon), onClick = { onKeyClick(key.id) },
                        highlighted = true)
                }
            }

            // 2. Atajos especiales (alfabético, toxicidad, categoría)
            if (atajosEspeciales.isNotEmpty()) {
                item { SectionTitle("⚡ Atajos rápidos") }
                items(atajosEspeciales) { key ->
                    KeyCard(key = key, icon = iconFor(key.icon), onClick = { onKeyClick(key.id) })
                }
            }

            // 3. Claves curadas a mano (con preguntas botánicas naturales)
            if (curated.isNotEmpty()) {
                item { SectionTitle("⭐ Claves guiadas (preguntas botánicas detalladas)") }
                items(curated) { key ->
                    KeyCard(
                        key = key,
                        icon = iconFor(key.icon),
                        onClick = { onKeyClick(key.id) },
                        starred = true
                    )
                }
            }

            // 4. Familias automáticas (con buscador)
            if (familiasAuto.isNotEmpty()) {
                item {
                    SectionTitle("🔬 Todas las familias (${familiasAuto.size})")
                }
                item {
                    SearchField(query = query, onChange = { query = it })
                }
                if (filteredFamilias.isEmpty()) {
                    item {
                        Text(
                            "Sin resultados para \"$query\"",
                            color = colors.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(filteredFamilias) { key ->
                        KeyCard(key = key, icon = iconFor(key.icon), onClick = { onKeyClick(key.id) })
                    }
                }
            }

            // 5. Por categoría
            if (porCategoria.isNotEmpty()) {
                item { SectionTitle("📂 Claves por categoría") }
                items(porCategoria) { key ->
                    KeyCard(key = key, icon = iconFor(key.icon), onClick = { onKeyClick(key.id) })
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun HeaderHelp(totalKeys: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.primaryContainer.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "🔑 $totalKeys claves disponibles",
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Cada planta del catálogo es alcanzable desde varias rutas. " +
                        "Empieza por la \"Clave general\" si no sabes por dónde tirar, " +
                        "o usa un atajo rápido si ya tienes una pista (familia, letra, toxicidad…).",
                fontSize = 13.sp,
                color = colors.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar familia (Solanaceae, Apocynaceae...)", fontSize = 13.sp) },
        singleLine = true,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun KeyCard(
    key: DichotomousKeyEntity,
    icon: ImageVector,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    starred: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (highlighted) colors.primaryContainer.copy(alpha = 0.4f) else colors.surface
    val elevation = if (highlighted) 4.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (starred) {
                        Icon(
                            Icons.Filled.Star, contentDescription = null,
                            tint = colors.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        key.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                }
                if (key.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        key.subtitle,
                        fontSize = 12.sp,
                        fontStyle = if (key.scope == "family") FontStyle.Italic else FontStyle.Normal,
                        color = colors.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.primary
            )
        }
    }
}

private fun iconFor(name: String): ImageVector = when (name) {
    "spa" -> Icons.Filled.Spa
    "local_florist" -> Icons.Filled.LocalFlorist
    "account_tree" -> Icons.Filled.AccountTree
    else -> Icons.Filled.FilterAlt
}
