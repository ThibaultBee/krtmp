/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.messages.extensions

import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.Chunk
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader0
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader1
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader2
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader3

private fun Message.buildHeader0(): MessageHeader0 {
    return MessageHeader0(
        timestamp = timestamp,
        messageLength = payload.size,
        messageType = messageType,
        messageStreamId = messageStreamId
    )
}

private fun Message.buildFirstHeader(previousMessage: Message?): MessageHeader {
    return if (previousMessage == null) {
        buildHeader0()
    } else {
        if (previousMessage.timestamp > timestamp) {
            KrtmpLogger.w(
                TAG,
                "Timestamps are not in order. Previous: ${previousMessage.timestamp}, current: $timestamp"
            )
            buildHeader0() // Force header 0 when timestamp are not in order
        } else
            if (previousMessage.messageStreamId == messageStreamId) {
                if ((previousMessage.messageType == messageType) && (previousMessage.payload.size == payload.size)) {
                    MessageHeader2(timestampDelta = timestamp - previousMessage.timestamp)
                } else {
                    MessageHeader1(
                        timestampDelta = timestamp - previousMessage.timestamp,
                        messageLength = payload.size,
                        messageType = messageType
                    )
                }
            } else {
                buildHeader0()
            }
    }
}

/**
 * Creates chunks from message payload.
 */
internal fun Message.createChunks(chunkSize: Int, previousMessage: Message?): List<Chunk> {
    val chunks = mutableListOf<Chunk>()

    val header = buildFirstHeader(previousMessage)
    chunks.add(Chunk(chunkStreamId, header, payload.getChunkPayload(chunkSize)))

    while (payload.hasRemaining) {
        chunks.add(Chunk(chunkStreamId, MessageHeader3(), payload.getChunkPayload(chunkSize)))
    }

    return chunks
}

private const val TAG = "MessageExtensions"