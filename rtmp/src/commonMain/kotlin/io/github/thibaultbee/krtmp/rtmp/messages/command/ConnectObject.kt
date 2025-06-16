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
package io.github.thibaultbee.krtmp.rtmp.messages.command

import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.krtmp.rtmp.extensions.orNull
import io.github.thibaultbee.krtmp.rtmp.messages.ObjectEncoding
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_AUDIO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_CAPABILITIES
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_FLASH_VER
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_VIDEO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_VIDEO_FUNCTION
import kotlinx.serialization.Serializable


/**
 * A builder for creating a [ConnectObject].
 */
class ConnectObjectBuilder(
    var app: String,
    var flashVer: String = DEFAULT_FLASH_VER,
    var tcUrl: String,
    var swfUrl: String? = null,
    var fpad: Boolean = false,
    var capabilities: Int = DEFAULT_CAPABILITIES,
    var audioCodecs: List<AudioMediaType>? = DEFAULT_AUDIO_CODECS,
    var videoCodecs: List<VideoMediaType>? = DEFAULT_VIDEO_CODECS,
    var videoFunction: List<ConnectObject.VideoFunction> = DEFAULT_VIDEO_FUNCTION,
    var pageUrl: String? = null,
    var objectEncoding: ObjectEncoding = ObjectEncoding.AMF0
) {
    fun build() = ConnectObject(
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
}

/**
 * The object sent by the client to the connect command.
 *
 *  @param app The server application name the client is connected to
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