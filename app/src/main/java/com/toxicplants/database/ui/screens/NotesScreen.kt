package com.toxicplants.database.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// Modelo
// ─────────────────────────────────────────────
data class PlantNote(
    val id: String      = UUID.randomUUID().toString(),
    val title: String   = "",
    val content: String = "",
    val date: String    = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
)

// ─────────────────────────────────────────────
// Persistencia
// ─────────────────────────────────────────────
private const val PREFS_NAME = "plant_notes_prefs"
private const val KEY_NOTES  = "notes_json"

private fun loadNotes(context: Context): List<PlantNote> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json  = prefs.getString(KEY_NOTES, "[]") ?: "[]"
    val arr   = JSONArray(json)
    return List(arr.length()) { i ->
        arr.getJSONObject(i).let { obj ->
            PlantNote(
                id      = obj.optString("id",      UUID.randomUUID().toString()),
                title   = obj.optString("title",   ""),
                content = obj.optString("content", ""),
                date    = obj.optString("date",    "")
            )
        }
    }
}

private fun saveNotes(context: Context, notes: List<PlantNote>) {
    val arr = JSONArray().apply {
        notes.forEach { note ->
            put(JSONObject().apply {
                put("id",      note.id)
                put("title",   note.title)
                put("content", note.content)
                put("date",    note.date)
            })
        }
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
        putString(KEY_NOTES, arr.toString())
    }
}

// ─────────────────────────────────────────────
// Pantalla
// ─────────────────────────────────────────────
@Suppress("ASSIGNED_BUT_NEVER_READ")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var notes         by remember { mutableStateOf(loadNotes(context)) }
    var showDialog    by remember { mutableStateOf(false) }
    var editingNote   by remember { mutableStateOf<PlantNote?>(null) }
    var deleteTarget  by remember { mutableStateOf<PlantNote?>(null) }

    LaunchedEffect(notes) { saveNotes(context, notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Notes, null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Bloc de Notas", fontWeight = FontWeight.Bold,
                                fontSize = 20.sp, color = Color.White)
                            Text("${notes.size} nota${if (notes.size != 1) "s" else ""}",
                                fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D3311)),
                actions = {
                    IconButton(onClick = { editingNote = null; showDialog = true }) {
                        Icon(Icons.Filled.Add, "Nueva nota", tint = Color.White,
                            modifier = Modifier.size(28.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingNote = null; showDialog = true },
                containerColor = Color(0xFF2E7D32)) {
                Icon(Icons.Filled.Add, "Nueva nota", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(
            Brush.verticalGradient(listOf(Color(0xFF060F07), Color(0xFF0A1A0C), Color(0xFF0D2410))))) {
            if (notes.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("📝", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Sin notas todavía", fontSize = 20.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("Pulsa + para crear tu primera nota",
                        fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(notes) { _, note ->
                        NoteCard(note     = note,
                            onEdit   = { editingNote = note; showDialog = true },
                            onDelete = { deleteTarget = note })
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showDialog) {
        NoteDialog(
            initial   = editingNote,
            onDismiss = { showDialog = false },
            onSave    = { newNote ->
                notes      = if (editingNote == null) listOf(newNote) + notes
                else notes.map { if (it.id == newNote.id) newNote else it }
                showDialog = false
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("¿Eliminar nota?", fontWeight = FontWeight.Bold) },
            text  = { Text("Se eliminará «${target.title.ifBlank { "Sin título" }}» de forma permanente.") },
            confirmButton = {
                TextButton(onClick = {
                    notes        = notes.filter { it.id != target.id }
                    deleteTarget = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

// ─────────────────────────────────────────────
// Tarjeta de nota
// ─────────────────────────────────────────────
@Composable
fun NoteCard(note: PlantNote, onEdit: () -> Unit, onDelete: () -> Unit) {
    val accents = listOf(
        Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF388E3C),
        Color(0xFF33691E), Color(0xFF1A6B1A), Color(0xFF2D6A30)
    )
    val accent = accents[(note.id.hashCode() and 0x7FFFFFFF) % accents.size]

    Card(modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF111F12)),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight()
                .background(accent, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)))
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                Text(note.title.ifBlank { "Sin título" }, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                if (note.content.isNotBlank()) {
                    Text(note.content, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f),
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                }
                Text(note.date, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
            }
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, "Editar", tint = Color(0xFF81C784), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "Eliminar", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Diálogo crear / editar
// ─────────────────────────────────────────────
@Composable
fun NoteDialog(initial: PlantNote?, onDismiss: () -> Unit, onSave: (PlantNote) -> Unit) {
    var title   by remember { mutableStateOf(initial?.title   ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF111F12), tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (initial == null) "📝" else "✏️", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(if (initial == null) "Nueva nota" else "Editar nota",
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Título", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color(0xFF4CAF50)))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("Contenido", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().height(180.dp), maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color(0xFF4CAF50)))
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Cancelar") }
                    Button(
                        onClick  = {
                            val now = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            onSave(PlantNote(id = initial?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(), content = content.trim(), date = now))
                        },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        enabled  = title.isNotBlank() || content.isNotBlank()
                    ) { Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
