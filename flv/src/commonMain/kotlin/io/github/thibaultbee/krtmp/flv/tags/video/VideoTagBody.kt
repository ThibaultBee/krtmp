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
package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.containers.amf0ContainerFrom
import io.github.thibaultbee.krtmp.amf.elements.containers.amfContainerOf
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.tags.video.ExtendedVideoData.SingleVideoPacketDescriptor.Companion.decodeBody
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for video tag body.
 */
interface VideoTagBody {
    val size: Int
    fun encode(output: Sink, amfVersion: AmfVersion)
}

interface SingleVideoTagBody : VideoTagBody

/**
 * Metadata video tag body.
 */
class MetadataVideoTagBody(
    val name: String,
    val value: AmfElement
) : SingleVideoTagBody {
    private val container = amfContainerOf(listOf(name, value))
    override val size = container.size0

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        container.write(amfVersion, output)
    }

    override fun toString(): String {
        return "MetadataVideoTagBody(name=$name, value=$value)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): MetadataVideoTagBody {
            val container = amf0ContainerFrom(2, source)
            val name = (container.first() as AmfString)
            val value = container.last()
            return MetadataVideoTagBody(name.value, value)
        }
    }
}

/**
 * Default video tag body.
 */
class RawVideoTagBody(
    val data: RawSource,
    val dataSize: Int
) : SingleVideoTagBody {
    override val size = dataSize

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.write(data, dataSize.toLong())
    }

    override fun toString(): String {
        return "RawVideoTagBody(dataSize=$dataSize)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): RawVideoTagBody {
            return RawVideoTagBody(source.readSource(sourceSize.toLong()), sourceSize)
        }
    }
}

internal class CommandLegacyVideoTagBody(
    val command: VideoCommand
) : VideoTagBody {
    override val size = 1

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.writeByte(command.value)
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): CommandLegacyVideoTagBody {
            require(sourceSize >= 1) { "Command video tag body must have at least 1 byte" }
            val command = source.readByte()
            return CommandLegacyVideoTagBody(VideoCommand.entryOf(command))
        }
    }
}

/**
 * Empty video tag body.
 */
internal class EmptyVideoTagBody : SingleVideoTagBody {
    override val size = 0
    override fun encode(output: Sink, amfVersion: AmfVersion) =
        Unit  // End of sequence does not have a body

    override fun toString(): String {
        return "Empty"
    }
}


/**
 * AVC HEVC coded frame video tag body.
 *
 * Only to be used with extended AVC and HEVC codec when packet type is
 * [VideoPacketType.CODED_FRAMES].
 *
 * @param compositionTime 24 bits composition time
 * @param data The raw source data of the video frame.
 * @param dataSize The size of the data in bytes.
 */
class CompositionTimeExtendedVideoTagBody(
    val compositionTime: Int, // 24 bits
    val data: RawSource,
    val dataSize: Int
) : SingleVideoTagBody {
    override val size = 3 + dataSize

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.writeInt24(compositionTime)
        output.write(data, dataSize.toLong())
    }

    override fun toString(): String {
        return "ExtendedWithCompositionTimeVideoTagBody(compositionTime=$compositionTime, dataSize=$dataSize)"
    }

    companion object {
        fun decode(
            source: Source, sourceSize: Int
        ): CompositionTimeExtendedVideoTagBody {
            val compositionTime = source.readInt24()
            val remainingSize = sourceSize - 3
            return CompositionTimeExtendedVideoTagBody(
                compositionTime,
                source.readSource(remainingSize.toLong()),
                remainingSize
            )
        }
    }
}


interface MultitrackVideoTagBody : VideoTagBody
interface OneCodecMultitrackVideoTagBody : MultitrackVideoTagBody

/**
 * One track video tag body.
 *
 * @param trackId The track id of the video.
 * @param body The video tag body.
 */
class OneTrackVideoTagBody(
    val trackId: Byte,
    val body: SingleVideoTagBody
) : OneCodecMultitrackVideoTagBody {
    override val size = 1 + body.size

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.writeByte(trackId)
        body.encode(output, amfVersion)
    }

    override fun toString(): String {
        return "OneTrackVideoTagBody(trackId=$trackId, body=$body)"
    }

    companion object {
        fun decode(
            packetType: VideoPacketType, fourCC: VideoFourCC, source: Source, sourceSize: Int
        ): OneTrackVideoTagBody {
            require(sourceSize >= 1) { "One track video tag body must have at least 1 byte" }
            val trackId = source.readByte()
            val body = decodeBody(packetType, fourCC, source, sourceSize - 1)
            return OneTrackVideoTagBody(trackId, body)
        }
    }
}

/**
 * Many track video tag body with one codec.
 *
 * @param tracks The set of tracks.
 */
class ManyTrackOneCodecVideoTagBody internal constructor(
    val tracks: Set<OneTrackVideoTagBody>
) : OneCodecMultitrackVideoTagBody {
    override val size = tracks.sumOf { it.size + 3 } // +3 for sizeOfVideoTrack

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        tracks.forEach { track ->
            output.writeByte(track.trackId)
            output.writeInt24(track.body.size)
            track.body.encode(output, amfVersion)
        }
    }

    override fun toString(): String {
        return "ManyTrackOneCodecVideoTagBody(tracks=$tracks)"
    }

    companion object {
        fun decode(
            packetType: VideoPacketType, fourCC: VideoFourCC, source: Source, sourceSize: Int
        ): ManyTrackOneCodecVideoTagBody {
            val tracks = mutableSetOf<OneTrackVideoTagBody>()
            var remainingSize = sourceSize
            while (remainingSize > 0) {
                val trackId = source.readByte()
                val sizeOfVideoTrack = source.readInt24()
                val body = decodeBody(packetType, fourCC, source, sizeOfVideoTrack)
                tracks.add(OneTrackVideoTagBody(trackId, body))
                remainingSize -= sizeOfVideoTrack
            }
            return ManyTrackOneCodecVideoTagBody(tracks)
        }
    }
}

/**
 * Many track video tag body with many codecs.
 *
 * @param tracks The set of tracks.
 */
class ManyTrackManyCodecVideoTagBody(
    val tracks: Set<OneTrackMultiCodecVideoTagBody>
) : MultitrackVideoTagBody {
    override val size = tracks.sumOf { it.size }

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        tracks.forEach { track ->
            track.encode(output, amfVersion)
        }
    }

    override fun toString(): String {
        return "ManyTrackManyCodecVideoTagBody(tracks=$tracks)"
    }

    companion object {
        fun decode(
            packetType: VideoPacketType, source: Source, sourceSize: Int
        ): ManyTrackManyCodecVideoTagBody {
            val tracks = mutableSetOf<OneTrackMultiCodecVideoTagBody>()
            var remainingSize = sourceSize
            while (remainingSize > 0) {
                val track = OneTrackMultiCodecVideoTagBody.decode(packetType, source, remainingSize)
                tracks.add(track)
                remainingSize -= track.size
            }
            return ManyTrackManyCodecVideoTagBody(tracks)
        }
    }

    data class OneTrackMultiCodecVideoTagBody(
        val fourCC: VideoFourCC,
        val trackId: Byte = 0,
        val body: SingleVideoTagBody
    ) {
        val size = 8 + body.size

        fun encode(output: Sink, amfVersion: AmfVersion) {
            output.writeInt(fourCC.value.code)
            output.writeByte(trackId)
            output.writeInt24(body.size)
            body.encode(output, amfVersion)
        }

        companion object {
            fun decode(
                packetType: VideoPacketType, source: Source, sourceSize: Int
            ): OneTrackMultiCodecVideoTagBody {
                val fourCC = VideoFourCC.codeOf(source.readInt())
                val trackId = source.readByte()
                val sizeOfVideoTrack = source.readInt24()
                val body = decodeBody(packetType, fourCC, source, sizeOfVideoTrack)
                return OneTrackMultiCodecVideoTagBody(fourCC, trackId, body)
            }
        }
    }
}
