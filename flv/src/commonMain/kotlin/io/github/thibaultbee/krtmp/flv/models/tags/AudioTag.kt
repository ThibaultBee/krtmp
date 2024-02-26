/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.flv.models.av.config.FlvAudioConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundType
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source


fun AudioTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    audioConfig: FlvAudioConfig,
    aacPacketType: AACPacketType? = null
): AudioTag {
    return AudioTag(
        timestampMs,
        source,
        sourceSize,
        AudioTag.TagHeader(
            audioConfig.soundFormat,
            audioConfig.soundRate,
            audioConfig.soundSize,
            audioConfig.soundType,
            aacPacketType
        )
    )
}

class AudioTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    val tagHeader: TagHeader
) :
    AVFlvTag(timestampMs, FlvTag.Type.AUDIO, source, sourceSize) {
    private val tagBodySize = sourceSize

    override val bodySize = tagHeader.size + tagBodySize

    init {
        require((tagHeader.soundFormat != SoundFormat.AAC) || (tagHeader.aacPacketType != null)) { "AAC packet type is required for AAC audio" }
    }

    override fun writeTagHeader(output: Sink) {
        tagHeader.writeToSink(output)
    }

    override fun writeTagBody(output: Sink) {
        output.transferFrom(source)
    }

    companion object {
        fun read(source: Source, header: FlvTagPacket.Header): AudioTag {
            val tagHeader = TagHeader.read(source)
            val remainingSize = header.bodySize - tagHeader.size
            return AudioTag(
                header.timestampMs,
                source,
                remainingSize,
                tagHeader
            )
        }
    }

    class TagHeader(
        val soundFormat: SoundFormat,
        val soundRate: SoundRate,
        val soundSize: SoundSize,
        val soundType: SoundType,
        val aacPacketType: AACPacketType? = null
    ) {
        val size = if (aacPacketType != null) 2 else 1
        fun writeToSink(output: Sink) {
            output.writeByte(
                ((soundFormat.value.toInt() shl 4) or
                        (soundRate.value.toInt() shl 2) or
                        (soundSize.value.toInt() shl 1) or
                        (soundType.value).toInt()).toByte()
            )
            aacPacketType?.let {
                output.writeByte(it.value)
            }
        }

        companion object {
            fun read(source: Source): TagHeader {
                val byte = source.readByte()
                val soundFormat = SoundFormat.entryOf(((byte.toInt() and 0xF0) shr 4).toByte())
                val soundRate = SoundRate.entryOf(((byte.toInt() and 0x0C) shr 2).toByte())
                val soundSize = SoundSize.entryOf(((byte.toInt() and 0x02) shr 1).toByte())
                val soundType = SoundType.entryOf((byte.toInt() and 0x01).toByte())
                val aacPacketType = if (soundFormat == SoundFormat.AAC) {
                    AACPacketType.entryOf(source.readByte())
                } else {
                    null
                }
                return TagHeader(soundFormat, soundRate, soundSize, soundType, aacPacketType)
            }
        }
    }
}

enum class AACPacketType(val value: Byte) {
    SEQUENCE_HEADER(0),
    RAW(1);

    companion object {
        fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}
