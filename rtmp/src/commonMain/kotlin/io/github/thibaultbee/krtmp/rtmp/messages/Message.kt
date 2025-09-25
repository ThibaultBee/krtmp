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

import io.github.thibaultbee.krtmp.flv.sources.LimitedRawSource
import io.github.thibaultbee.krtmp.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.RtmpConstants
import io.github.thibaultbee.krtmp.rtmp.extensions.readFully
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.BasicHeader
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.Chunk
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader0
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader1
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader2
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.MessageHeader3
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
    val payloadSize: Int
) {
    init {
        require(timestamp >= 0) { "Timestamp must be positive but $timestamp" }
    }

    constructor(
        chunkStreamId: Int,
        messageStreamId: Int,
        timestamp: Int,
        messageType: MessageType,
        payload: Buffer
    ) : this(
        chunkStreamId = chunkStreamId,
        messageStreamId = messageStreamId,
        timestamp = timestamp,
        messageType = messageType,
        payload = payload,
        payloadSize = payload.size.toInt()
    )

    override fun toString(): String {
        return "Message(chunkStreamId=$chunkStreamId, messageStreamId=$messageStreamId, timestamp=$timestamp, messageType=$messageType, payloadSize=$payloadSize)"
    }

    internal suspend fun write(
        writeChannel: ByteWriteChannel,
        chunkSize: Int = RtmpConstants.MIN_CHUNK_SIZE,
        previousMessage: Message? = null
    ): Int {
        val chunks = createChunks(chunkSize, previousMessage)
        chunks.forEach { it.write(writeChannel) }
        return payloadSize
    }

    companion object {
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
            getPreviousMessage: suspend (Int) -> Message?
        ): Message {
            val payload = Buffer()

            // Read first chunk
            val basicHeader = BasicHeader.read(channel)

            val chunkStreamId = basicHeader.chunkStreamId.toInt()
            val previousMessage = getPreviousMessage(chunkStreamId)

            val messageHeader = MessageHeader.read(channel, basicHeader.headerType)
            val messageLength = when (messageHeader) {
                is MessageHeader0 -> messageHeader.messageLength
                is MessageHeader1 -> messageHeader.messageLength
                is MessageHeader2 -> previousMessage?.payloadSize
                    ?: throw IllegalArgumentException("Header2: Previous message with $chunkStreamId must not be null")

                is MessageHeader3 -> previousMessage?.payloadSize
                    ?: throw IllegalArgumentException("Header3: Previous message with $chunkStreamId must not be null")
            }
            require(messageLength > 0) { "Message length must be greater than 0 but is $messageLength" }

            channel.readFully(payload, min(messageLength, chunkSize))
            val firstChunk = Chunk(basicHeader, messageHeader, payload)

            val messageType = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.messageType
                is MessageHeader1 -> firstChunk.messageHeader.messageType
                is MessageHeader2 -> previousMessage?.messageType
                    ?: throw IllegalArgumentException("Header2: Previous message with $chunkStreamId must not be null")

                is MessageHeader3 -> previousMessage?.messageType
                    ?: throw IllegalArgumentException("Header3: Previous message with $chunkStreamId must not be null")
            }

            val messageStreamId = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.messageStreamId
                is MessageHeader1 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Header1: Previous message with $chunkStreamId must not be null")

                is MessageHeader2 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Header2: Previous message with $chunkStreamId must not be null")

                is MessageHeader3 -> previousMessage?.messageStreamId
                    ?: throw IllegalArgumentException("Header3: Previous message with $chunkStreamId must not be null")
            }

            val timestamp = when (firstChunk.messageHeader) {
                is MessageHeader0 -> firstChunk.messageHeader.timestamp
                is MessageHeader1 -> previousMessage?.timestamp?.plus(firstChunk.messageHeader.timestampDelta)
                    ?: throw IllegalArgumentException("Header1: Previous message with $chunkStreamId must not be null")

                is MessageHeader2 -> previousMessage?.timestamp?.plus(firstChunk.messageHeader.timestampDelta)
                    ?: throw IllegalArgumentException("Header2: Previous message with $chunkStreamId must not be null")

                is MessageHeader3 -> previousMessage?.timestamp
                    ?: throw IllegalArgumentException("Header3: Previous message with $chunkStreamId must not be null")
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
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.ABORT -> Abort(
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.ACK -> Acknowledgement(
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.USER_CONTROL -> UserControl(
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.WINDOW_ACK_SIZE -> WindowAcknowledgementSize(
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.SET_PEER_BANDWIDTH -> SetPeerBandwidth(
                    chunkStreamId = chunkStreamId,
                    timestamp = timestamp,
                    payload = payload
                )

                MessageType.COMMAND_AMF0, MessageType.COMMAND_AMF3 -> CommandMessage(
                    chunkStreamId = chunkStreamId,
                    messageStreamId = messageStreamId,
                    timestamp = timestamp,
                    messageType = messageType,
                    payload = payload
                )

                MessageType.DATA_AMF0, MessageType.DATA_AMF3 -> DataAmfMessage(
                    chunkStreamId = chunkStreamId,
                    messageStreamId = messageStreamId,
                    timestamp = timestamp,
                    messageType = messageType,
                    payload = payload,
                    payloadSize = payload.size.toInt()
                )

                MessageType.VIDEO -> Video(
                    chunkStreamId = chunkStreamId,
                    messageStreamId = messageStreamId,
                    timestamp = timestamp,
                    payload = payload,
                    payloadSize = payload.size.toInt()
                )

                MessageType.AUDIO -> Audio(
                    chunkStreamId = chunkStreamId,
                    messageStreamId = messageStreamId,
                    timestamp = timestamp,
                    payload = payload,
                    payloadSize = payload.size.toInt()
                )

                else -> {
                    throw IllegalArgumentException("Message type $messageType not supported")
                }
            }
        }
    }
}


private fun Message.buildHeader0(): MessageHeader0 {
    return MessageHeader0(
        timestamp = timestamp,
        messageLength = payloadSize,
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

/**
 * Creates chunks from message payload.
 */
internal fun Message.createChunks(chunkSize: Int, previousMessage: Message?): List<Chunk> {
    val chunks = mutableListOf<Chunk>()

    val header = buildFirstHeader(previousMessage)
    val firstChunkPayloadSize = minOf(chunkSize, payloadSize)
    chunks.add(
        Chunk(
            chunkStreamId,
            header,
            LimitedRawSource(payload, firstChunkPayloadSize.toLong()),
            firstChunkPayloadSize
        )
    )

    var remainingSize = payloadSize - firstChunkPayloadSize
    while (remainingSize > 0) {
        val chunkPayloadSize = minOf(chunkSize, remainingSize)
        chunks.add(
            Chunk(
                chunkStreamId,
                MessageHeader3(),
                LimitedRawSource(payload, chunkPayloadSize.toLong()),
                chunkPayloadSize
            )
        )
        remainingSize -= chunkPayloadSize
    }

    return chunks
}

private const val TAG = "Message"