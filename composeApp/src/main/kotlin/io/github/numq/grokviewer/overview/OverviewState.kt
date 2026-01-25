package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.content.Content

data class OverviewState(
    val archives: List<Archive> = emptyList(),
    val lastDirectoryPath: String? = null,
    val contentToSave: Content? = null,
    val isHovered: Boolean = false,
)