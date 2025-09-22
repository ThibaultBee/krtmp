package io.github.thibaultbee.krtmp.common.sources

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

/**
 * A [kotlinx.io.RawSource] that reads from a [ByteArray].
 */
class ByteArrayBackedRawSource(private val array: ByteArray, startIndex: Long = 0) :
    RawSource {
    private var position = startIndex
    private val size = array.size.toLong()

    val isExhausted: Boolean
        get() = position >= (size - 1)

    init {
        require(startIndex >= 0) { "Start index must be a positive value" }
        require(size >= 0) { "size must be a positive value" }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount < 0) {
            throw IllegalArgumentException("byteCount < 0: $byteCount")
        }

        if (isExhausted) {
            return -1
        }

        val bytesToRead = min(size - position, position + byteCount)
        sink.write(array, position.toInt(), (position + bytesToRead).toInt())
        position += bytesToRead.toInt()
        return bytesToRead
    }

    override fun close() {
        // Nothing to do
    }
}