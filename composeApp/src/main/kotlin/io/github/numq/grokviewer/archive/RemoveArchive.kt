package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase

class RemoveArchive(private val repository: ArchiveRepository) : UseCase<RemoveArchive.Input, Unit> {
    data class Input(val path: String)

    override suspend fun execute(input: Input) = repository.removeArchive(path = input.path)
}