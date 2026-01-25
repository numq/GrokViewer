package io.github.numq.grokviewer.overview

import io.github.numq.grokviewer.archive.AddArchives
import io.github.numq.grokviewer.archive.ClearArchives
import io.github.numq.grokviewer.archive.GetArchives
import io.github.numq.grokviewer.archive.RemoveArchive
import io.github.numq.grokviewer.content.SaveContent
import io.github.numq.grokviewer.feature.*
import kotlinx.coroutines.flow.map

class OverviewReducer(
    private val addArchives: AddArchives,
    private val clearArchives: ClearArchives,
    private val getArchives: GetArchives,
    private val removeArchive: RemoveArchive,
    private val saveContent: SaveContent,
) : Reducer<OverviewState, OverviewCommand, OverviewEvent> {
    override fun reduce(state: OverviewState, command: OverviewCommand) = when (command) {
        is OverviewCommand.HandleFailure -> transition(state).event(
            OverviewEvent.Error(message = command.throwable.message ?: "Unknown error")
        )

        is OverviewCommand.Initialize -> transition(state).effect(
            action(key = command.key, block = {
                getArchives.execute(input = Unit).fold(
                    onSuccess = OverviewCommand::InitializeSuccess, onFailure = OverviewCommand::HandleFailure
                )
            })
        )

        is OverviewCommand.InitializeSuccess -> transition(state).effect(
            stream(key = command.key, flow = command.flow.map(OverviewCommand::HandleArchives))
        )

        is OverviewCommand.HandleArchives -> transition(state.copy(archives = command.archives))

        is OverviewCommand.UploadArchives -> transition(state).effect(action(key = command.key, block = {
            addArchives.execute(input = AddArchives.Input(paths = command.paths)).fold(
                onSuccess = { OverviewCommand.UploadArchivesSuccess }, onFailure = OverviewCommand::HandleFailure
            )
        }))

        is OverviewCommand.UploadArchivesSuccess -> transition(state)

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

        is OverviewCommand.SaveContent -> transition(state.copy(contentToSave = command.content))

        is OverviewCommand.SaveContentConfirmation -> when (val content = state.contentToSave) {
            null -> transition(state)

            else -> transition(
                state.copy(lastDirectoryPath = command.path)
            ).effect(action(key = command.key, block = {
                saveContent.execute(
                    input = SaveContent.Input(
                        content = content,
                        path = command.path,
                        name = command.name
                    )
                ).fold(
                    onSuccess = { OverviewCommand.SaveContentSuccess }, onFailure = OverviewCommand::HandleFailure
                )
            }))
        }

        is OverviewCommand.SaveContentCancellation -> transition(state.copy(contentToSave = null))

        is OverviewCommand.SaveContentSuccess -> transition(state.copy(contentToSave = null))

        is OverviewCommand.UpdateHovering -> transition(state.copy(isHovered = command.isHovered))
    }
}