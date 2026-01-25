package io.github.numq.grokviewer.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

interface ContentService {
    suspend fun parse(path: String): Result<List<Content>>

    suspend fun save(content: Content, path: String, name: String): Result<Unit>

    class BinaryContentService(private val tika: Tika) : ContentService {
        private fun resolveContent(id: String, path: String, bytes: ByteArray): Content {
            val mimeType = tika.detect(bytes)

            return when {
                mimeType.startsWith("image/") -> Content.Visual.Image(
                    id = id, path = path, rawBytes = bytes, extension = mimeType.substringAfter("/")
                )

                mimeType == "video/webm" || mimeType == "video/x-matroska" -> Content.Visual.VideoPreview(
                    id = id, path = path, rawBytes = bytes
                )

                mimeType == "application/json" -> Content.JsonMetadata(
                    id = id, path = path, fileName = "metadata.json"
                )

                else -> Content.Unknown(id = id, path = path, rawBytes = bytes)
            }
        }

        override suspend fun parse(path: String) = runCatching {
            buildList {
                ZipInputStream(File(path).inputStream().buffered()).use { zis ->
                    var entry = zis.getNextEntry()

                    while (entry != null) {
                        if (!coroutineContext.isActive) throw kotlinx.coroutines.CancellationException()

                        if (entry.name.endsWith("/content")) {
                            val bytes = zis.readAllBytes()

                            val fileId = entry.name.split("/").dropLast(1).last()

                            val uniquePath = "$path!/${entry.name}"

                            add(resolveContent(id = fileId, path = uniquePath, bytes = bytes))
                        }

                        entry = zis.getNextEntry()
                    }
                }
            }
        }

        override suspend fun save(content: Content, path: String, name: String) = runCatching {
            withContext(Dispatchers.IO) {
                val destination = File(path, name)

                destination.parentFile?.mkdirs()

                destination.writeBytes(content.rawBytes)
            }
        }
    }
}