@file:OptIn(ExperimentalComposeUiApi::class)

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material.icons.outlined.ViewModule
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
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.content.ContentCard
import io.github.numq.grokviewer.content.ContentListRow
import io.github.numq.grokviewer.save.SaveCandidate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val HEADER_SIZE = 64f

private const val CELL_SIZE = 128f

private val DATE_FORMAT_INPUT = DateTimeFormatter.ISO_LOCAL_DATE

private fun filterContentsByDate(
    contents: List<Content>,
    dateRangeStart: Long?,
    dateRangeEnd: Long?
): List<Content> {
    if (dateRangeStart == null && dateRangeEnd == null) return contents
    return contents.filter { content ->
        val t = content.lastModified ?: return@filter false
        (dateRangeStart == null || t >= dateRangeStart) && (dateRangeEnd == null || t <= dateRangeEnd)
    }
}

private fun LazyGridScope.ProcessedArchiveItems(
    archive: Archive.Processed,
    state: OverviewState,
    scope: CoroutineScope,
    feature: OverviewFeature,
    isListView: Boolean,
    isStickyHeaderHovered: Boolean,
    isFloatingHovered: Boolean,
    filteredContents: List<Content>,
    onPreviewImageRequest: (Content) -> Unit,
) {
    items(
        items = filteredContents,
        key = { content -> "${archive.path}_${content.id}" },
        contentType = { it::class },
        span = if (isListView) { { GridItemSpan(maxLineSpan) } } else { { GridItemSpan(1) } }
    ) { content ->
        val isContentSelected by remember(state, content.id) {
            derivedStateOf {
                (state as? OverviewState.Selection)?.contentIds?.contains(content.id)
                    ?: false
            }
        }
        val clickBlock: () -> Unit = {
            when (state) {
                is OverviewState.Default -> {
                    if (content.mimeType.startsWith("image")) {
                        onPreviewImageRequest(content)
                    } else {
                        scope.launch {
                            feature.execute(OverviewCommand.SaveContent(content = content))
                        }
                    }
                }
                is OverviewState.Selection -> scope.launch {
                    val command = when {
                        isContentSelected -> OverviewCommand.RemoveFromSelection(
                            contents = listOf(content)
                        )
                        else -> OverviewCommand.AddToSelection(
                            contents = listOf(content)
                        )
                    }
                    feature.execute(command)
                }
            }
        }
        val longClickBlock: () -> Unit = {
            if (state is OverviewState.Default) {
                scope.launch {
                    feature.execute(
                        OverviewCommand.AddToSelection(contents = listOf(content))
                    )
                }
            }
        }
        if (isListView) {
            ContentListRow(
                content = content,
                isHoverable = !isStickyHeaderHovered && !isFloatingHovered,
                isSelectionModeActive = state is OverviewState.Selection,
                isSelected = isContentSelected,
                click = clickBlock,
                longClick = longClickBlock
            )
        } else {
            ContentCard(
                content = content,
                size = Size(CELL_SIZE, CELL_SIZE),
                isHoverable = !isStickyHeaderHovered && !isFloatingHovered,
                isSelectionModeActive = state is OverviewState.Selection,
                isSelected = isContentSelected,
                click = clickBlock,
                longClick = longClickBlock
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OverviewView(feature: OverviewFeature = koinInject()) {
    val scope = rememberCoroutineScope()

    val state by feature.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var previewContent by remember { mutableStateOf<Content?>(null) }

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
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.dateRangeStart?.let { millis ->
                                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT_INPUT)
                            } ?: "",
                            onValueChange = { text ->
                                scope.launch {
                                    val start = text.ifBlank { null }?.let { s ->
                                        try {
                                            LocalDate.parse(s, DATE_FORMAT_INPUT)
                                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        } catch (_: DateTimeParseException) { null }
                                    }
                                    feature.execute(OverviewCommand.SetDateRange(start = start, end = state.dateRangeEnd))
                                }
                            },
                            placeholder = { Text("From (yyyy-MM-dd)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.widthIn(min = 120.dp, max = 140.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default
                        )
                        OutlinedTextField(
                            value = state.dateRangeEnd?.let { millis ->
                                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FORMAT_INPUT)
                            } ?: "",
                            onValueChange = { text ->
                                scope.launch {
                                    val end = text.ifBlank { null }?.let { s ->
                                        try {
                                            LocalDate.parse(s, DATE_FORMAT_INPUT)
                                                .atTime(23, 59, 59, 999_999_999)
                                                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        } catch (_: DateTimeParseException) { null }
                                    }
                                    feature.execute(OverviewCommand.SetDateRange(start = state.dateRangeStart, end = end))
                                }
                            },
                            placeholder = { Text("To (yyyy-MM-dd)", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.widthIn(min = 120.dp, max = 140.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default
                        )
                        if (state.dateRangeStart != null || state.dateRangeEnd != null) {
                            IconButton(onClick = {
                                scope.launch {
                                    feature.execute(OverviewCommand.SetDateRange(start = null, end = null))
                                }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear date filter")
                            }
                        }
                        TooltipArea(tooltip = {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 8.dp
                            ) {
                                Text(
                                    text = "Grid view",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        feature.execute(OverviewCommand.SetViewMode(ViewMode.GRID))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ViewModule,
                                    contentDescription = "Grid view",
                                    modifier = Modifier.then(
                                        if (state.viewMode == ViewMode.GRID) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                                    )
                                )
                            }
                        }
                        TooltipArea(tooltip = {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 8.dp
                            ) {
                                Text(
                                    text = "List view with details",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        feature.execute(OverviewCommand.SetViewMode(ViewMode.LIST))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ViewList,
                                    contentDescription = "List view",
                                    modifier = Modifier.then(
                                        if (state.viewMode == ViewMode.LIST) Modifier.alpha(1f) else Modifier.alpha(0.5f)
                                    )
                                )
                            }
                        }
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

                    else -> OverviewGridContent(
                        state = state,
                        scope = scope,
                        feature = feature,
                        isFloatingHovered = isFloatingHovered,
                        onPreviewImageRequest = { previewContent = it },
                    )
                }
            }
        }
    })

    ImagePreviewDialog(
        content = previewContent,
        onDismiss = { previewContent = null },
        onSave = { c ->
            previewContent = null
            scope.launch {
                feature.execute(OverviewCommand.SaveContent(content = c))
            }
        }
    )
}

@Composable
private fun OverviewGridContent(
    state: OverviewState,
    scope: CoroutineScope,
    feature: OverviewFeature,
    isFloatingHovered: Boolean,
    onPreviewImageRequest: (Content) -> Unit,
) {
    val gridState = rememberLazyGridState()

    val headerIndices = remember(
        state.overviewArchives,
        state.dateRangeStart,
        state.dateRangeEnd
    ) {
        var currentIndex = 0

        state.overviewArchives.associate { overviewArchive ->
            val index = currentIndex

            currentIndex += 1

            val archive = overviewArchive.archive

            if (overviewArchive is OverviewArchive.Expanded) {
                currentIndex += when (archive) {
                    is Archive.Processing, is Archive.Failure -> 1
                    is Archive.Processed ->
                        filterContentsByDate(
                            archive.contents,
                            state.dateRangeStart,
                            state.dateRangeEnd
                        ).size
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

    val isListView = state.viewMode == ViewMode.LIST

    val filteredContentsByArchive = remember(
        state.overviewArchives,
        state.dateRangeStart,
        state.dateRangeEnd
    ) {
        state.overviewArchives.associate { overviewArchive ->
            val archive = overviewArchive.archive
            overviewArchive.archive.path to when (archive) {
                is Archive.Processed -> filterContentsByDate(
                    archive.contents,
                    state.dateRangeStart,
                    state.dateRangeEnd
                )
                else -> emptyList()
            }
        }
    }

    LazyVerticalGrid(
        columns = if (isListView) GridCells.Fixed(1) else GridCells.Adaptive(minSize = CELL_SIZE.dp),
        modifier = Modifier.fillMaxSize(),
        state = gridState
    ) {
        EmitOverviewArchives(
            overviewArchives = state.overviewArchives,
            state = state,
            scope = scope,
            feature = feature,
            gridState = gridState,
            headerIndices = headerIndices,
            expandedOverviewArchiveExists = expandedOverviewArchiveExists,
            isStickyHeaderHovered = isStickyHeaderHovered,
            isListView = isListView,
            isFloatingHovered = isFloatingHovered,
            onStickyHeaderHoveredChange = { isStickyHeaderHovered = it },
            filteredContentsByArchive = filteredContentsByArchive,
            onPreviewImageRequest = onPreviewImageRequest,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.EmitOverviewArchives(
    overviewArchives: List<OverviewArchive>,
    state: OverviewState,
    scope: CoroutineScope,
    feature: OverviewFeature,
    gridState: LazyGridState,
    headerIndices: Map<String, Int>,
    expandedOverviewArchiveExists: Boolean,
    isStickyHeaderHovered: Boolean,
    isListView: Boolean,
    isFloatingHovered: Boolean,
    onStickyHeaderHoveredChange: (Boolean) -> Unit,
    filteredContentsByArchive: Map<String, List<Content>>,
    onPreviewImageRequest: (Content) -> Unit,
) {
    for (overviewArchive in overviewArchives) {
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
                                            onStickyHeaderHoveredChange(true)
                                        }).onPointerEvent(
                                            eventType = PointerEventType.Exit, onEvent = {
                                                onStickyHeaderHoveredChange(false)
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

                is Archive.Processed -> ProcessedArchiveItems(
                    archive = archive,
                    state = state,
                    scope = scope,
                    feature = feature,
                    isListView = isListView,
                    isStickyHeaderHovered = isStickyHeaderHovered,
                    isFloatingHovered = isFloatingHovered,
                    filteredContents = filteredContentsByArchive[archive.path] ?: emptyList(),
                    onPreviewImageRequest = onPreviewImageRequest,
                )
            }
        }
    }
}