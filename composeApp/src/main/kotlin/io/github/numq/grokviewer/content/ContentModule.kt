package io.github.numq.grokviewer.content

import org.apache.tika.Tika
import org.koin.dsl.bind
import org.koin.dsl.module

val contentModule = module {
    single { ContentService.BinaryContentService(tika = Tika()) } bind ContentService::class

    single { SaveContent(service = get()) } bind SaveContent::class
}