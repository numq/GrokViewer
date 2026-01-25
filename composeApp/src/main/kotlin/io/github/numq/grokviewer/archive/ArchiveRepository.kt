package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.content.ContentService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.concurrent.ConcurrentHashMap

interface ArchiveRepository : AutoCloseable {
    val archives: StateFlow<List<Archive>>

    suspend fun addArchives(paths: List<String>): Result<Unit>

    suspend fun removeArchive(path: String): Result<Unit>

    suspend fun clearArchives(): Result<Unit>

    class InMemoryArchiveRepository(private val service: ContentService) : ArchiveRepository {
        private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        private val activeJobs = ConcurrentHashMap<String, Job>()

        private val _processingQueue = Channel<Archive.Processing>()

        init {
            coroutineScope.launch {
                for (processing in _processingQueue) {
                    _archives.update { archives ->
                        when {
                            archives.none { archive -> archive.path == processing.path } -> archives + Archive.Processing(
                                path = processing.path, name = processing.name
                            )

                            else -> archives
                        }
                    }

                    val job = launch {
                        try {
                            withContext(Dispatchers.IO) {
                                service.parse(path = processing.path).fold(onSuccess = { contents ->
                                    _archives.update { current ->
                                        current.filterNot { archive ->
                                            archive is Archive.Processing && archive.path == processing.path
                                        } + Archive.Processed(
                                            path = processing.path, name = processing.name, contents = contents
                                        )
                                    }
                                }, onFailure = { throwable ->
                                    _archives.update { current ->
                                        current.map { archive ->
                                            when {
                                                archive is Archive.Processing && archive.path == processing.path -> Archive.Failure(
                                                    path = archive.path, name = archive.name, throwable = throwable
                                                )

                                                else -> archive
                                            }
                                        }
                                    }
                                })
                            }
                        } finally {
                            activeJobs.remove(processing.path)
                        }
                    }

                    activeJobs[processing.path] = job
                }
            }
        }

        private val _archives = MutableStateFlow<List<Archive>>(emptyList())

        override val archives = _archives.asStateFlow()

        override suspend fun addArchives(paths: List<String>) = runCatching {
            paths.forEach { path ->
                val file = File(path)

                val name = file.name

                if (file.isFile && name.endsWith("zip")) {
                    _processingQueue.send(Archive.Processing(path = path, name = name))
                }
            }
        }

        override suspend fun removeArchive(path: String) = runCatching {
            activeJobs[path]?.cancel()

            activeJobs.remove(path)

            _archives.update { current ->
                current.filterNot { archive -> archive.path == path }
            }
        }

        override suspend fun clearArchives() = runCatching {
            _archives.update { emptyList() }
        }

        override fun close() {
            coroutineScope.cancel()

            _processingQueue.close()
        }
    }
}