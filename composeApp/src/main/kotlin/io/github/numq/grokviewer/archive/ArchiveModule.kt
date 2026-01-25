package io.github.numq.grokviewer.archive

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

val archiveModule = module {
    single { ArchiveRepository.InMemoryArchiveRepository(service = get()) } bind ArchiveRepository::class onClose { it?.close() }

    single { AddArchives(get()) }

    single { ClearArchives(get()) }

    single { GetArchives(get()) }

    single { RemoveArchive(get()) }
}