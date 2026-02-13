package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.*
import io.github.numq.grokviewer.content.Content
import io.github.numq.grokviewer.feature.*
import io.github.numq.grokviewer.save.SaveArchive
import io.github.numq.grokviewer.save.SaveCandidate
import io.github.numq.grokviewer.save.SaveContent
import io.github.numq.grokviewer.save.SaveContents
import kotlinx.coroutines.flow.map

class OverviewReducer(
    private val getArchiveContentFilters: GetArchiveContentFilters,
    private val addArchiveContentFilter: AddArchiveContentFilter,
    private val removeArchiveContentFilter: RemoveArchiveContentFilter,
    private val addArchives: AddArchives,
    private val clearArchives: ClearArchives,
    private val getArchives: GetArchives,
    private val removeArchive: RemoveArchive,
    private val saveArchive: SaveArchive,
    private val saveContent: SaveContent,
    private val saveContents: SaveContents,
) : Reducer<OverviewState, OverviewCommand, OverviewEvent> {
    override fun reduce(state: OverviewState, command: OverviewCommand) = when (command) {
        is OverviewCommand.HandleFailure -> transition(state).event(
            OverviewEvent.Error(message = command.throwable.message ?: "Unknown error")
        )

        is OverviewCommand.Initialize -> transition(state).effects(
            action(key = command.key, block = {
            getArchiveContentFilters.execute(input = Unit).fold(
                onSuccess = OverviewCommand::InitializeContentFilters, onFailure = OverviewCommand::HandleFailure
            )
        }), action(key = command.key, block = {
            getArchives.execute(input = Unit)
                .fold(onSuccess = OverviewCommand::InitializeArchives, onFailure = OverviewCommand::HandleFailure)
        })
        )

        is OverviewCommand.InitializeContentFilters -> transition(state).effect(
            stream(key = command.key, flow = command.flow.map(OverviewCommand::HandleContentFilters))
        )

        is OverviewCommand.InitializeArchives -> transition(state).effect(
            stream(key = command.key, flow = command.flow.map(OverviewCommand::HandleArchives))
        )

        is OverviewCommand.HandleContentFilters -> {
            val contentFilters = command.contentFilters

            when (state) {
                is OverviewState.Default -> transition(state.copy(contentFilters = contentFilters))

                is OverviewState.Selection -> transition(state.copy(contentFilters = contentFilters))
            }
        }

        is OverviewCommand.HandleArchives -> {
            val updatedOverviewArchives = command.archives.map { newArchive ->
                val existing = state.overviewArchives.find { overviewArchive ->
                    overviewArchive.archive.path == newArchive.path
                }

                when (existing) {
                    is OverviewArchive.Collapsed -> OverviewArchive.Collapsed(newArchive)

                    else -> OverviewArchive.Expanded(newArchive)
                }
            }

            when (state) {
                is OverviewState.Default -> transition(state.copy(overviewArchives = updatedOverviewArchives))

                is OverviewState.Selection -> transition(state.copy(overviewArchives = updatedOverviewArchives))
            }
        }

        is OverviewCommand.AddContentFilter -> transition(state).effect(action(key = command.key, block = {
            addArchiveContentFilter.execute(
                input = AddArchiveContentFilter.Input(contentFilter = command.contentFilter)
            ).fold(onSuccess = { OverviewCommand.AddContentFilterSuccess }, onFailure = OverviewCommand::HandleFailure)
        }))

        is OverviewCommand.AddContentFilterSuccess -> transition(state)

        is OverviewCommand.RemoveContentFilter -> transition(state).effect(action(key = command.key, block = {
            removeArchiveContentFilter.execute(
                input = RemoveArchiveContentFilter.Input(contentFilter = command.contentFilter)
            ).fold(
                onSuccess = { OverviewCommand.RemoveContentFilterSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.RemoveContentFilterSuccess -> transition(state)

        is OverviewCommand.UploadArchives -> transition(state).effect(action(key = command.key, block = {
            addArchives.execute(input = AddArchives.Input(paths = command.paths)).fold(
                onSuccess = { OverviewCommand.UploadArchivesSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.UploadArchivesSuccess -> transition(state)

        is OverviewCommand.ToggleArchiveExpansion -> {
            val overviewArchiveReplacement = when (command.overviewArchive) {
                is OverviewArchive.Expanded -> OverviewArchive.Collapsed(archive = command.overviewArchive.archive)

                is OverviewArchive.Collapsed -> OverviewArchive.Expanded(archive = command.overviewArchive.archive)
            }

            val updatedState = when (state) {
                is OverviewState.Default -> state.copy(overviewArchives = state.overviewArchives.map { overviewArchive ->
                    when (overviewArchive.archive.path) {
                        overviewArchiveReplacement.archive.path -> overviewArchiveReplacement

                        else -> overviewArchive
                    }
                })

                is OverviewState.Selection -> state.copy(overviewArchives = state.overviewArchives.map { overviewArchive ->
                    when (overviewArchive.archive.path) {
                        overviewArchiveReplacement.archive.path -> overviewArchiveReplacement

                        else -> overviewArchive
                    }
                })
            }

            transition(updatedState)
        }

        is OverviewCommand.RemoveArchive -> transition(state).effect(action(key = command.key, block = {
            removeArchive.execute(input = RemoveArchive.Input(path = command.archive.path)).fold(
                onSuccess = { OverviewCommand.RemoveArchiveSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.RemoveArchiveSuccess -> transition(state)

        is OverviewCommand.ClearArchives -> transition(state).effect(action(key = command.key, block = {
            clearArchives.execute(input = Unit).fold(
                onSuccess = { OverviewCommand.ClearArchivesSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.ClearArchivesSuccess -> transition(state)

        is OverviewCommand.AddToSelection -> when (state) {
            is OverviewState.Default -> with(state) {
                transition(
                    OverviewState.Selection(
                        overviewArchives = overviewArchives.map { overviewArchive ->
                            OverviewArchive.Expanded(archive = overviewArchive.archive)
                        },
                        lastDirectoryPath = lastDirectoryPath,
                        saveCandidate = saveCandidate,
                        isHovered = isHovered,
                        dateRangeStart = dateRangeStart,
                        dateRangeEnd = dateRangeEnd,
                        viewMode = viewMode,
                        contents = command.contents.toSet(),
                        contentIds = command.contents.map(Content::id),
                    )
                )
            }

            is OverviewState.Selection -> {
                val contents = state.contents + command.contents

                transition(state.copy(contents = contents, contentIds = contents.map(Content::id)))
            }
        }

        is OverviewCommand.RemoveFromSelection -> when (state) {
            is OverviewState.Default -> transition(state)

            is OverviewState.Selection -> with(state) {
                val contents = state.contents - command.contents

                when {
                    contents.isEmpty() -> transition(
                        OverviewState.Default(
                            overviewArchives = overviewArchives,
                            lastDirectoryPath = lastDirectoryPath,
                            saveCandidate = saveCandidate,
                            isHovered = isHovered,
                            dateRangeStart = dateRangeStart,
                            dateRangeEnd = dateRangeEnd,
                            viewMode = viewMode,
                        )
                    )

                    else -> transition(state.copy(contents = contents, contentIds = contents.map(Content::id)))
                }
            }
        }

        is OverviewCommand.ClearSelection -> when (state) {
            is OverviewState.Default -> transition(state)

            is OverviewState.Selection -> with(state) {
                transition(
                    OverviewState.Default(
                        overviewArchives = overviewArchives,
                        lastDirectoryPath = lastDirectoryPath,
                        saveCandidate = saveCandidate,
                        isHovered = isHovered,
                        dateRangeStart = dateRangeStart,
                        dateRangeEnd = dateRangeEnd,
                        viewMode = viewMode,
                    )
                )
            }
        }

        is OverviewCommand.SaveArchive -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Archive(archive = command.archive)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Archive(archive = command.archive)))
        }

        is OverviewCommand.SaveContent -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Content(content = command.content)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Content(content = command.content)))
        }

        is OverviewCommand.SaveContents -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = SaveCandidate.Contents(contents = command.contents)))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = SaveCandidate.Contents(contents = command.contents)))
        }

        is OverviewCommand.SaveConfirmation -> {
            val saveEffect = when (val candidate = state.saveCandidate) {
                is SaveCandidate.Archive -> action(key = command.key, block = {
                    saveArchive.execute(
                        input = SaveArchive.Input(
                            archive = candidate.archive, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                is SaveCandidate.Content -> action(key = command.key, block = {
                    saveContent.execute(
                        input = SaveContent.Input(
                            content = candidate.content, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                is SaveCandidate.Contents -> action(key = command.key, block = {
                    saveContents.execute(
                        input = SaveContents.Input(
                            contents = candidate.contents, path = command.path, name = command.name
                        )
                    ).fold(
                        onSuccess = { OverviewCommand.SaveSuccess }, onFailure = OverviewCommand::HandleFailure
                    )
                })

                else -> null
            }

            when (saveEffect) {
                null -> transition(state)

                else -> when (state) {
                    is OverviewState.Default -> transition(state.copy(lastDirectoryPath = command.path)).effect(
                        saveEffect
                    )

                    is OverviewState.Selection -> transition(state.copy(lastDirectoryPath = command.path)).effect(
                        saveEffect
                    )
                }
            }
        }

        is OverviewCommand.SaveCancellation -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = null))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = null))
        }

        is OverviewCommand.SaveSuccess -> when (state) {
            is OverviewState.Default -> transition(state.copy(saveCandidate = null))

            is OverviewState.Selection -> transition(state.copy(saveCandidate = null))
        }

        is OverviewCommand.UpdateHovering -> when (state) {
            is OverviewState.Default -> transition(state.copy(isHovered = command.isHovered))

            is OverviewState.Selection -> transition(state.copy(isHovered = command.isHovered))
        }

        is OverviewCommand.SetDateRange -> when (state) {
            is OverviewState.Default -> transition(state.copy(dateRangeStart = command.start, dateRangeEnd = command.end))
            is OverviewState.Selection -> transition(state.copy(dateRangeStart = command.start, dateRangeEnd = command.end))
        }

        is OverviewCommand.SetViewMode -> when (state) {
            is OverviewState.Default -> transition(state.copy(viewMode = command.viewMode))
            is OverviewState.Selection -> transition(state.copy(viewMode = command.viewMode))
        }
    }
}