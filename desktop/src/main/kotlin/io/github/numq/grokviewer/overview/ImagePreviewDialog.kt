package io.github.numq.grokviewer.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Preview size: decode and dialog max dimensions (pixels for decode, dp for layout)
private const val PREVIEW_MAX_WIDTH = 1400f
private const val PREVIEW_MAX_HEIGHT = 1000f
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

@Composable
fun ImagePreviewDialog(
    content: Content?,
    onDismiss: () -> Unit,
    onSave: (Content) -> Unit,
) {
    if (content == null) return

    val imageResult by produceState<Result<ImageBitmap>?>(
        initialValue = null,
        key1 = content.id
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                ImageProvider.getOrCreate(
                    content = content,
                    dstWidth = PREVIEW_MAX_WIDTH,
                    dstHeight = PREVIEW_MAX_HEIGHT
                )?.bitmap ?: throw NoSuchElementException("Failed to decode image")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onDismiss()
                    true
                } else false
            },
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = PREVIEW_MAX_WIDTH.dp + 48.dp)
                    .heightIn(max = PREVIEW_MAX_HEIGHT.dp + 120.dp)
            ) {
                Text(
                    text = "${content.id}.${content.extension}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                content.lastModified?.let { millis ->
                    Text(
                        text = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_FORMAT),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val bitmap = imageResult?.getOrNull()) {
                        null -> if (imageResult?.isFailure == true) {
                            Text(
                                text = "Unable to preview",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                        else -> Box(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(content)
                            onDismiss()
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
