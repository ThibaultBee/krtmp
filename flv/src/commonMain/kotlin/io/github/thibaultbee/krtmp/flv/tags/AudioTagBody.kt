package io.github.thibaultbee.krtmp.flv.tags

import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for audio tag body.
 */
interface IAudioTagBody {
    val size: Int
    fun encode(output: Sink)
}

class DefaultAudioTagBody(
    val data: RawSource,
    val dataSize: Int
) : IAudioTagBody {
    override val size = dataSize

    override fun encode(output: Sink) {
        output.write(data, dataSize.toLong())
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): DefaultAudioTagBody {
            return DefaultAudioTagBody(source, sourceSize)
        }
    }
}