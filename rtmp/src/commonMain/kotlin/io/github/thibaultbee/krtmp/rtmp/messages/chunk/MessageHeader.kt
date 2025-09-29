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

import io.github.thibaultbee.krtmp.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.extensions.readInt24
import io.github.thibaultbee.krtmp.rtmp.extensions.writeInt24
import io.github.thibaultbee.krtmp.rtmp.extensions.writeIntLittleEndian
import io.github.thibaultbee.krtmp.rtmp.messages.MessageType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByte
import kotlin.math.min

internal sealed class MessageHeader(val type: HeaderType) {
    abstract val size: Int
    abstract val hasExtendedTimestamp: Boolean
    abstract val extendedTimestamp: Int?

    abstract suspend fun write(channel: ByteWriteChannel)

    /**
     * RTMP message header type
     *
     * @param value the header type value
     */
    enum class HeaderType(val value: Byte) {
        TYPE_0(0x00),
        TYPE_1(0x01),
        TYPE_2(0x02),
        TYPE_3(0x03);

        companion object {
            /**
             * Get header type from value
             *
             * @param value the header type value
             * @return the header type
             */
            fun entryOf(value: Byte) = entries.first { it.value == value }
        }
    }

    companion object {
        const val TIMESTAMP_EXTENDED = 0xFFFFFF

        /**
         * Read message header from input stream
         *
         * @param channel the byte read channel
         * @param headerType the header type
         * @return the message header
         */
        suspend fun read(channel: ByteReadChannel, headerType: HeaderType): MessageHeader {
            return when (headerType) {
                HeaderType.TYPE_0 -> MessageHeader0.read(channel)
                HeaderType.TYPE_1 -> MessageHeader1.read(channel)
                HeaderType.TYPE_2 -> MessageHeader2.read(channel)
                HeaderType.TYPE_3 -> MessageHeader3()
            }
        }
    }
}

internal class MessageHeader0(
    val timestamp: Int,
    val messageLength: Int,
    val messageType: MessageType,
    val messageStreamId: Int
) : MessageHeader(HeaderType.TYPE_0) {
    override val size = 11
    override val hasExtendedTimestamp = timestamp >= TIMESTAMP_EXTENDED
    override val extendedTimestamp = if (hasExtendedTimestamp) timestamp else null

    init {
        require(messageLength >= 0) { "Message length must be greater than 0" }
    }

    override suspend fun write(channel: ByteWriteChannel) {
        channel.writeInt24(min(timestamp, TIMESTAMP_EXTENDED))
        channel.writeInt24(messageLength)
        channel.writeByte(messageType.value)
        channel.writeIntLittleEndian(messageStreamId)
    }

    override fun toString(): String {
        return "MessageHeader0(timestamp=$timestamp, messageLength=$messageLength, messageType=$messageType, messageStreamId=$messageStreamId, hasExtendedTimestamp=$hasExtendedTimestamp, extendedTimestamp=$extendedTimestamp)"
    }

    companion object {
        /**
         * Read message header from input stream
         *
         * @param channel the byte read channel
         * @return the message header
         */
        suspend fun read(channel: ByteReadChannel): MessageHeader0 {
            val timestamp = channel.readInt24()
            val messageLength = channel.readInt24()
            val messageType = MessageType.entryOf(channel.readByte())
            val messageStreamId = channel.readInt()

            return if (timestamp == TIMESTAMP_EXTENDED) {
                val extendedTimestamp = channel.readInt()
                MessageHeader0(extendedTimestamp, messageLength, messageType, messageStreamId)
            } else {
                MessageHeader0(timestamp, messageLength, messageType, messageStreamId)
            }
        }

    }
}

internal class MessageHeader1(
    val timestampDelta: Int,
    val messageLength: Int,
    val messageType: MessageType
) : MessageHeader(HeaderType.TYPE_1) {
    override val size = 7
    override val hasExtendedTimestamp = timestampDelta >= TIMESTAMP_EXTENDED
    override val extendedTimestamp = if (hasExtendedTimestamp) timestampDelta else null

    override suspend fun write(channel: ByteWriteChannel) {
        channel.writeInt24(min(timestampDelta, TIMESTAMP_EXTENDED))
        channel.writeInt24(messageLength)
        channel.writeByte(messageType.value)
    }

    override fun toString(): String {
        return "MessageHeader1(timestampDelta=$timestampDelta, messageLength=$messageLength, messageType=$messageType, hasExtendedTimestamp=$hasExtendedTimestamp, extendedTimestamp=$extendedTimestamp)"
    }

    companion object {
        private const val TAG = "MessageHeader1"

        /**
         * Read message header from input stream
         *
         * @param channel the byte read channel
         * @return the message header
         */
        suspend fun read(channel: ByteReadChannel): MessageHeader1 {
            val timestampDelta = channel.readInt24()
            val messageLength = channel.readInt24()
            try {
                val messageType = MessageType.entryOf(channel.readByte())
                return if (timestampDelta == TIMESTAMP_EXTENDED) {
                    val extendedTimestamp = channel.readInt()
                    MessageHeader1(extendedTimestamp, messageLength, messageType)
                } else {
                    MessageHeader1(timestampDelta, messageLength, messageType)
                }
            } catch (t: Throwable) {
                KrtmpLogger.e(
                    TAG,
                    "Error while reading message header type 1: timestamp delta = $timestampDelta, messageLength = $messageLength: ${t.message}"
                )
                throw t
            }
        }
    }
}

internal class MessageHeader2(
    val timestampDelta: Int
) : MessageHeader(HeaderType.TYPE_2) {
    override val size = 3
    override val hasExtendedTimestamp = timestampDelta >= TIMESTAMP_EXTENDED
    override val extendedTimestamp = if (hasExtendedTimestamp) timestampDelta else null

    override suspend fun write(channel: ByteWriteChannel) {
        channel.writeInt24(min(timestampDelta, TIMESTAMP_EXTENDED))
    }

    override fun toString(): String {
        return "MessageHeader2(timestampDelta=$timestampDelta, hasExtendedTimestamp=$hasExtendedTimestamp, extendedTimestamp=$extendedTimestamp)"
    }

    companion object {
        /**
         * Read message header from input stream
         *
         * @param channel the byte read channel
         * @return the message header
         */
        suspend fun read(channel: ByteReadChannel): MessageHeader2 {
            val timestampDelta = channel.readInt24()
            return if (timestampDelta == TIMESTAMP_EXTENDED) {
                val extendedTimestamp = channel.readInt()
                MessageHeader2(extendedTimestamp)
            } else {
                MessageHeader2(timestampDelta)
            }
        }
    }
}

internal class MessageHeader3 : MessageHeader(HeaderType.TYPE_3) {
    override val size = 0
    override val hasExtendedTimestamp = false
    override val extendedTimestamp = null

    override suspend fun write(channel: ByteWriteChannel) {
        // Do nothing
    }

    override fun toString(): String {
        return "MessageHeader3()"
    }
}