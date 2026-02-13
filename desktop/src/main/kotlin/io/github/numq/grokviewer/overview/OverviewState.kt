package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.ArchiveContentFilter
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.save.SaveCandidate

sealed interface OverviewState {
    val contentFilters: Set<ArchiveContentFilter>

    val overviewArchives: List<OverviewArchive>

    val lastDirectoryPath: String?

    val saveCandidate: SaveCandidate?

    val isHovered: Boolean

    /** Start of date filter range (epoch millis), null = no start bound. */
    val dateRangeStart: Long?

    /** End of date filter range (epoch millis), null = no end bound. */
    val dateRangeEnd: Long?

    val viewMode: ViewMode

    data class Default(
        override val contentFilters: Set<ArchiveContentFilter> = emptySet(),
        override val overviewArchives: List<OverviewArchive> = emptyList(),
        override val lastDirectoryPath: String? = null,
        override val saveCandidate: SaveCandidate? = null,
        override val isHovered: Boolean = false,
        override val dateRangeStart: Long? = null,
        override val dateRangeEnd: Long? = null,
        override val viewMode: ViewMode = ViewMode.GRID,
    ) : OverviewState

    data class Selection(
        override val contentFilters: Set<ArchiveContentFilter> = emptySet(),
        override val overviewArchives: List<OverviewArchive> = emptyList(),
        override val lastDirectoryPath: String? = null,
        override val saveCandidate: SaveCandidate? = null,
        override val isHovered: Boolean = false,
        override val dateRangeStart: Long? = null,
        override val dateRangeEnd: Long? = null,
        override val viewMode: ViewMode = ViewMode.GRID,
        val contents: Set<Content>,
        val contentIds: List<String>
    ) : OverviewState
}