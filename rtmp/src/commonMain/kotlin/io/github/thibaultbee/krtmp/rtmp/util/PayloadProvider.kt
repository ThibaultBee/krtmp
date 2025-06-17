package io.github.thibaultbee.krtmp.rtmp.util

import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ByteArrayChunkData
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkData
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.RawSourceChunkData
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered

/**
 * Interface representing a payload that can split into [ChunkData].
 */
interface PayloadProvider {
    /**
     * The total size of the payload in bytes.
     */
    val size: Int

    /**
     * The current position in the payload.
     */
    val hasRemaining: Boolean

    /**
     * Returns a [ChunkData] that writes a portion of the payload to the channel.
     *
     * @param chunkSize The maximum size of the chunk to write.
     * @return A [ChunkData] containing the payload data.
     * @throws NoSuchElementException if there is no more data to read.
     */
    fun getChunkPayload(chunkSize: Int): ChunkData

    /**
     * Returns a [Source] that can be used to read the payload data.
     *
     * This method is useful for reading the entire payload as a stream.
     *
     * @return A [Source] that provides the payload data.
     */
    fun buffered(): Source
}

/**
 * A [PayloadProvider] from a [ByteArray].
 */
class ByteArrayPayloadProvider(
    val data: ByteArray,
    val offset: Int = 0,
    override val size: Int = data.size - offset
) : PayloadProvider {
    private var position = offset

    override val hasRemaining: Boolean
        get() = size > (position - offset)

    /**
     * Returns a [ChunkData] that writes a portion of the byte array to the channel.
     *
     * @param chunkSize The maximum size of the chunk to write.
     * @return A [ChunkData] containing the byte array data.
     */
    override fun getChunkPayload(chunkSize: Int): ChunkData {
        require(chunkSize > 0) { "Chunk size must be positive" }
        val remaining = size - (position - offset)
        return if (remaining <= 0) {
            throw NoSuchElementException("No more data to read")
        } else {
            val size = minOf(chunkSize, remaining)
            val payload = ByteArrayChunkData(data, position, size)
            position += size
            payload
        }
    }

    override fun buffered(): Source {
        return Buffer().apply {
            write(data, offset, (offset + size).toInt())
        }
    }
}


internal fun bufferPayloadProviderOf(
    buffer: Buffer,
) = RawSourcePayloadProvider(buffer, buffer.size.toInt())

class RawSourcePayloadProvider(
    val source: RawSource,
    override val size: Int
) : PayloadProvider {
    private var position = 0

    override val hasRemaining: Boolean
        get() = size > position

    override fun getChunkPayload(chunkSize: Int): ChunkData {
        require(chunkSize > 0) { "Chunk size must be positive" }
        val remaining = size - position
        val size = minOf(chunkSize, remaining)
        val payload = RawSourceChunkData(source, size)
        position += size
        return payload
    }

    override fun buffered() = source.buffered()
}