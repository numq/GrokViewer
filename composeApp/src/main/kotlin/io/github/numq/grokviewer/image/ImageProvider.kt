package io.github.numq.grokviewer.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import java.util.*

object ImageProvider {
    private const val MAX_ENTRIES = 100

    private val cache =
        Collections.synchronizedMap(object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, .75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > MAX_ENTRIES
        })

    fun getCached(id: String) = cache[id]

    fun getOrCreate(id: String, bytes: ByteArray) = runCatching {
        when (val cached = cache[id]) {
            null -> bytes.decodeToImageBitmap().also { imageBitmap ->
                cache[id] = imageBitmap
            }

            else -> cached
        }
    }.getOrNull()
}