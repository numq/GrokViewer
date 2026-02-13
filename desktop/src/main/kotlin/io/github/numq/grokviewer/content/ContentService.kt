package io.github.numq.grokviewer.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.tika.Tika
import java.io.File
import java.util.zip.ZipFile

interface ContentService {
    suspend fun parse(path: String): Result<List<Content>>

    class BinaryContentService(json: Json, private val tika: Tika) : ContentService {
        private val preferredExtensions = setOf("jpg", "jpeg", "png", "txt", "mp3", "mp4", "json")

        @OptIn(ExperimentalSerializationApi::class)
        private val mimeTypes = javaClass.getResourceAsStream("/json/mime.json")?.use { inputStream ->
            json.decodeFromStream<Map<String, String>>(stream = inputStream).entries.groupBy({ it.value }, { it.key })
                .mapValues { (_, extensions) ->
                    val preferred = extensions.find(preferredExtensions::contains)

                    if (preferred != null) return@mapValues preferred

                    extensions.minByOrNull { it.length } ?: "bin"
                }
        } ?: emptyMap()

        override suspend fun parse(path: String) = withContext(Dispatchers.IO) {
            runCatching {
                check(mimeTypes.isNotEmpty()) { "Unable to load MIME types" }

                buildList {
                    ZipFile(File(path)).use { zipFile ->
                        val entries = zipFile.entries()

                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (!entry.isDirectory) {
                                val previewBytes = zipFile.getInputStream(entry).use { inputStream ->
                                    inputStream.readNBytes(1024)
                                }

                                val nameParts = entry.name.split("/")

                                val id = when {
                                    entry.name.endsWith("/content") -> nameParts.dropLast(1).lastOrNull() ?: entry.name

                                    else -> nameParts.last().substringBeforeLast(".")
                                }

                                val uniquePath = "$path!/${entry.name}"

                                val mimeType = tika.detect(previewBytes)

                                val extension = entry.name.substringAfterLast(".", "").takeIf(String::isNotBlank)
                                    ?: mimeTypes[mimeType.lowercase()]

                                val lastModified = entry.lastModifiedTime.toMillis().takeIf { it > 0 }
                                val size = entry.size

                                val content = when (extension) {
                                    null -> Content.Unknown(
                                        id = id,
                                        path = uniquePath,
                                        mimeType = mimeType,
                                        entryName = entry.name,
                                        zipFilePath = path,
                                        lastModified = lastModified,
                                        size = size
                                    )

                                    else -> Content.Resolved(
                                        id = id,
                                        path = uniquePath,
                                        mimeType = mimeType,
                                        extension = extension,
                                        entryName = entry.name,
                                        zipFilePath = path,
                                        lastModified = lastModified,
                                        size = size
                                    )
                                }

                                add(content)
                            }
                        }
                    }
                }.sortedWith(
                    // Sort by date (newest first). lastModified is from the ZIP entry metadata
                    // (Grok export); it may not reflect original file creation date.
                    compareBy<Content> { it.lastModified == null }
                        .thenByDescending { it.lastModified ?: 0L }
                        .thenBy(Content::id)
                )
            }
        }
    }
}