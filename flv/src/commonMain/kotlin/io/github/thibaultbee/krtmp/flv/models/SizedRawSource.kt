package io.github.thibaultbee.krtmp.flv.models

import io.github.thibaultbee.krtmp.flv.models.sources.ByteArrayRawSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

/**
 * A [SizedRawSource] from a [Buffer].
 *
 * @param buffer the [Buffer] to wrap
 */
fun SizedRawSource(buffer: Buffer) =
    SizedRawSource(buffer, buffer.size)

/**
 * A [SizedRawSource] from a [ByteArray].
 *
 * @param array the [ByteArray] to wrap
 */
fun SizedRawSource(array: ByteArray) =
    SizedRawSource(ByteArrayRawSource(array), array.size.toLong())

fun SizedRawSource(source: RawSource, byteCount: Int) =
    SizedRawSource(source, byteCount.toLong())

/**
 * A [RawSource] with a [byteCount].
 *
 * @param source the [RawSource] to wrap.
 * @param byteCount the number of bytes to read in the [source]
 */
open class SizedRawSource(internal val source: RawSource, internal val byteCount: Long) :
    RawSource {
    private var bytesRead = 0L

    init {
        require(byteCount >= 0) { "byteCount must be positive" }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (bytesRead >= this.byteCount) {
            return -1
        }

        val read = source.readAtMostTo(sink, min(byteCount, this.byteCount - bytesRead))
        bytesRead += read

        return read
    }

    override fun close() {
        source.close()
    }
}