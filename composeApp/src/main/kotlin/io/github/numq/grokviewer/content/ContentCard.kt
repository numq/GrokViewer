package io.github.numq.grokviewer.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContentCard(content: Content, click: () -> Unit) {
    val cached = remember(content.id) { ImageProvider.getCached(content.id) }

    val imageBitmap by produceState(initialValue = cached, key1 = content.id) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                ImageProvider.getOrCreate(content.id, content.rawBytes)
            }
        }
    }

    Card(modifier = Modifier.padding(4.dp).aspectRatio(1f)) {
        Box(modifier = Modifier.fillMaxSize().clickable(onClick = click), contentAlignment = Alignment.Center) {
            when (content) {
                is Content.Visual -> {
                    val bitmap = imageBitmap

                    when {
                        bitmap != null -> Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        cached == null -> CircularProgressIndicator(modifier = Modifier.size(24.dp))

                        else -> Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is Content.JsonMetadata -> Text(
                    text = "JSON",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                is Content.Unknown -> Text(
                    text = "?",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}