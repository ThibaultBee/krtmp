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
package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.sources.MultiRawSource
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for audio tag body.
 */
interface AudioTagBody {
    val size: Int
    fun encode(output: Sink)
    fun readRawSource(): RawSource
}

interface SingleAudioTagBody : AudioTagBody

class RawAudioTagBody(
    val data: RawSource,
    val dataSize: Int
) : SingleAudioTagBody {
    override val size = dataSize

    override fun encode(output: Sink) {
        output.write(data, dataSize.toLong())
    }

    override fun readRawSource() = data

    override fun toString(): String {
        return "RawAudioTagBody(dataSize=$dataSize)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): RawAudioTagBody {
            return RawAudioTagBody(source.readSource(sourceSize.toLong()), sourceSize)
        }
    }
}

/**
 * Empty video tag body.
 */
internal class EmptyAudioTagBody : SingleAudioTagBody {
    override val size = 0

    override fun encode(output: Sink) =
        Unit  // End of sequence does not have a body

    override fun readRawSource() = Buffer()

    override fun toString(): String {
        return "Empty"
    }
}

sealed class MultichannelConfigAudioTagBody(
    val channelOrder: AudioChannelOrder,
    val channelCount: Byte,
) : SingleAudioTagBody {
    override val size = 8

    abstract fun encodeImpl(output: Sink)

    override fun encode(output: Sink) {
        output.writeByte(channelOrder.value)
        output.writeByte(channelCount)
    }

    override fun readRawSource(): RawSource {
        return Buffer().apply {
            encode(this)
        }
    }

    override fun toString(): String {
        return "MultichannelConfigAudioTagBody(channelOrder=$channelOrder, channelCount=$channelCount)"
    }

    companion object {
        fun decode(
            source: Source,
            sourceSize: Int
        ): MultichannelConfigAudioTagBody {
            require(sourceSize >= 2) { "Multichannel audio tag body must have at least 2 bytes" }
            val channelOrder = AudioChannelOrder.entryOf(source.readByte())
            val channelCount = source.readByte()
            val remainingSize = sourceSize - 2
            return when (channelOrder) {
                AudioChannelOrder.NATIVE -> NativeMultichannelConfigAudioTagBody.decode(
                    channelCount,
                    source,
                    remainingSize
                )

                AudioChannelOrder.CUSTOM -> CustomMultichannelConfigAudioTagBody.decode(
                    channelCount,
                    source,
                    remainingSize
                )

                AudioChannelOrder.UNSPECIFIED -> UnspecifiedMultichannelConfigAudioTagBody(
                    channelCount
                )
            }
        }
    }

    class UnspecifiedMultichannelConfigAudioTagBody(
        channelCount: Byte,
    ) : MultichannelConfigAudioTagBody(
        channelOrder = AudioChannelOrder.UNSPECIFIED,
        channelCount = channelCount,
    ) {
        override fun encodeImpl(output: Sink) = Unit
    }

    class NativeMultichannelConfigAudioTagBody(
        channelCount: Byte,
    ) : MultichannelConfigAudioTagBody(
        channelOrder = AudioChannelOrder.NATIVE,
        channelCount = channelCount,
    ) {
        override fun encodeImpl(output: Sink) = TODO("Not yet implemented")

        companion object {
            fun decode(
                channelCount: Byte,
                source: Source,
                sourceSize: Int
            ): NativeMultichannelConfigAudioTagBody {
                require(sourceSize >= 4) { "Native multichannel audio tag body must have at least 4 bytes" }
                source.skip(4)
                return NativeMultichannelConfigAudioTagBody(channelCount)
            }
        }
    }

    class CustomMultichannelConfigAudioTagBody(
        channelCount: Byte,
    ) : MultichannelConfigAudioTagBody(
        channelOrder = AudioChannelOrder.CUSTOM,
        channelCount = channelCount,
    ) {
        override fun encodeImpl(output: Sink) = TODO("Not yet implemented")

        companion object {
            fun decode(
                channelCount: Byte,
                source: Source,
                sourceSize: Int
            ): CustomMultichannelConfigAudioTagBody {
                require(sourceSize == channelCount.toInt()) { "Custom multichannel audio tag body must have at least $channelCount bytes" }
                source.skip(channelCount.toLong())
                return CustomMultichannelConfigAudioTagBody(channelCount)
            }
        }
    }


    enum class AudioChannelOrder(val value: Byte) {
        /**
         * Only the channel count is specified
         */
        UNSPECIFIED(0),

        /**
         * The native channel order
         */
        NATIVE(1),

        /**
         * The channel order does not correspond to any predefined order and is stored as an explicit map.
         */
        CUSTOM(2);

        companion object {
            fun entryOf(value: Byte) =
                entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                    "Invalid AudioChannelOrder value: $value"
                )
        }
    }
}


interface MultitrackAudioTagBody : AudioTagBody
interface OneCodecMultitrackAudioTagBody : MultitrackAudioTagBody

/**
 * One track audio tag body.
 *
 * @param trackId The track id of the audio.
 * @param body The audio tag body.
 */
class OneTrackAudioTagBody(
    val trackId: Byte,
    val body: SingleAudioTagBody
) : OneCodecMultitrackAudioTagBody {
    override val size = 1 + body.size

    private fun encodeHeader(output: Sink) {
        output.writeByte(trackId)
    }

    override fun encode(output: Sink) {
        encodeHeader(output)
        body.encode(output)
    }

    override fun readRawSource(): RawSource {
        return MultiRawSource(Buffer().apply { encodeHeader(this) }, body.readRawSource())
    }

    override fun toString(): String {
        return "OneTrackAudioTagBody(trackId=$trackId, body=$body)"
    }

    companion object {
        fun decode(
            source: Source, sourceSize: Int
        ): OneTrackAudioTagBody {
            require(sourceSize >= 1) { "One track audio tag body must have at least 1 byte" }
            val trackId = source.readByte()
            val body = RawAudioTagBody.decode(source, sourceSize - 1)
            return OneTrackAudioTagBody(trackId, body)
        }
    }
}

/**
 * Many track audio tag body with one codec.
 *
 * @param tracks The set of tracks.
 */
class ManyTrackOneCodecAudioTagBody internal constructor(
    val tracks: Set<OneTrackAudioTagBody>
) : OneCodecMultitrackAudioTagBody {
    init {
        require(tracks.size > 1) { "Many track audio tag body must have at least 2 tracks" }
    }

    override val size = tracks.sumOf { it.size + 3 } // +3 for sizeOfAudioTrack

    override fun encode(output: Sink) {
        tracks.forEach { track ->
            output.writeByte(track.trackId)
            output.writeInt24(track.body.size)
            track.body.encode(output)
        }
    }

    override fun readRawSource(): RawSource {
        val rawSources = mutableListOf<RawSource>()
        tracks.forEach { track ->
            rawSources.add(Buffer().apply {
                writeByte(track.trackId)
                writeInt24(track.body.size)
            })
            rawSources.add(track.body.readRawSource())
        }
        return MultiRawSource(rawSources)
    }

    override fun toString(): String {
        return "ManyTrackOneCodecAudioTagBody(tracks=$tracks)"
    }

    companion object {
        fun decode(
            source: Source, sourceSize: Int
        ): ManyTrackOneCodecAudioTagBody {
            val tracks = mutableSetOf<OneTrackAudioTagBody>()
            var remainingSize = sourceSize
            while (remainingSize > 0) {
                val trackId = source.readByte()
                val sizeOfAudioTrack = source.readInt24()
                val body = RawAudioTagBody.decode(source, sizeOfAudioTrack)
                tracks.add(
                    OneTrackAudioTagBody(
                        trackId,
                        body
                    )
                )
                remainingSize -= sizeOfAudioTrack
            }
            return ManyTrackOneCodecAudioTagBody(tracks)
        }
    }
}

/**
 * Many track audio tag body with many codecs.
 *
 * @param tracks The set of tracks.
 */
class ManyTrackManyCodecAudioTagBody(
    val tracks: Set<OneTrackMultiCodecAudioTagBody>
) : MultitrackAudioTagBody {
    init {
        require(tracks.size > 1) { "Many track video tag body must have at least 2 tracks" }
    }
    
    override val size = tracks.sumOf { it.size }

    override fun encode(output: Sink) {
        tracks.forEach { track ->
            track.encode(output)
        }
    }

    override fun readRawSource(): RawSource {
        return MultiRawSource(tracks.map { it.readRawSource() })
    }

    override fun toString(): String {
        return "ManyTrackManyCodecAudioTagBody(tracks=$tracks)"
    }

    companion object {
        fun decode(
            source: Source, sourceSize: Int
        ): ManyTrackManyCodecAudioTagBody {
            val tracks = mutableSetOf<OneTrackMultiCodecAudioTagBody>()
            var remainingSize = sourceSize
            while (remainingSize > 0) {
                val track = OneTrackMultiCodecAudioTagBody.decode(source, remainingSize)
                tracks.add(track)
                remainingSize -= track.size
            }
            return ManyTrackManyCodecAudioTagBody(tracks)
        }
    }

    data class OneTrackMultiCodecAudioTagBody(
        val fourCC: AudioFourCC,
        val trackId: Byte = 0,
        val body: SingleAudioTagBody
    ) {
        val size = 8 + body.size

        private fun encodeHeader(output: Sink) {
            output.writeInt(fourCC.value.code)
            output.writeByte(trackId)
            output.writeInt24(body.size)
        }

        fun encode(output: Sink) {
            encodeHeader(output)
            body.encode(output)
        }

        fun readRawSource(): RawSource {
            return MultiRawSource(
                Buffer().apply {
                    encodeHeader(this)
                },
                body.readRawSource()
            )
        }

        companion object {
            fun decode(
                source: Source, sourceSize: Int
            ): OneTrackMultiCodecAudioTagBody {
                val fourCC = AudioFourCC.codeOf(source.readInt())
                val trackId = source.readByte()
                val sizeOfAudioTrack = source.readInt24()
                val body = RawAudioTagBody.decode(source, sizeOfAudioTrack)
                return OneTrackMultiCodecAudioTagBody(fourCC, trackId, body)
            }
        }
    }
}
