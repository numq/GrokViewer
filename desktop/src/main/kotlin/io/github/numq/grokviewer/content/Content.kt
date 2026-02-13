package io.github.numq.grokviewer.content

sealed interface Content {
    val id: String

    val path: String

    val mimeType: String

    val extension: String

    val entryName: String

    val zipFilePath: String

    /** Last modified time in epoch millis, or null if not available. */
    val lastModified: Long?

    /** Uncompressed size in bytes, or -1 if unknown. */
    val size: Long

    data class Unknown(
        override val id: String,
        override val path: String,
        override val mimeType: String,
        override val entryName: String,
        override val zipFilePath: String,
        override val lastModified: Long? = null,
        override val size: Long = -1L
    ) : Content {
        override val extension = "bin"
    }

    data class Resolved(
        override val id: String,
        override val path: String,
        override val mimeType: String,
        override val extension: String,
        override val entryName: String,
        override val zipFilePath: String,
        override val lastModified: Long? = null,
        override val size: Long = -1L
    ) : Content
}