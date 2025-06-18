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

import io.github.thibaultbee.krtmp.rtmp.extensions.readFully
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeBuffer
import io.ktor.utils.io.writeInt
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

/**
 * Creates a chunk with a [Buffer] payload.
 */
internal fun Chunk(
    basicHeader: BasicHeader,
    messageHeader: MessageHeader,
    data: Buffer
) = Chunk(
    basicHeader,
    messageHeader,
    data,
    data.size.toInt()
)

/**
 * Creates a chunk with a [RawSource] payload.
 */
internal fun Chunk(
    chunkStreamId: Number,
    messageHeader: MessageHeader,
    data: RawSource,
    dataSize: Int
) = Chunk(
    BasicHeader(messageHeader.type, chunkStreamId),
    messageHeader,
    data,
    dataSize
)

/**
 * RTMP chunk
 */
internal class Chunk(
    val basicHeader: BasicHeader,
    val messageHeader: MessageHeader,
    val data: RawSource,
    val dataSize: Int
) {
    private val extendedTimestamp = messageHeader.extendedTimestamp

    val size =
        basicHeader.size + messageHeader.size + (extendedTimestamp?.let { 4 }
            ?: 0) + dataSize

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
        channel.writeBuffer(data)
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
            channel,
            chunkSize,
            Buffer()
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
            channel: ByteReadChannel,
            chunkSize: Int,
            buffer: Buffer
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
            return Chunk(basicHeader, messageHeader, buffer)
        }
    }
}

