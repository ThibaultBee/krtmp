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
package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.common.logger.Logger
import io.github.thibaultbee.krtmp.rtmp.RtmpConfiguration
import io.github.thibaultbee.krtmp.rtmp.chunk.Chunk
import io.github.thibaultbee.krtmp.rtmp.chunk.MessageHeader
import io.github.thibaultbee.krtmp.rtmp.chunk.MessageHeader0
import io.github.thibaultbee.krtmp.rtmp.chunk.MessageHeader1
import io.github.thibaultbee.krtmp.rtmp.chunk.MessageHeader2
import io.github.thibaultbee.krtmp.rtmp.chunk.MessageHeader3
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

sealed class Message(
    val chunkStreamId: Int,
    val messageStreamId: Int,
    val timestamp: Int,
    val messageType: MessageType,
    val payload: RawSource,
) {
    var payloadSize: Int = 0

    init {
        require(timestamp >= 0) { "Timestamp must be positive but $timestamp" }
    }

    private fun buildHeader0(): MessageHeader0 {
        return MessageHeader0(
            timestamp = timestamp,
            messageLength = payloadSize,
            messageType = messageType,
            messageStreamId = messageStreamId
        )
    }

    private fun buildFirstHeader(previousMessage: Message?): MessageHeader {
        return if (previousMessage == null) {
            buildHeader0()
        } else {
            if (previousMessage.timestamp > timestamp) {
                Logger.w(
                    TAG,
                    "Timestamps are not in order. Previous: ${previousMessage.timestamp}, current: $timestamp"
                )
                buildHeader0() // Force header 0 when timestamp are not in order
            } else
                if (previousMessage.messageStreamId == messageStreamId) {
                    if ((previousMessage.messageType == messageType) && (previousMessage.payloadSize == payloadSize)) {
                        MessageHeader2(timestampDelta = timestamp - previousMessage.timestamp)
                    } else {
                        MessageHeader1(
                            timestampDelta = timestamp - previousMessage.timestamp,
                            messageLength = payloadSize,
                            messageType = messageType
                        )
                    }
                } else {
                    buildHeader0()
                }
        }
    }

    private fun buildChunks(chunkSize: Int, previousMessage: Message?): List<Chunk> {
        val firstBuffer = Buffer()
        payloadSize += payload.readAtMostTo(firstBuffer, chunkSize.toLong()).toInt()
        val chunks = mutableListOf<Chunk>()

        do {
            val buffer = Buffer()
            val bytesRead = payload.readAtMostTo(buffer, chunkSize.toLong()).toInt()
            if (bytesRead > 0) {
                chunks.add(Chunk(chunkStreamId, MessageHeader3(), buffer))
                payloadSize += bytesRead
            }
        } while (bytesRead > 0)

        val header = buildFirstHeader(previousMessage)
        chunks.add(0, Chunk(chunkStreamId, header, firstBuffer))

        return chunks
    }

    suspend fun write(
        writeChannel: ByteWriteChannel,
        chunkSize: Int = RtmpConfiguration.DEFAULT_CHUNK_SIZE,
        previousMessage: Message? = null
    ) {
        val chunks = buildChunks(chunkSize, previousMessage)
        chunks.forEach { it.write(writeChannel) }
    }

    companion object {
        private val TAG = "Message"

        /**
         * Reads a message from input stream.
         * For test purpose only.
         */
        suspend fun read(
            channel: ByteReadChannel,
            chunkSize: Int,
            previousMessage: Message?
        ) = read(channel, chunkSize) { _ -> previousMessage }

        /**
         * Reads a message from input stream.
         */
        suspend fun read(
            channel: ByteReadChannel,
            chunkSize: Int,
            onPreviousMessage: (Int) -> Message?
        ): Message {
            val payload = Buffer()
            val firstChunk = Chunk.read(channel, chunkSize, payload)

            val previousMessage = onPreviousMessage(firstChunk.basicHeader.chunkStreamId.toInt())

            val messageLength = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.messageLength
                is MessageHeader1 -> firstChunk.messageHeader.messageLength
                is MessageHeader2 -> previousMessage?.payloadSize
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader3 -> previousMessage?.payloadSize
                    ?: throw IllegalArgumentException("Previous message must not be null")
            }

            val messageType = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.messageType
                is MessageHeader1 -> firstChunk.messageHeader.messageType
                is MessageHeader2 -> previousMessage?.messageType
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader3 -> previousMessage?.messageType
                    ?: throw IllegalArgumentException("Previous message must not be null")
            }

            val messageStreamId = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.messageStreamId
                is MessageHeader1 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader2 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader3 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Previous message must not be null")
            }

            val timestamp = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.timestamp
                is MessageHeader1 -> previousMessage?.timestamp?.plus(firstChunk.messageHeader.timestampDelta)
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader2 -> previousMessage?.timestamp?.plus(firstChunk.messageHeader.timestampDelta)
                    ?: throw IllegalArgumentException("Previous message must not be null")

                is MessageHeader3 -> previousMessage?.timestamp
                    ?: throw IllegalArgumentException("Previous message must not be null")
            }

            while (payload.size < messageLength) {
                Chunk.read(
                    channel,
                    min(chunkSize, messageLength - payload.size.toInt()),
                    payload
                )
            }

            return when (messageType) {
                MessageType.SET_CHUNK_SIZE -> SetChunkSize(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.ABORT -> Abort(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.ACK -> Acknowledgement(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.USER_CONTROL -> UserControl(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.WINDOW_ACK_SIZE -> WindowAcknowledgementSize(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.SET_PEER_BANDWIDTH -> SetPeerBandwidth(
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.COMMAND_AMF0, MessageType.COMMAND_AMF3 -> CommandMessage(
                    chunkStreamId = firstChunk.basicHeader.chunkStreamId.toInt(),
                    messageStreamId = messageStreamId,
                    timestamp = timestamp,
                    messageType = messageType,
                    payload = payload
                )

                else -> {
                    throw IllegalArgumentException("Message type $messageType not supported")
                }
            }
        }
    }
}