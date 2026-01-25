package io.github.numq.grokviewer.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.ContentCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OverviewView(feature: OverviewFeature = koinInject()) {
    val scope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is OverviewEvent.Error -> println(event.message)
            }
        }
    }

    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent) = when (val payload = event.dragData()) {
                is DragData.FilesList -> {
                    val paths = payload.readFiles().map { uri ->
                        Path.of(URI(uri)).toAbsolutePath().toString()
                    }

                    scope.launch {
                        feature.execute(OverviewCommand.UploadArchives(paths = paths))
                    }

                    true
                }

                else -> false
            }

            override fun onStarted(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = true))
                }
            }

            override fun onEnded(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = false))
                }
            }

            override fun onExited(event: DragAndDropEvent) {
                scope.launch {
                    feature.execute(OverviewCommand.UpdateHovering(isHovered = false))
                }
            }
        }
    }

    LaunchedEffect(state.contentToSave) {
        val content = state.contentToSave ?: return@LaunchedEffect

        val file = withContext(Dispatchers.IO) {
            val dialog = FileDialog(null as Frame?, "Save Content", FileDialog.SAVE).apply {
                file = "${content.id}.${content.extension}"

                directory = state.lastDirectoryPath

                isVisible = true
            }

            if (dialog.file != null) File(dialog.directory, dialog.file) else null
        }

        when (file) {
            null -> feature.execute(OverviewCommand.SaveContentCancellation)

            else -> feature.execute(OverviewCommand.SaveContentConfirmation(path = file.parent, name = file.name))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            if (state.isHovered) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
        ).dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.dragData() is DragData.FilesList
            }, target = target
        ), contentAlignment = Alignment.Center
    ) {
        when {
            state.archives.isEmpty() -> Text(
                text = "Drag and drop files here", color = MaterialTheme.colorScheme.onBackground
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp), modifier = Modifier.fillMaxSize()
            ) {
                state.archives.forEach { archive ->
                    stickyHeader(key = "header_${archive.path}", contentType = "header") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = Path.of(archive.name).fileName.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = {
                                    scope.launch {
                                        feature.execute(OverviewCommand.RemoveArchive(archive = archive))
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete, contentDescription = null
                                    )
                                }
                            }
                        }
                    }

                    when (archive) {
                        is Archive.Processing -> item(
                            key = "processing_${archive.path}", span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is Archive.Failure -> item(
                            key = "failure_${archive.path}", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Failed to upload: ${archive.throwable.message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        is Archive.Processed -> items(
                            items = archive.contents,
                            key = { content -> "${archive.path}_${content.id}" },
                            contentType = { it::class }) { content ->
                            ContentCard(content = content, click = {
                                scope.launch {
                                    feature.execute(OverviewCommand.SaveContent(content = content))
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}