package io.github.numq.grokviewer.content

sealed interface Content {
    val id: String

    val path: String

    val extension: String

    val rawBytes: ByteArray

    sealed interface Visual : Content {
        data class Image(
            override val id: String,
            override val path: String,
            override val rawBytes: ByteArray,
            override val extension: String
        ) : Visual {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Image

                if (id != other.id) return false
                if (path != other.path) return false
                if (!rawBytes.contentEquals(other.rawBytes)) return false
                if (extension != other.extension) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id.hashCode()
                result = 31 * result + path.hashCode()
                result = 31 * result + rawBytes.contentHashCode()
                result = 31 * result + extension.hashCode()
                return result
            }
        }

        data class VideoPreview(
            override val id: String,
            override val path: String,
            override val rawBytes: ByteArray,
            override val extension: String = "webm"
        ) : Visual {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as VideoPreview

                if (id != other.id) return false
                if (path != other.path) return false
                if (!rawBytes.contentEquals(other.rawBytes)) return false
                if (extension != other.extension) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id.hashCode()
                result = 31 * result + path.hashCode()
                result = 31 * result + rawBytes.contentHashCode()
                result = 31 * result + extension.hashCode()
                return result
            }
        }
    }

    data class JsonMetadata(override val id: String, override val path: String, val fileName: String) : Content {
        override val extension: String = "json"

        override val rawBytes = byteArrayOf()
    }

    data class Unknown(override val id: String, override val path: String, override val rawBytes: ByteArray) : Content {
        override val extension: String = "bin"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Unknown

            if (id != other.id) return false
            if (path != other.path) return false
            if (!rawBytes.contentEquals(other.rawBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + path.hashCode()
            result = 31 * result + rawBytes.contentHashCode()
            return result
        }
    }
}