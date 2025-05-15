package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for audio tag body.
 */
interface AudioTagBody {
    val size: Int
    fun encode(output: Sink)
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

    override fun toString(): String {
        return "RawAudioTagBody(dataSize=$dataSize)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): RawAudioTagBody {
            return RawAudioTagBody(source.readSource(sourceSize.toLong()), sourceSize)
        }
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

    override fun encode(output: Sink) {
        output.writeByte(trackId)
        body.encode(output)
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
    override val size = tracks.sumOf { it.size + 3 } // +3 for sizeOfAudioTrack

    override fun encode(output: Sink) {
        tracks.forEach { track ->
            output.writeByte(track.trackId)
            output.writeInt24(track.body.size)
            track.body.encode(output)
        }
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
    override val size = tracks.sumOf { it.size }

    override fun encode(output: Sink) {
        tracks.forEach { track ->
            track.encode(output)
        }
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

        fun encode(output: Sink) {
            output.writeInt(fourCC.value.code)
            output.writeByte(trackId)
            output.writeInt24(body.size)
            body.encode(output)
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
