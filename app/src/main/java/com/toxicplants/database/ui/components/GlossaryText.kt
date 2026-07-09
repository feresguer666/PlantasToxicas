package com.toxicplants.database.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toxicplants.database.GlossaryDataSource
import com.toxicplants.database.GlossaryTerm
import com.toxicplants.database.data.repository.GlossaryPhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Texto Compose que detecta términos del glosario y los hace clicables.
 * Al pulsar uno, se abre un pop-up con foto(s) real(es) y la definición.
 */
@Composable
fun GlossaryText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    plain: Boolean = false
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    if (plain || text.isBlank()) {
        Text(
            text = text,
            modifier = modifier,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight
        )
        return
    }

    val glossary = remember { GlossaryDataSource.load(context) }
    val matches = remember(text) {
        GlossaryDataSource.findMatches(text, glossary)
    }
    var openedTerm by remember { mutableStateOf<GlossaryTerm?>(null) }

    val annotated = remember(text, matches, colors.primary) {
        buildAnnotated(text, matches, colors.primary)
    }

    if (matches.isEmpty()) {
        Text(
            text = text,
            modifier = modifier,
            fontSize = fontSize,
            color = color,
            fontWeight = fontWeight
        )
    } else {
        androidx.compose.foundation.text.ClickableText(
            text = annotated,
            modifier = modifier,
            style = TextStyle(
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight ?: FontWeight.Normal
            ),
            onClick = { offset ->
                annotated.getStringAnnotations(TAG, offset, offset).firstOrNull()?.let { ann ->
                    glossary.byId[ann.item]?.let { openedTerm = it }
                }
            }
        )
    }

    openedTerm?.let { term ->
        TermPopupDialog(term = term, onDismiss = { openedTerm = null })
    }
}

private const val TAG = "GLOSS"

private fun buildAnnotated(
    text: String,
    matches: List<GlossaryDataSource.Match>,
    highlight: Color
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (m in matches) {
        if (m.start > cursor) append(text.substring(cursor, m.start))
        pushStringAnnotation(tag = TAG, annotation = m.term.id)
        withStyle(
            SpanStyle(
                color = highlight,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(text.substring(m.start, m.end))
        }
        pop()
        cursor = m.end
    }
    if (cursor < text.length) append(text.substring(cursor))
}

@Composable
private fun TermPopupDialog(term: GlossaryTerm, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    rememberCoroutineScope()

    var photos by remember(term.id) {
        mutableStateOf<List<GlossaryPhotoRepository.GlossaryPhoto>>(emptyList())
    }
    var downloading by remember(term.id) { mutableStateOf(false) }

    LaunchedEffect(term.id) {
        photos = withContext(Dispatchers.IO) {
            GlossaryPhotoRepository.listPhotos(context, term.id)
        }
        if (photos.none { it.isSeed } && !term.wikimediaSearch.isNullOrBlank()) {
            downloading = true
            withContext(Dispatchers.IO) {
                GlossaryPhotoRepository.ensureSeedPhotos(
                    context, term.id, term.wikimediaSearch
                )
            }
            photos = withContext(Dispatchers.IO) {
                GlossaryPhotoRepository.listPhotos(context, term.id)
            }
            downloading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📖 ${term.term.replaceFirstChar { it.titlecase() }}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            colors.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val first = photos.firstOrNull()
                    when {
                        downloading && photos.isEmpty() -> CircularProgressIndicator()
                        first == null -> Text("📷", fontSize = 40.sp, color = colors.primary.copy(alpha = 0.5f))
                        else -> {
                            val bmp = remember(first.file.absolutePath, first.file.lastModified()) {
                                runCatching { BitmapFactory.decodeFile(first.file.absolutePath) }.getOrNull()
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = term.term,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(text = term.definition, fontSize = 14.sp, color = colors.onSurface)
                if (term.synonyms.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "También: " + term.synonyms.joinToString(", "),
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
