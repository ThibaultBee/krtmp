/*
 * Copyright (C) 2023 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.krtmp.rtmp.messages.chunk

import io.github.thibaultbee.krtmp.flv.sources.ReducedRawSource
import io.github.thibaultbee.krtmp.rtmp.extensions.readFully
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeBuffer
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

interface ChunkData {
    /**
     * The size of the data in bytes.
     */
    val size: Int

    /**
     * Write the chunk data to the given channel.
     *
     * @param channel the byte write channel
     */
    suspend fun write(channel: ByteWriteChannel)
}

/**
 * A chunk data that contains a [ByteArray].
 */
internal class ByteArrayChunkData(
    val array: ByteArray,
    val offset: Int = 0,
    override val size: Int = array.size - offset
) : ChunkData {
    override suspend fun write(channel: ByteWriteChannel) {
        channel.writeFully(array, offset, offset + size)
    }
}

/**
 * A chunk data from a [Buffer].
 */
internal fun chunkDataOf(buffer: Buffer) = RawSourceChunkData(buffer, buffer.size.toInt())

/**
 * A chunk data that contains a [RawSource] and its size.
 */
internal fun RawSourceChunkData(
    source: RawSource,
    size: Int
) = RawSourceChunkData(ReducedRawSource(source, size.toLong()))

/**
 * A chunk data that contains a [ReducedRawSource].
 */
internal class RawSourceChunkData(
    val source: ReducedRawSource,
) : ChunkData {
    override val size = source.byteCount.toInt()
    override suspend fun write(channel: ByteWriteChannel) {
        channel.writeBuffer(source)
    }
}

/**
 * Creates a [Chunk] from a [chunkStreamId], [messageHeader], and [data].
 *
 * @param chunkStreamId the chunk stream ID
 * @param messageHeader the message header
 * @param data the chunk data
 * @return a new [Chunk]
 */
internal fun Chunk(
    chunkStreamId: Number,
    messageHeader: MessageHeader,
    data: ChunkData
) = Chunk(
    BasicHeader(messageHeader.type, chunkStreamId),
    messageHeader,
    data
)

/**
 * This class represents a chunk of data in the RTMP protocol.
 *
 * It contains a [BasicHeader], a [MessageHeader], and the data itself.
 */
internal class Chunk(
    val basicHeader: BasicHeader, val messageHeader: MessageHeader, val data: ChunkData
) {
    private val extendedTimestamp = messageHeader.extendedTimestamp

    val size =
        basicHeader.size + messageHeader.size + (extendedTimestamp?.let { 4 } ?: 0) + data.size

    init {
        if (extendedTimestamp != null) {
            require(extendedTimestamp >= MessageHeader.TIMESTAMP_EXTENDED) { "Extended timestamp must be greater than ${MessageHeader.TIMESTAMP_EXTENDED}" }
        }
    }

    suspend fun write(channel: ByteWriteChannel) {
        basicHeader.write(channel)
        messageHeader.write(channel)
        extendedTimestamp?.let {
            channel.writeInt(it)
        }
        data.write(channel)
    }

    companion object {
        /**
         * Read chunk from input stream
         *
         * @param channel the byte read channel
         * @param chunkSize the chunk size
         * @return chunk
         */
        suspend fun read(channel: ByteReadChannel, chunkSize: Int) = read(
            channel, chunkSize, Buffer()
        )

        /**
         * Read chunk from input stream
         *
         * @param channel the byte read channel
         * @param chunkSize the chunk size
         * @param buffer the buffer where data will be written
         * @return chunk
         */
        suspend fun read(
            channel: ByteReadChannel, chunkSize: Int, buffer: Buffer
        ): Chunk {
            val basicHeader = BasicHeader.read(channel)
            val messageHeader = MessageHeader.read(channel, basicHeader.headerType)
            // Extended timestamp is read in MessageHeader.read
            val messageLength = when (messageHeader) {
                is MessageHeader0 -> messageHeader.messageLength
                is MessageHeader1 -> messageHeader.messageLength
                else -> Int.MAX_VALUE // Do nothing, Already pass by chunkSize
            }
            channel.readFully(buffer, min(messageLength, chunkSize))
            return Chunk(basicHeader, messageHeader, chunkDataOf(buffer))
        }
    }
}

