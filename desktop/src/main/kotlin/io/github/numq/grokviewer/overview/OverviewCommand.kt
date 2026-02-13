package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.Archive
import io.github.numq.grokviewer.archive.ArchiveContentFilter
import io.github.numq.grokviewer.content.Content
import kotlinx.coroutines.flow.Flow

sealed interface OverviewCommand {
    enum class Key {
        INITIALIZE, INITIALIZE_CONTENT_FILTERS, INITIALIZE_ARCHIVES, ADD_CONTENT_FILTER, REMOVE_CONTENT_FILTER, UPLOAD_ARCHIVES, REMOVE_ARCHIVE, CLEAR_ARCHIVES, SAVE_CONTENT_CONFIRMATION
    }

    data class HandleFailure(val throwable: Throwable) : OverviewCommand

    data object Initialize : OverviewCommand {
        val key = Key.INITIALIZE
    }

    data class InitializeContentFilters(val flow: Flow<Set<ArchiveContentFilter>>) : OverviewCommand {
        val key = Key.INITIALIZE_CONTENT_FILTERS
    }

    data class InitializeArchives(val flow: Flow<List<Archive>>) : OverviewCommand {
        val key = Key.INITIALIZE_ARCHIVES
    }

    data class HandleContentFilters(val contentFilters: Set<ArchiveContentFilter>) : OverviewCommand

    data class HandleArchives(val archives: List<Archive>) : OverviewCommand

    data class AddContentFilter(val contentFilter: ArchiveContentFilter) : OverviewCommand {
        val key = Key.ADD_CONTENT_FILTER
    }

    data object AddContentFilterSuccess : OverviewCommand

    data class RemoveContentFilter(val contentFilter: ArchiveContentFilter) : OverviewCommand {
        val key = Key.REMOVE_CONTENT_FILTER
    }

    data object RemoveContentFilterSuccess : OverviewCommand

    data class UploadArchives(val paths: List<String>) : OverviewCommand {
        val key = Key.UPLOAD_ARCHIVES
    }

    data object UploadArchivesSuccess : OverviewCommand

    data class ToggleArchiveExpansion(val overviewArchive: OverviewArchive) : OverviewCommand

    data class RemoveArchive(val archive: Archive) : OverviewCommand {
        val key = Key.REMOVE_ARCHIVE
    }

    data object RemoveArchiveSuccess : OverviewCommand

    data object ClearArchives : OverviewCommand {
        val key = Key.CLEAR_ARCHIVES
    }

    data object ClearArchivesSuccess : OverviewCommand

    data class AddToSelection(val contents: List<Content>) : OverviewCommand

    data class RemoveFromSelection(val contents: List<Content>) : OverviewCommand

    data object ClearSelection : OverviewCommand

    data class SaveArchive(val archive: Archive.Processed) : OverviewCommand

    data class SaveContent(val content: Content) : OverviewCommand

    data class SaveContents(val contents: List<Content>) : OverviewCommand

    data class SaveConfirmation(val path: String, val name: String) : OverviewCommand {
        val key = Key.SAVE_CONTENT_CONFIRMATION
    }

    data object SaveCancellation : OverviewCommand

    data object SaveSuccess : OverviewCommand

    data class UpdateHovering(val isHovered: Boolean) : OverviewCommand

    data class SetDateRange(val start: Long?, val end: Long?) : OverviewCommand

    data class SetViewMode(val viewMode: ViewMode) : OverviewCommand
}