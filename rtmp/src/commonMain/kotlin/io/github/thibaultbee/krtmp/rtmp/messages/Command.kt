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

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader
import io.github.thibaultbee.krtmp.amf.elements.Amf3ElementReader
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfElementFactory
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import io.github.thibaultbee.krtmp.common.FourCCs
import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.rtmp.chunk.ChunkStreamId
import io.github.thibaultbee.krtmp.rtmp.extensions.orNull
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
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
    ) {
        createMessage(amfVersion).write(writeChannel, chunkSize, previousMessage)
    }

    override fun toString(): String {
        return "Command($name, $transactionId, $commandObject, ${arguments.contentToString()})"
    }

    companion object {
        private const val COMMAND_RESULT_NAME = "_result"
        private const val COMMAND_ERROR_NAME = "_error"

        private const val COMMAND_ON_STATUS_NAME = "onStatus"

        private const val COMMAND_CONNECT_NAME = "connect"

        private const val COMMAND_CREATE_STREAM_NAME = "createStream"
        private const val COMMAND_RELEASE_STREAM_NAME = "releaseStream"
        private const val COMMAND_CLOSE_STREAM_NAME = "closeStream"
        private const val COMMAND_DELETE_STREAM_NAME = "deleteStream"

        private const val COMMAND_FCPUBLISH_NAME = "FCPublish"
        private const val COMMAND_FCUNPUBLISH_NAME = "FCUnpublish"
        private const val COMMAND_PUBLISH_NAME = "publish"

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
                return when (name) {
                    COMMAND_RESULT_NAME -> {
                        Result(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            if (!payload.exhausted()) amfElementReader.read(payload) else null
                        )
                    }

                    COMMAND_ERROR_NAME -> {
                        Error(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            amfElementReader.read(payload)
                        )
                    }

                    COMMAND_ON_STATUS_NAME -> {
                        OnStatus(
                            commandMessage.messageStreamId,
                            transactionId,
                            commandMessage.timestamp,
                            commandObject,
                            amfElementReader.read(payload)
                        )
                    }

                    else -> {
                        Command(
                            commandMessage.chunkStreamId,
                            commandMessage.messageStreamId,
                            commandMessage.timestamp,
                            name,
                            transactionId,
                            commandObject
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
        resultObject: AmfElement?,
        informationObject: AmfElement?
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
        errorObject: AmfElement?,
        informationObject: AmfElement
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
        onStatusObject: AmfElement?,
        informationObject: AmfElement
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            messageStreamId,
            timestamp,
            COMMAND_ON_STATUS_NAME,
            transactionId,
            onStatusObject,
            informationObject
        )

    class Connect(
        transactionId: Long,
        timestamp: Int,
        connectObject: ConnectObject
    ) : Command(
        ChunkStreamId.PROTOCOL_CONTROL.value,
        MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp,
        COMMAND_CONNECT_NAME,
        transactionId,
        @OptIn(ExperimentalSerializationApi::class)
        Amf { explicitNulls = false }.encodeToAmfElement(
            ConnectObject.serializer(),
            connectObject
        )
    ) {

        init {
            require(transactionId == 1L) {
                "Transaction ID must be 1"
            }
        }

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
        private constructor(
            val app: String,
            val flashVer: String,
            val tcUrl: String,
            val swfUrl: String?,
            val fpad: Boolean,
            val capabilities: Double,
            val audioCodecs: Double?,
            val videoCodecs: Double?,
            val fourCcList: List<String>?,
            val videoFunction: Double,
            val pageUrl: String?,
            val objectEncoding: Double
        ) {
            constructor(
                app: String,
                flashVer: String = DEFAULT_FLASH_VER,
                tcUrl: String,
                swfUrl: String? = null,
                fpad: Boolean = false,
                capabilities: Int = DEFAULT_CAPABILITIES,
                audioCodecs: List<MimeType>? = DEFAULT_AUDIO_CODECS,
                videoCodecs: List<MimeType>? = DEFAULT_VIDEO_CODECS,
                videoFunction: List<VideoFunction> = DEFAULT_VIDEO_FUNCTION,
                pageUrl: String? = null,
                objectEncoding: ObjectEncoding = ObjectEncoding.AMF0
            ) : this(app,
                flashVer,
                tcUrl,
                swfUrl,
                fpad,
                capabilities.toDouble(),
                audioCodecs?.filter { AudioCodec.isSupportedCodec(it) }?.map {
                    AudioCodec.fromMimeTypes(listOf(it))
                }?.fold(0) { acc, audioCodec ->
                    acc or audioCodec.value
                }?.toDouble(),
                videoCodecs?.filter { VideoCodec.isSupportedCodec(it) }?.map {
                    VideoCodec.fromMimeType(it)
                }?.fold(0) { acc, videoCodec ->
                    acc or videoCodec.value
                }?.toDouble(),
                videoCodecs?.filter { ExVideoCodec.isSupportedCodec(it) }?.map {
                    ExVideoCodec.fromMimeType(it).value.toString()
                }?.orNull(),
                videoFunction.fold(0) { acc, vFunction ->
                    acc or vFunction.value
                }.toDouble(),
                pageUrl,
                objectEncoding.value.toDouble()
            )

            companion object {
                internal const val DEFAULT_FLASH_VER = "FMLE/3.0 (compatible; FMSc/1.0)"
                internal const val DEFAULT_CAPABILITIES = 239
                internal val DEFAULT_VIDEO_FUNCTION = emptyList<VideoFunction>()
                internal val DEFAULT_AUDIO_CODECS = listOf(
                    MimeType.AUDIO_AAC, MimeType.AUDIO_G711A, MimeType.AUDIO_G711U
                )
                internal val DEFAULT_VIDEO_CODECS = listOf(
                    MimeType.VIDEO_H263, MimeType.VIDEO_AVC
                )
            }
        }

        enum class AudioCodec(val value: Int, val mimeType: MimeType?) {
            NONE(0x0001, null),
            ADPCM(0x0002, null),
            MP3(0x0004, MimeType.AUDIO_MP3),
            INTEL(0x0008, null),
            UNUSED(0x0010, null),
            NELLY8(0x0020, null),
            NELLY(0x0040, null),
            G711A(0x0080, MimeType.AUDIO_G711A),
            G711U(0x0100, MimeType.AUDIO_G711U),
            NELLY16(0x0200, null),
            AAC(0x0400, MimeType.AUDIO_AAC),
            SPEEX(0x0800, null);

            companion object {
                fun isSupportedCodec(mimeType: MimeType): Boolean {
                    return entries.any { it.mimeType == mimeType }
                }

                fun fromMimeTypes(mimeTypes: List<MimeType>): AudioCodec {
                    return entries.first { it.mimeType in mimeTypes }
                }
            }
        }

        enum class VideoCodec(val value: Int, val mimeType: MimeType?) {
            UNUSED(0x01, null),
            JPEG(0x02, null),
            SORENSON(0x04, MimeType.VIDEO_H263),
            HOMEBREW(0x08, null),
            VP6(0x10, null),
            VP6ALPHA(0x20, null),
            HOMEBREWV(0x40, null),
            H264(0x80, MimeType.VIDEO_AVC);

            companion object {
                fun isSupportedCodec(mimeType: MimeType): Boolean {
                    return entries.any { it.mimeType == mimeType }
                }

                fun fromMimeType(mimeType: MimeType): VideoCodec {
                    return entries.first { it.mimeType == mimeType }
                }
            }
        }

        class ExVideoCodec {
            companion object {
                private val supportedCodecs = listOf(
                    MimeType.VIDEO_VP9, MimeType.VIDEO_HEVC, MimeType.VIDEO_AV1
                )

                fun isSupportedCodec(mimeType: MimeType): Boolean {
                    return supportedCodecs.contains(mimeType)
                }

                fun fromMimeType(mimeType: MimeType): FourCCs {
                    if (!isSupportedCodec(mimeType)) {
                        throw IllegalArgumentException("Unsupported codec: $mimeType")
                    }
                    return FourCCs.mimeTypeOf(mimeType)
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

        enum class ObjectEncoding(val value: Int) {
            AMF0(0),
            AMF3(3)
        }
    }

    class CreateStream(transactionId: Long, timestamp: Int) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            MessageStreamId.PROTOCOL_CONTROL.value,
            timestamp,
            COMMAND_CREATE_STREAM_NAME,
            transactionId,
            null
        )


    class ReleaseStream(
        transactionId: Long,
        timestamp: Int,
        streamKey: String
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            MessageStreamId.PROTOCOL_CONTROL.value,
            timestamp,
            COMMAND_RELEASE_STREAM_NAME,
            transactionId,
            null,
            streamKey
        )

    class CloseStream(transactionId: Long, timestamp: Int) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            MessageStreamId.PROTOCOL_CONTROL.value,
            timestamp,
            COMMAND_CLOSE_STREAM_NAME,
            transactionId,
            null
        )

    class DeleteStream(transactionId: Long, timestamp: Int, streamKey: String) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            MessageStreamId.PROTOCOL_CONTROL.value,
            timestamp,
            COMMAND_DELETE_STREAM_NAME,
            transactionId,
            null,
            streamKey
        )

    class FCPublish(
        transactionId: Long,
        timestamp: Int,
        streamKey: String
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            MessageStreamId.PROTOCOL_CONTROL.value,
            timestamp,
            COMMAND_FCPUBLISH_NAME,
            transactionId,

            null,
            streamKey
        )

    class FCUnpublish(
        messageStreamId: Int,
        transactionId: Long,
        timestamp: Int,
        streamKey: String
    ) :
        Command(
            ChunkStreamId.PROTOCOL_CONTROL.value,
            messageStreamId,
            timestamp,
            COMMAND_FCUNPUBLISH_NAME,
            transactionId,
            null,
            streamKey
        )


    class Publish(
        messageStreamId: Int,
        transactionId: Long,
        timestamp: Int,
        streamKey: String,
        streamType: Type
    ) : Command(
        ChunkStreamId.COMMAND_CHANNEL.value,
        messageStreamId,
        timestamp,
        COMMAND_PUBLISH_NAME,
        transactionId,
        null,
        streamKey,
        streamType.value
    ) {

        enum class Type(val value: String) {
            LIVE("live"), RECORD("record"), APPEND("append")
        }
    }
}
