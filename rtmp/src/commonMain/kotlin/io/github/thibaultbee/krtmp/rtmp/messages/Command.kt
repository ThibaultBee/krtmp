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
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.krtmp.rtmp.chunk.ChunkStreamId
import io.github.thibaultbee.krtmp.rtmp.extensions.orNull
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CLOSE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CONNECT_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CREATE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_DELETE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCUNPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PLAY_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_RELEASE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject.Companion.DEFAULT_AUDIO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject.Companion.DEFAULT_CAPABILITIES
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject.Companion.DEFAULT_FLASH_VER
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject.Companion.DEFAULT_VIDEO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject.Companion.DEFAULT_VIDEO_FUNCTION
import io.github.thibaultbee.krtmp.rtmp.util.AmfUtil.amf
import io.github.thibaultbee.krtmp.rtmp.util.NetConnectionConnectCode
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCode
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.serialization.Serializable

class CommandMessage(
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
        return "Command($name, $transactionId, $commandObject, ${arguments.contentToString()})"
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
        internal const val COMMAND_FCUNPUBLISH_NAME = "FCUnpublish"
        internal const val COMMAND_PUBLISH_NAME = "publish"

        fun read(
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
                        require(arguments.size == 1) {
                            "result command must have exactly one argument"
                        }
                        Result(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            arguments.firstOrNull()
                        )
                    }

                    COMMAND_ERROR_NAME -> {
                        require(arguments.size == 1) {
                            "error command must have exactly one argument"
                        }
                        Error(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            arguments.firstOrNull()
                        )
                    }

                    COMMAND_ON_STATUS_NAME -> {
                        require(arguments.size == 1) {
                            "onStatus command must have exactly one argument"
                        }
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
        informationObject: AmfElement? = null
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
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
        informationObject: AmfElement? = null
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
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
        information: NetStreamOnStatusInformation
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
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
            val details: String
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

enum class ObjectEncoding(val value: Int) {
    AMF0(0),
    AMF3(3)
}

fun CommandConnectResultErrorInformation(
    level: String,
    code: NetConnectionConnectCode,
    description: String,
    objectEncoding: ObjectEncoding
) = NetConnectionResultInformation(
    level = level,
    code = code,
    description = description,
    objectEncoding = objectEncoding.value.toDouble()
)

@Serializable
class NetConnectionResultInformation(
    override val level: String,
    override val code: NetConnectionConnectCode,
    override val description: String,
    val objectEncoding: Double,
) : ResultInformation

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
            CommandConnectResultErrorInformation(
                level = "status",
                code = "NetConnection.Connect.Success",
                description = "Connection succeeded.",
                objectEncoding = objectEncoding
            )
        ),
    )
}

@Serializable
class NetConnectionResultObject(
    val fmsVer: String = DEFAULT_FMS_VER,
    val capabilities: Double = DEFAULT_CAPABILITIES.toDouble(),
) {
    companion object {
        internal const val DEFAULT_FMS_VER = "FMS/3,0,1,123"

        internal val default = NetConnectionResultObject()
    }
}

fun CommandConnect(
    transactionId: Long,
    timestamp: Int,
    connectObject: ConnectObject
): Command {
    require(transactionId == 1L) {
        "Transaction ID must be 1"
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

/**
 * Creates a [ConnectObject] with a more intelligible API.
 */
fun ConnectObject(
    app: String,
    flashVer: String = DEFAULT_FLASH_VER,
    tcUrl: String,
    swfUrl: String? = null,
    fpad: Boolean = false,
    capabilities: Int = DEFAULT_CAPABILITIES,
    audioCodecs: List<AudioMediaType>? = DEFAULT_AUDIO_CODECS,
    videoCodecs: List<VideoMediaType>? = DEFAULT_VIDEO_CODECS,
    videoFunction: List<ConnectObject.VideoFunction> = DEFAULT_VIDEO_FUNCTION,
    pageUrl: String? = null,
    objectEncoding: ObjectEncoding = ObjectEncoding.AMF0
) = ConnectObject(
    app,
    flashVer,
    tcUrl,
    swfUrl,
    fpad,
    capabilities.toDouble(),
    audioCodecs?.filter { ConnectObject.AudioCodec.isSupportedCodec(it) }?.map {
        ConnectObject.AudioCodec.fromMediaTypes(listOf(it))
    }?.fold(0) { acc, audioCodec ->
        acc or audioCodec.value
    }?.toDouble(),
    videoCodecs?.filter { ConnectObject.VideoCodec.isSupportedCodec(it) }?.map {
        ConnectObject.VideoCodec.fromMimeType(it)
    }?.fold(0) { acc, videoCodec ->
        acc or videoCodec.value
    }?.toDouble(),
    videoCodecs?.filter { ConnectObject.ExVideoCodec.isSupportedCodec(it) }?.map {
        ConnectObject.ExVideoCodec.fromMediaType(it).value.toString()
    }?.orNull(),
    videoFunction.fold(0) { acc, vFunction ->
        acc or vFunction.value
    }.toDouble(),
    pageUrl,
    objectEncoding.value.toDouble()
)

/**
 * @param app The server application name the client is connected to
 * @param flashVer The flash Player version
 * @param tcUrl The server IP address the client is connected to (format: rtmp://ip:port/app/instance)
 * @param swfUrl The URL of the source SWF file
 * @param fpad True if proxy is used
 * @param audioCodecs The supported (by the client) audio codecs
 * @param videoCodecs The supported (by the client) video codecs
 * @param fourCcList The supported (by the client) video codecs (extended RTMP)
 * @param pageUrl  The URL of the web page in which the media was embedded
 * @param objectEncoding The AMF encoding version
 */
@Serializable
class ConnectObject
internal constructor(
    val app: String,
    val flashVer: String = DEFAULT_FLASH_VER,
    val tcUrl: String,
    val swfUrl: String? = null,
    val fpad: Boolean = false,
    val capabilities: Double = DEFAULT_CAPABILITIES.toDouble(),
    val audioCodecs: Double?,
    val videoCodecs: Double?,
    val fourCcList: List<String>?,
    val videoFunction: Double = 0.0,
    val pageUrl: String?,
    val objectEncoding: Double = ObjectEncoding.AMF0.value.toDouble()
) {
    override fun toString(): String {
        return "ConnectObject(app='$app', flashVer='$flashVer', tcUrl='$tcUrl', swfUrl=$swfUrl, fpad=$fpad, capabilities=$capabilities, audioCodecs=$audioCodecs, videoCodecs=$videoCodecs, fourCcList=$fourCcList, videoFunction=$videoFunction, pageUrl=$pageUrl, objectEncoding=$objectEncoding)"
    }

    companion object {
        internal const val DEFAULT_FLASH_VER = "FMLE/3.0 (compatible; FMSc/1.0)"
        internal const val DEFAULT_CAPABILITIES = 239
        internal val DEFAULT_VIDEO_FUNCTION = emptyList<VideoFunction>()
        internal val DEFAULT_AUDIO_CODECS = listOf(
            AudioMediaType.AAC, AudioMediaType.G711_ALAW, AudioMediaType.G711_MLAW
        )
        internal val DEFAULT_VIDEO_CODECS = listOf(
            VideoMediaType.SORENSON_H263, VideoMediaType.AVC
        )
    }


    enum class AudioCodec(val value: Int, val mediaType: AudioMediaType?) {
        NONE(0x0001, null),
        ADPCM(0x0002, AudioMediaType.ADPCM),
        MP3(0x0004, AudioMediaType.MP3),
        INTEL(0x0008, null),
        UNUSED(0x0010, null),
        NELLY8(0x0020, AudioMediaType.NELLYMOSER_8KHZ),
        NELLY(0x0040, AudioMediaType.NELLYMOSER),
        G711A(0x0080, AudioMediaType.G711_ALAW),
        G711U(0x0100, AudioMediaType.G711_MLAW),
        NELLY16(0x0200, AudioMediaType.NELLYMOSER_16KHZ),
        AAC(0x0400, AudioMediaType.AAC),
        SPEEX(0x0800, AudioMediaType.SPEEX);

        companion object {
            fun isSupportedCodec(mediaType: AudioMediaType): Boolean {
                return entries.any { it.mediaType == mediaType }
            }

            fun fromMediaTypes(mediaTypes: List<AudioMediaType>): AudioCodec {
                return entries.firstOrNull { it.mediaType in mediaTypes }
                    ?: throw IllegalArgumentException("Unsupported codec: $mediaTypes")
            }
        }
    }

    enum class VideoCodec(val value: Int, val mediaType: VideoMediaType?) {
        UNUSED(0x01, null),
        JPEG(0x02, null),
        SORENSON(0x04, VideoMediaType.SORENSON_H263),
        HOMEBREW(0x08, null),
        VP6(0x10, VideoMediaType.VP6),
        VP6_ALPHA(0x20, VideoMediaType.VP6_ALPHA),
        HOMEBREWV(0x40, null),
        H264(0x80, VideoMediaType.AVC);

        companion object {
            fun isSupportedCodec(mediaType: VideoMediaType): Boolean {
                return entries.any { it.mediaType == mediaType }
            }

            fun fromMimeType(mediaType: VideoMediaType): VideoCodec {
                return entries.firstOrNull { it.mediaType == mediaType }
                    ?: throw IllegalArgumentException("Unsupported codec: $mediaType")
            }
        }
    }

    class ExVideoCodec {
        companion object {
            private val supportedCodecs = listOf(
                VideoMediaType.VP9, VideoMediaType.HEVC, VideoMediaType.AV1
            )

            fun isSupportedCodec(mediaType: VideoMediaType): Boolean {
                return supportedCodecs.contains(mediaType)
            }

            fun fromMediaType(mediaType: VideoMediaType): VideoFourCC {
                if (!isSupportedCodec(mediaType)) {
                    throw IllegalArgumentException("Unsupported codec: $mediaType")
                }
                return mediaType.fourCCs ?: throw IllegalArgumentException(
                    "Unsupported codec: $mediaType"
                )
            }
        }
    }

    enum class VideoFunction(val value: Int) {
        CLIENT_SEEK(0x1),

        // Enhanced RTMP v1
        CLIENT_HDR(0x2),
        CLIENT_PACKET_TYPE_METADATA(0x4),
        CLIENT_LARGE_SCALE_TILE(0x8),
    }
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

fun CommandDeleteStream(transactionId: Long, timestamp: Int, streamKey: String) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_DELETE_STREAM_NAME,
        transactionId,
        null,
        streamKey
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

fun CommandFCUnpublish(
    messageStreamId: Int,
    transactionId: Long,
    timestamp: Int,
    streamKey: String
) =
    Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        messageStreamId,
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

enum class StreamPublishType(val value: String) {
    LIVE("live"), RECORD("record"), APPEND("append");

    companion object {
        fun valueOf(value: String): StreamPublishType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown stream type: $value")
        }
    }
}
