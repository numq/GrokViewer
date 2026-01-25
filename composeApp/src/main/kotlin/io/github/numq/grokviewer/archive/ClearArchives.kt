package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase

class ClearArchives(private val repository: ArchiveRepository) : UseCase<Unit, Unit> {
    override suspend fun execute(input: Unit) = repository.clearArchives()
}