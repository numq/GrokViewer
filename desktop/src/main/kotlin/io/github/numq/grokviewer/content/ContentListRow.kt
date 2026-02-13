package io.github.numq.grokviewer.content

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.image.CachedImage
import io.github.numq.grokviewer.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LIST_ROW_THUMB_SIZE = 40.dp
private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ContentListRow(
    content: Content,
    isHoverable: Boolean,
    isSelectionModeActive: Boolean,
    isSelected: Boolean,
    click: () -> Unit,
    longClick: () -> Unit
) {
    val progress by animateFloatAsState(targetValue = if (isSelected) 1f else 0f, label = "list_row_progress")

    val hintContent = @Composable {
        when {
            isSelectionModeActive -> Text(
                text = if (isSelected) "Click to Deselect" else "Click to Add to selection",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
            else -> Column(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 4.dp)
            ) {
                Text("Click to Save", style = MaterialTheme.typography.labelMedium)
                Text("Long click to Select", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    val imageBitmap by produceState<CachedImage?>(
        initialValue = null, key1 = content.id, key2 = LIST_ROW_THUMB_SIZE
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                ImageProvider.getOrCreate(
                    content = content,
                    dstWidth = LIST_ROW_THUMB_SIZE.value * 2,
                    dstHeight = LIST_ROW_THUMB_SIZE.value * 2
                )
            }
        }
    }

    val dateText = content.lastModified?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DATE_FORMAT)
    } ?: "—"

    val sizeText = if (content.size >= 0) {
        NumberFormat.getIntegerInstance(Locale.getDefault()).format(content.size) + " B"
    } else "—"

    TooltipArea(
        tooltip = {
            if (isHoverable) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    content = hintContent
                )
            }
        },
        delayMillis = 500
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .heightIn(min = 56.dp)
                .combinedClickable(onLongClick = longClick, onClick = click),
            tonalElevation = if (isSelected) 4.dp else 1.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(LIST_ROW_THUMB_SIZE)
                        .graphicsLayer { alpha = 1f - (progress * .5f) },
                    contentAlignment = Alignment.Center
                ) {
                    when (val bitmap = imageBitmap?.bitmap) {
                        null -> Text(
                            text = content.extension.takeIf(String::isNotBlank)
                                ?: content.mimeType.takeIf(String::isNotBlank)?.take(3) ?: "?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> androidx.compose.foundation.Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${content.id}.${content.extension}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = content.mimeType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (progress > 0f) {
                    Card(
                        modifier = Modifier.graphicsLayer { alpha = progress },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Done,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
