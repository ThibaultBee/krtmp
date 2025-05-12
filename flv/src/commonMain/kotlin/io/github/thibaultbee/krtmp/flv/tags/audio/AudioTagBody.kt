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
