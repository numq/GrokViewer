package io.github.numq.grokviewer.archive

import io.github.numq.grokviewer.content.Content

sealed interface Archive {
    val path: String

    val name: String

    data class Failure(override val path: String, override val name: String, val throwable: Throwable) : Archive

    data class Processing(override val path: String, override val name: String) : Archive

    data class Processed(override val path: String, override val name: String, val contents: List<Content>) : Archive
}