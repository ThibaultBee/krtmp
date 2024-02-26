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
package io.github.thibaultbee.krtmp.rtmp.chunk

import io.github.thibaultbee.krtmp.rtmp.extensions.shl
import io.github.thibaultbee.krtmp.rtmp.extensions.shr
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeShort
import kotlin.experimental.and

/**
 * RTMP basic header
 *
 * Size of the basic header depends on the chunk stream ID.
 *
 * @param headerType the header type
 * @param chunkStreamId the chunk stream ID
 */
internal data class BasicHeader(
    val headerType: MessageHeader.HeaderType,
    val chunkStreamId: Number
) {
    val size = when (chunkStreamId.toInt()) {
        in 2..63 -> 1
        in 64..319 -> 2
        in 320..65599 -> 3
        else -> throw IllegalArgumentException("Chunk stream ID must be between 2 and 65599 but is $chunkStreamId")
    }

    suspend fun write(writeChannel: ByteWriteChannel) {
        when (chunkStreamId.toInt()) {
            in 2..63 -> writeChannel.writeByte(((headerType.value shl 6) or chunkStreamId.toInt()).toByte())
            in 64..319 -> writeChannel.writeShort(((headerType.value shl 14) or (chunkStreamId.toInt() - 64)).toShort())
            in 320..65599 -> {
                writeChannel.writeByte(((headerType.value shl 6) or 1).toByte())
                writeChannel.writeShort((chunkStreamId.toInt() - 64).toShort())
            }

            else -> throw IllegalArgumentException("Chunk stream ID must be between 2 and 65599 but is $chunkStreamId")
        }
    }

    companion object {
        /**
         * Read basic header from input stream
         *
         * @param channel the byte read channel
         * @return the basic header
         */
        suspend fun read(channel: ByteReadChannel): BasicHeader {
            val basicHeader = channel.readByte()
            val headerType =
                MessageHeader.HeaderType.entryOf(((basicHeader shr 6) and 0x3).toByte())
            val chunkStreamId = when (val firstByte = basicHeader and 0x3F) {
                0.toByte() -> channel.readByte() + 64
                1.toByte() -> channel.readShort() + 64
                else -> firstByte
            }

            return BasicHeader(headerType, chunkStreamId)
        }
    }
}