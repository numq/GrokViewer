package io.github.numq.grokviewer.overview

import org.koin.dsl.module
import org.koin.dsl.onClose

val overviewModule = module {
    single {
        OverviewReducer(
            addArchives = get(), clearArchives = get(), getArchives = get(), removeArchive = get(), saveContent = get()
        )
    }

    single { OverviewFeature(reducer = get()) } onClose { it?.close() }
}