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

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader
import io.github.thibaultbee.krtmp.amf.elements.Amf3ElementReader
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfElementFactory
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CLOSE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CONNECT_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CREATE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_DELETE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCUNPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_ONFCPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PLAY_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_RELEASE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject
import io.github.thibaultbee.krtmp.rtmp.messages.command.NetConnectionResultInformation
import io.github.thibaultbee.krtmp.rtmp.messages.command.NetConnectionResultObject
import io.github.thibaultbee.krtmp.rtmp.messages.command.ObjectEncoding
import io.github.thibaultbee.krtmp.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.krtmp.rtmp.util.AmfUtil.amf
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCode
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.serialization.Serializable

internal class CommandMessage(
    chunkStreamId: Int,
    messageStreamId: Int,
    timestamp: Int,
    messageType: MessageType,
    payload: Buffer
) : Message(
    chunkStreamId = chunkStreamId,
    messageStreamId = messageStreamId,
    timestamp = timestamp,
    messageType = messageType,
    payload = payload
)

open class Command(
    val chunkStreamId: Int,
    val messageStreamId: Int,
    val timestamp: Int,
    val name: String,
    val transactionId: Long,
    val commandObject: AmfElement?,
    vararg val arguments: Any?
) : AmfMessage {
    override fun createMessage(amfVersion: AmfVersion): Message {
        val payload =
            AmfElementFactory.buildContainer(
                listOf(
                    name,
                    transactionId.toDouble(),
                    commandObject
                ) + arguments
            )
        val messageType = when (amfVersion) {
            AmfVersion.AMF0 -> MessageType.COMMAND_AMF0
            AmfVersion.AMF3 -> MessageType.COMMAND_AMF3
        }
        return CommandMessage(
            chunkStreamId = chunkStreamId,
            messageStreamId = messageStreamId,
            timestamp = timestamp,
            messageType = messageType,
            payload = payload.write(amfVersion)
        )
    }

    override suspend fun write(
        writeChannel: ByteWriteChannel,
        amfVersion: AmfVersion,
        chunkSize: Int,
        previousMessage: Message?
    ): Int {
        return createMessage(amfVersion).write(writeChannel, chunkSize, previousMessage)
    }

    override fun toString(): String {
        return "Command(timestamp=$timestamp, messageStreamId=$messageStreamId, name=$name, transactionId=$transactionId, commandObject=$commandObject, arguments=${arguments.joinToString()})"
    }

    companion object {
        internal const val COMMAND_RESULT_NAME = "_result"
        internal const val COMMAND_ERROR_NAME = "_error"

        internal const val COMMAND_ON_STATUS_NAME = "onStatus"

        internal const val COMMAND_CONNECT_NAME = "connect"

        internal const val COMMAND_CREATE_STREAM_NAME = "createStream"
        internal const val COMMAND_RELEASE_STREAM_NAME = "releaseStream"
        internal const val COMMAND_CLOSE_STREAM_NAME = "closeStream"
        internal const val COMMAND_DELETE_STREAM_NAME = "deleteStream"

        internal const val COMMAND_PLAY_NAME = "play"

        internal const val COMMAND_FCPUBLISH_NAME = "FCPublish"
        internal const val COMMAND_ONFCPUBLISH_NAME = "onFCPublish"
        internal const val COMMAND_FCUNPUBLISH_NAME = "FCUnpublish"
        internal const val COMMAND_PUBLISH_NAME = "publish"

        internal fun read(
            commandMessage: CommandMessage
        ): Command {
            val amfElementReader = when (commandMessage.messageType) {
                MessageType.COMMAND_AMF0 -> Amf0ElementReader
                MessageType.COMMAND_AMF3 -> Amf3ElementReader
                else -> throw IllegalArgumentException("Unknown message type: ${commandMessage.messageType}")
            }
            val payload = commandMessage.payload.buffered()

            @Suppress("UNCHECKED_CAST")
            val name = (amfElementReader.read(payload) as AmfPrimitive<String>).value
            try {
                @Suppress("UNCHECKED_CAST")
                val transactionId =
                    if (!payload.exhausted()) (amfElementReader.read(payload) as AmfPrimitive<Number>).value.toLong() else 0
                val commandObject =
                    if (!payload.exhausted()) amfElementReader.read(payload) else null
                val arguments = mutableListOf<AmfElement>()
                while (!payload.exhausted()) {
                    arguments.add(amfElementReader.read(payload))
                }
                return when (name) {
                    COMMAND_RESULT_NAME -> {
                        Result(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            arguments.firstOrNull()
                        )
                    }

                    COMMAND_ERROR_NAME -> {
                        Error(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            arguments.firstOrNull()
                        )
                    }

                    COMMAND_ON_STATUS_NAME -> {
                        OnStatus(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            arguments.firstOrNull()
                                ?: throw IllegalArgumentException("onStatus command must have exactly one argument")
                        )
                    }

                    else -> {
                        Command(
                            commandMessage.chunkStreamId,
                            commandMessage.messageStreamId,
                            commandMessage.timestamp,
                            name,
                            transactionId,
                            commandObject,
                            *arguments.toTypedArray()
                        )
                    }
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("Cannot read command message $name", e)
            }
        }
    }

    class Result(
        messageStreamId: Int,
        transactionId: Long,
        timestamp: Int,
        resultObject: AmfElement? = null,
        informationObject: AmfElement? = null,
        chunkStreamId: Int = ChunkStreamId.PROTOCOL_CONTROL.value
    ) :
        Command(
            chunkStreamId,
            messageStreamId,
            timestamp,
            COMMAND_RESULT_NAME,
            transactionId,
            resultObject,
            informationObject
        )

    class Error(
        messageStreamId: Int,
        transactionId: Long,
        timestamp: Int,
        errorObject: AmfElement? = null,
        informationObject: AmfElement? = null,
        chunkStreamId: Int = ChunkStreamId.PROTOCOL_CONTROL.value
    ) :
        Command(
            chunkStreamId,
            messageStreamId,
            timestamp,
            COMMAND_ERROR_NAME,
            transactionId,
            errorObject,
            informationObject
        )

    class OnStatus(
        messageStreamId: Int,
        transactionId: Long,
        timestamp: Int,
        information: NetStreamOnStatusInformation,
        chunkStreamId: Int = ChunkStreamId.PROTOCOL_CONTROL.value
    ) :
        Command(
            chunkStreamId,
            messageStreamId,
            timestamp,
            COMMAND_ON_STATUS_NAME,
            transactionId,
            null,
            amf.encodeToAmfElement(
                NetStreamOnStatusInformation.serializer(),
                information
            )
        ) {
        @Serializable
        class NetStreamOnStatusInformation(
            override val level: NetStreamOnStatusLevel,
            override val code: NetStreamOnStatusCode,
            override val description: String,
        ) : ResultInformation

        companion object {
            fun from(
                messageStreamId: Int,
                transactionId: Long,
                timestamp: Int,
                information: AmfElement
            ) = OnStatus(
                messageStreamId,
                transactionId,
                timestamp,
                amf.decodeFromAmfElement(NetStreamOnStatusInformation.serializer(), information)
            )
        }
    }
}

interface ResultInformation {
    val level: String
    val code: String
    val description: String
}

fun OnStatus(
    messageStreamId: Int,
    transactionId: Long,
    timestamp: Int,
    information: AmfElement
) = Command.OnStatus.from(messageStreamId, transactionId, timestamp, information)

fun CommandNetConnectionResult(
    timestamp: Int,
    connectReplyObject: NetConnectionResultObject = NetConnectionResultObject.default,
    objectEncoding: ObjectEncoding = ObjectEncoding.AMF0
): Command.Result {
    return Command.Result(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        1,
        timestamp,
        amf.encodeToAmfElement(
            NetConnectionResultObject.serializer(),
            connectReplyObject
        ),
        amf.encodeToAmfElement(
            NetConnectionResultInformation.serializer(),
            NetConnectionResultInformation(
                level = "status",
                code = "NetConnection.Connect.Success",
                description = "Connection succeeded.",
                objectEncoding = objectEncoding
            )
        ),
    )
}

fun CommandConnect(
    transactionId: Long,
    timestamp: Int,
    connectObject: ConnectObject
): Command {
    require(transactionId == 1L) {
        "Transaction ID must be 1 for connect command, got $transactionId"
    }
    return Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_CONNECT_NAME,
        transactionId,
        amf.encodeToAmfElement(
            ConnectObject.serializer(),
            connectObject
        )
    )
}

fun CommandCreateStream(transactionId: Long, timestamp: Int) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_CREATE_STREAM_NAME,
        transactionId,
        null
    )


fun CommandReleaseStream(
    transactionId: Long,
    timestamp: Int,
    streamKey: String
) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_RELEASE_STREAM_NAME,
        transactionId,
        null,
        streamKey
    )

fun CommandCloseStream(transactionId: Long, timestamp: Int) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_CLOSE_STREAM_NAME,
        transactionId,
        null
    )

fun CommandDeleteStream(transactionId: Long, timestamp: Int, streamId: Int) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_DELETE_STREAM_NAME,
        transactionId,
        null,
        streamId.toDouble()
    )

fun CommandPlay(timestamp: Int, streamName: String) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_PLAY_NAME,
        0,
        null,
        streamName
    )

fun CommandFCPublish(
    transactionId: Long,
    timestamp: Int,
    streamKey: String
) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_FCPUBLISH_NAME,
        transactionId,
        null,
        streamKey
    )

fun CommandOnFCPublish(
    transactionId: Long,
    timestamp: Int
) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_ONFCPUBLISH_NAME,
        transactionId,
        null,
        null
    )

fun CommandFCUnpublish(
    transactionId: Long,
    timestamp: Int,
    streamKey: String
) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_FCUNPUBLISH_NAME,
        transactionId,
        null,
        streamKey
    )


fun CommandPublish(
    messageStreamId: Int,
    transactionId: Long,
    timestamp: Int,
    streamKey: String,
    streamType: StreamPublishType
) = Command(
    ChunkStreamId.COMMAND_CHANNEL.value,
    messageStreamId,
    timestamp,
    COMMAND_PUBLISH_NAME,
    transactionId,
    null,
    streamKey,
    streamType.value
)
