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
import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.sources.MultiRawSource
import io.github.thibaultbee.krtmp.flv.tags.video.SingleVideoTagBody.Companion.decode
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for video tag body.
 */
interface VideoTagBody : AutoCloseable {
    fun getSize(amfVersion: AmfVersion): Int
    fun encode(output: Sink, amfVersion: AmfVersion)
    fun asRawSource(amfVersion: AmfVersion): RawSource

    /**
     * Closes the video tag body and releases any resources associated with it.
     */
    override fun close() {}
}

interface SingleVideoTagBody : VideoTagBody {
    companion object {
        internal fun decode(
            packetType: VideoPacketType,
            fourCC: VideoFourCC,
            source: Source,
            sourceSize: Int
        ): SingleVideoTagBody {
            return if (sourceSize == 0) {
                EmptyVideoTagBody()
            } else if ((packetType == VideoPacketType.CODED_FRAMES) && ((fourCC == VideoFourCC.HEVC) ||
                        (fourCC == VideoFourCC.AVC))
            ) {
                CompositionTimeExtendedVideoTagBody.decode(source, sourceSize)
            } else if (packetType == VideoPacketType.META_DATA) {
                MetadataVideoTagBody.decode(source, sourceSize)
            } else {
                RawVideoTagBody.decode(source, sourceSize)
            }
        }
    }
}

/**
 * Metadata video tag body.
 */
class MetadataVideoTagBody(
    val name: String,
    val value: AmfElement
) : SingleVideoTagBody {
    private val container = amfContainerOf(listOf(name, value))
    override fun getSize(amfVersion: AmfVersion): Int {
        return if (amfVersion == AmfVersion.AMF0) {
            container.size0 // AMF0 size
        } else {
            container.size3 // AMF3 size
        }
    }

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        container.write(amfVersion, output)
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return Buffer().apply {
            encode(this, amfVersion)
        }
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
 * Creates a raw video tag body from a [ByteArray].
 *
 * @param data the coded frame as a [ByteArray]
 */
fun RawVideoTagBody(data: ByteArray) =
    RawVideoTagBody(ByteArrayBackedRawSource(data), data.size)

/**
 * Default video tag body for a single frame.
 *
 * @param data the coded frame as a [RawSource]
 * @param dataSize The size of the [data]
 */
class RawVideoTagBody(
    val data: RawSource,
    val dataSize: Int
) : SingleVideoTagBody {
    override fun getSize(amfVersion: AmfVersion) = dataSize

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.write(data, dataSize.toLong())
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return data
    }

    override fun close() {
        data.close()
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
    override fun getSize(amfVersion: AmfVersion) = 1

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        output.writeByte(command.value)
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return Buffer().apply { encode(this, amfVersion) }
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
    override fun getSize(amfVersion: AmfVersion) = 0
    override fun encode(output: Sink, amfVersion: AmfVersion) =
        Unit  // End of sequence does not have a body

    override fun asRawSource(amfVersion: AmfVersion) = Buffer()

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
 * @param data The coded frame as a [RawSource]
 * @param dataSize The size of the [data]
 */
class CompositionTimeExtendedVideoTagBody(
    val compositionTime: Int, // 24 bits
    val data: RawSource,
    val dataSize: Int
) : SingleVideoTagBody {
    private val size = 3 + dataSize
    override fun getSize(amfVersion: AmfVersion) = size

    private fun encodeHeader(output: Sink) {
        output.writeInt24(compositionTime)
    }

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        encodeHeader(output)
        output.write(data, dataSize.toLong())
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return MultiRawSource(
            Buffer().apply { encodeHeader(this) },
            data
        )
    }

    override fun close() {
        data.close()
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
    override fun getSize(amfVersion: AmfVersion) = 1 + body.getSize(amfVersion)

    private fun encodeHeader(output: Sink) {
        output.writeByte(trackId)
    }

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        encodeHeader(output)
        body.encode(output, amfVersion)
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return MultiRawSource(
            Buffer().apply { encodeHeader(this) },
            body.asRawSource(amfVersion)
        )
    }

    /**
     * Closes the body of the one track video tag body.
     */
    override fun close() {
        body.close()
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
            val body = SingleVideoTagBody.decode(packetType, fourCC, source, sourceSize - 1)
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
    init {
        require(tracks.size > 1) { "Many track video tag body must have at least 2 tracks" }
    }

    override fun getSize(amfVersion: AmfVersion) =
        tracks.sumOf { it.getSize(amfVersion) + 3 } // +3 for sizeOfVideoTrack

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        tracks.forEach { track ->
            output.writeByte(track.trackId)
            output.writeInt24(track.body.getSize(amfVersion))
            track.body.encode(output, amfVersion)
        }
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        val rawSources = mutableListOf<RawSource>()
        tracks.forEach { track ->
            rawSources.add(Buffer().apply {
                writeByte(track.trackId)
                writeInt24(track.body.getSize(amfVersion))
            })
            rawSources.add(track.body.asRawSource(amfVersion))
        }
        return MultiRawSource(rawSources)
    }

    /**
     * Closes all the tracks in the many track video tag body.
     */
    override fun close() {
        tracks.forEach { it.close() }
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
                val body = SingleVideoTagBody.decode(packetType, fourCC, source, sizeOfVideoTrack)
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
    init {
        require(tracks.size > 1) { "Many track video tag body must have at least 2 tracks" }
    }

    override fun getSize(amfVersion: AmfVersion): Int {
        return tracks.sumOf { it.getSize(amfVersion) }
    }

    override fun encode(output: Sink, amfVersion: AmfVersion) {
        tracks.forEach { track ->
            track.encode(output, amfVersion)
        }
    }

    override fun asRawSource(amfVersion: AmfVersion): RawSource {
        return MultiRawSource(tracks.map { it.asRawSource(amfVersion) })
    }

    override fun close() {
        tracks.forEach { it.close() }
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
                remainingSize -= track.getSize(AmfVersion.AMF0) // AMF version does not matter here
            }
            return ManyTrackManyCodecVideoTagBody(tracks)
        }
    }

    data class OneTrackMultiCodecVideoTagBody(
        val fourCC: VideoFourCC,
        val trackId: Byte = 0,
        val body: SingleVideoTagBody
    ) : AutoCloseable {
        fun getSize(amfVersion: AmfVersion) = 8 + body.getSize(amfVersion)

        private fun encodeHeader(output: Sink, amfVersion: AmfVersion) {
            output.writeInt(fourCC.value.code)
            output.writeByte(trackId)
            output.writeInt24(body.getSize(amfVersion))
        }

        fun encode(output: Sink, amfVersion: AmfVersion) {
            encodeHeader(output, amfVersion)
            body.encode(output, amfVersion)
        }

        fun asRawSource(amfVersion: AmfVersion): RawSource {
            return MultiRawSource(
                Buffer().apply {
                    encodeHeader(this, amfVersion)
                },
                body.asRawSource(amfVersion)
            )
        }

        /**
         * Closes the body of the one track multi codec video tag body.
         */
        override fun close() {
            body.close()
        }

        companion object {
            fun decode(
                packetType: VideoPacketType, source: Source, sourceSize: Int
            ): OneTrackMultiCodecVideoTagBody {
                val fourCC = VideoFourCC.codeOf(source.readInt())
                val trackId = source.readByte()
                val sizeOfVideoTrack = source.readInt24()
                val body = decode(packetType, fourCC, source, sizeOfVideoTrack)
                return OneTrackMultiCodecVideoTagBody(fourCC, trackId, body)
            }
        }
    }
}
