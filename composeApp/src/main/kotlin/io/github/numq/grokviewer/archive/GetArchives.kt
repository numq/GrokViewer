package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.usecase.UseCase
import kotlinx.coroutines.flow.StateFlow

class GetArchives(private val repository: ArchiveRepository) : UseCase<Unit, StateFlow<List<Archive>>> {
    override suspend fun execute(input: Unit) = runCatching { repository.archives }
}