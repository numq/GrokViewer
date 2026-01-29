package io.github.numq.grokviewer.overview

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.archive.ArchiveContentFilter
import io.github.numq.grokviewer.content.ContentCard
import io.github.numq.grokviewer.save.SaveCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Path

private const val HEADER_SIZE = 64f

private const val CELL_SIZE = 128f

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OverviewView(feature: OverviewFeature = koinInject()) {
    val scope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        feature.events.collect { event ->
            when (event) {
                is OverviewEvent.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = event.message, withDismissAction = true, duration = SnackbarDuration.Short
                        )
                    }
                }
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

    LaunchedEffect(state.saveCandidate) {
        val candidate = state.saveCandidate ?: return@LaunchedEffect

        val file = withContext(Dispatchers.IO) {
            val fileName = when (candidate) {
                is SaveCandidate.Archive -> "${candidate.archive.name}-${System.currentTimeMillis()}.zip"

                is SaveCandidate.Content -> "${candidate.content.id}.${candidate.content.extension}"

                is SaveCandidate.Contents -> "${System.currentTimeMillis()}.zip"
            }

            val dialog = FileDialog(null as Frame?, "Save as ZIP", FileDialog.SAVE).apply {
                file = fileName

                directory = state.lastDirectoryPath

                isVisible = true
            }

            if (dialog.file != null) File(dialog.directory, dialog.file) else null
        }

        when (file) {
            null -> feature.execute(OverviewCommand.SaveCancellation)

            else -> feature.execute(OverviewCommand.SaveConfirmation(path = file.parent, name = file.name))
        }
    }

    var isFloatingHovered by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        AnimatedVisibility(visible = state.overviewArchives.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            TopAppBar(title = {
                Text(text = "Files uploaded: ${state.overviewArchives.size}")
            }, actions = {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArchiveContentFilter.entries.forEach { contentFilter ->
                        val isSelected = contentFilter in state.contentFilters

                        ElevatedFilterChip(
                            selected = isSelected, onClick = {
                            scope.launch {
                                val command = if (isSelected) {
                                    OverviewCommand.RemoveContentFilter(contentFilter)
                                } else {
                                    OverviewCommand.AddContentFilter(contentFilter)
                                }

                                feature.execute(command)
                            }
                        }, label = { Text("Filter by ${contentFilter.displayName}") }, leadingIcon = when {
                            isSelected -> {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            }

                            else -> null
                        })
                    }
                }
            })
        }
    }, snackbarHost = {
        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                dismissActionContentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }, floatingActionButton = {
        AnimatedVisibility(
            modifier = Modifier.onPointerEvent(
            eventType = PointerEventType.Enter, onEvent = { isFloatingHovered = true })
            .onPointerEvent(eventType = PointerEventType.Exit, onEvent = { isFloatingHovered = false }),
            visible = (state as? OverviewState.Selection)?.contents?.isNotEmpty() == true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            when (val selectionState = state) {
                is OverviewState.Selection -> Card(shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(
                            space = 4.dp, alignment = Alignment.CenterHorizontally
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgedBox(badge = {
                            if (selectionState.contents.isNotEmpty()) {
                                Badge {
                                    Text("${selectionState.contents.size}")
                                }
                            }
                        }, content = {
                            IconButton(onClick = {
                                scope.launch {
                                    feature.execute(OverviewCommand.SaveContents(contents = selectionState.contents.toList()))
                                }
                            }, enabled = selectionState.contents.isNotEmpty()) {
                                Icon(imageVector = Icons.Default.FolderZip, contentDescription = null)
                            }
                        })
                        IconButton(onClick = {
                            scope.launch {
                                feature.execute(OverviewCommand.ClearSelection)
                            }
                        }, enabled = selectionState.contents.isNotEmpty()) {
                            Icon(Icons.Filled.SelectAll, contentDescription = null)
                        }
                    }
                }

                else -> Unit
            }
        }
    }, floatingActionButtonPosition = FabPosition.Center, content = { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (state.isHovered) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
            ).dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    state is OverviewState.Default && event.dragData() is DragData.FilesList
                }, target = target
            ).padding(paddingValues), contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = state.overviewArchives.isEmpty(), transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                }, label = "overview_content_animation"
            ) { isEmpty ->
                when {
                    isEmpty -> Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Text(text = "Drag and drop ZIP files here", modifier = Modifier.padding(24.dp))
                        }
                    }

                    else -> {
                        val gridState = rememberLazyGridState()

                        val headerIndices = remember(state.overviewArchives) {
                            var currentIndex = 0

                            state.overviewArchives.associate { overviewArchive ->
                                val index = currentIndex

                                currentIndex += 1

                                val archive = overviewArchive.archive

                                if (overviewArchive is OverviewArchive.Expanded) {
                                    currentIndex += when (archive) {
                                        is Archive.Processing, is Archive.Failure -> 1

                                        is Archive.Processed -> archive.contents.size
                                    }
                                }

                                overviewArchive.archive.path to index
                            }
                        }

                        val expandedOverviewArchiveExists by remember {
                            derivedStateOf {
                                state.overviewArchives.any { overviewArchive ->
                                    overviewArchive is OverviewArchive.Expanded
                                }
                            }
                        }

                        var isStickyHeaderHovered by remember { mutableStateOf(false) }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = CELL_SIZE.dp),
                            modifier = Modifier.fillMaxSize(),
                            state = gridState
                        ) {
                            state.overviewArchives.forEach { overviewArchive ->
                                val archive = overviewArchive.archive

                                stickyHeader(
                                    key = "header_${archive.path}_${
                                        when (overviewArchive) {
                                            is OverviewArchive.Expanded -> "expanded"

                                            is OverviewArchive.Collapsed -> "collapsed"
                                        }
                                    }", contentType = "header"
                                ) {
                                    val headerIndex = headerIndices[archive.path] ?: 0

                                    val isHeaderSticky by remember(
                                        gridState, headerIndices, expandedOverviewArchiveExists
                                    ) {
                                        derivedStateOf {
                                            if (!expandedOverviewArchiveExists) return@derivedStateOf false

                                            val headerIndex = headerIndices[archive.path] ?: return@derivedStateOf false

                                            val firstVisibleIndex = gridState.firstVisibleItemIndex

                                            when {
                                                firstVisibleIndex > headerIndex -> true

                                                firstVisibleIndex == headerIndex -> gridState.firstVisibleItemScrollOffset > 0

                                                else -> false
                                            }
                                        }
                                    }

                                    val hasSelectedContent by remember(state, archive) {
                                        derivedStateOf {
                                            state is OverviewState.Selection && archive is Archive.Processed && archive.contents.any { content ->
                                                content.id in (state as OverviewState.Selection).contentIds
                                            }
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth().height(HEADER_SIZE.dp)
                                        .clickable(onClick = {
                                            scope.launch {
                                                feature.execute(
                                                    OverviewCommand.ToggleArchiveExpansion(
                                                        overviewArchive = overviewArchive
                                                    )
                                                )
                                            }
                                        }).onPointerEvent(eventType = PointerEventType.Enter, onEvent = {
                                            isStickyHeaderHovered = true
                                        }).onPointerEvent(
                                            eventType = PointerEventType.Exit, onEvent = {
                                                isStickyHeaderHovered = false
                                            }), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (overviewArchive) {
                                                    is OverviewArchive.Expanded -> Icons.Default.ExpandLess

                                                    is OverviewArchive.Collapsed -> Icons.Default.ExpandMore
                                                }, contentDescription = null
                                            )
                                            Text(
                                                text = "${Path.of(archive.name).fileName}",
                                                color = when (archive) {
                                                    is Archive.Failure -> MaterialTheme.colorScheme.error

                                                    else -> Color.Unspecified
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f).alpha(
                                                    alpha = when (archive) {
                                                        is Archive.Processing -> .5f

                                                        else -> 1f
                                                    }
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AnimatedVisibility(
                                                    visible = isHeaderSticky, enter = fadeIn(), exit = fadeOut()
                                                ) {
                                                    TooltipArea(tooltip = {
                                                        if (isHeaderSticky) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                                tonalElevation = 8.dp
                                                            ) {
                                                                Text(
                                                                    text = "Scroll to top", modifier = Modifier.padding(
                                                                        horizontal = 8.dp, vertical = 4.dp
                                                                    ), style = MaterialTheme.typography.labelMedium
                                                                )
                                                            }
                                                        }
                                                    }, content = {
                                                        IconButton(onClick = {
                                                            scope.launch {
                                                                gridState.animateScrollToItem(headerIndex)
                                                            }
                                                        }, enabled = isHeaderSticky) {
                                                            Icon(
                                                                imageVector = Icons.Default.ArrowUpward,
                                                                contentDescription = null
                                                            )
                                                        }
                                                    })
                                                }
                                                TooltipArea(tooltip = {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        tonalElevation = 8.dp
                                                    ) {
                                                        Text(
                                                            text = if (state is OverviewState.Selection) "Clear selected files" else "Select files",
                                                            modifier = Modifier.padding(
                                                                horizontal = 8.dp, vertical = 4.dp
                                                            ),
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                }, content = {
                                                    IconButton(
                                                        onClick = {
                                                            when {
                                                                hasSelectedContent -> scope.launch {
                                                                    feature.execute(OverviewCommand.ClearSelection)
                                                                }

                                                                archive is Archive.Processed -> scope.launch {
                                                                    feature.execute(
                                                                        OverviewCommand.AddToSelection(
                                                                            contents = archive.contents
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }, enabled = archive is Archive.Processed
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.SelectAll,
                                                            contentDescription = null
                                                        )
                                                    }
                                                })
                                                TooltipArea(tooltip = {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        tonalElevation = 8.dp
                                                    ) {
                                                        Text(
                                                            text = "Save as ZIP", modifier = Modifier.padding(
                                                                horizontal = 8.dp, vertical = 4.dp
                                                            ), style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                }, content = {
                                                    IconButton(
                                                        onClick = {
                                                            if (archive is Archive.Processed) {
                                                                scope.launch {
                                                                    feature.execute(OverviewCommand.SaveArchive(archive = archive))
                                                                }
                                                            }
                                                        },
                                                        enabled = state is OverviewState.Default && archive is Archive.Processed
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.FolderZip,
                                                            contentDescription = null
                                                        )
                                                    }
                                                })
                                                TooltipArea(tooltip = {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        tonalElevation = 8.dp
                                                    ) {
                                                        Text(
                                                            text = "Remove from uploaded", modifier = Modifier.padding(
                                                                horizontal = 8.dp, vertical = 4.dp
                                                            ), style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                }, content = {
                                                    IconButton(
                                                        onClick = {
                                                            scope.launch {
                                                                feature.execute(OverviewCommand.RemoveArchive(archive = archive))
                                                            }
                                                        },
                                                        enabled = state is OverviewState.Default && archive is Archive.Processed
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = null
                                                        )
                                                    }
                                                })
                                            }
                                        }
                                    }
                                }

                                if (overviewArchive is OverviewArchive.Expanded) {
                                    when (archive) {
                                        is Archive.Processing -> item(
                                            key = "processing_${archive.path}", span = { GridItemSpan(maxLineSpan) }) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
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
                                            val isContentSelected by remember(state, content.id) {
                                                derivedStateOf {
                                                    (state as? OverviewState.Selection)?.contentIds?.contains(content.id)
                                                        ?: false
                                                }
                                            }

                                            ContentCard(
                                                content = content,
                                                size = Size(CELL_SIZE, CELL_SIZE),
                                                isHoverable = !isStickyHeaderHovered && !isFloatingHovered,
                                                isSelectionModeActive = state is OverviewState.Selection,
                                                isSelected = isContentSelected,
                                                click = {
                                                    when (state) {
                                                        is OverviewState.Default -> scope.launch {
                                                            feature.execute(OverviewCommand.SaveContent(content = content))
                                                        }

                                                        is OverviewState.Selection -> scope.launch {
                                                            val command = when {
                                                                isContentSelected -> OverviewCommand.RemoveFromSelection(
                                                                    contents = listOf(content)
                                                                )

                                                                else -> OverviewCommand.AddToSelection(
                                                                    contents = listOf(
                                                                        content
                                                                    )
                                                                )
                                                            }

                                                            feature.execute(command)
                                                        }
                                                    }
                                                },
                                                longClick = {
                                                    if (state is OverviewState.Default) {
                                                        scope.launch {
                                                            feature.execute(
                                                                OverviewCommand.AddToSelection(contents = listOf(content))
                                                            )
                                                        }
                                                    }
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    })
}