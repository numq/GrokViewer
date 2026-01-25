package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase

class AddArchives(private val repository: ArchiveRepository) : UseCase<AddArchives.Input, Unit> {
    data class Input(val paths: List<String>)

    override suspend fun execute(input: Input) = repository.addArchives(paths = input.paths)
}