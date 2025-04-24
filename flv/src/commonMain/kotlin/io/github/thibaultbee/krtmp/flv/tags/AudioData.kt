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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import kotlinx.io.Sink
import kotlinx.io.Source

class AudioData(
    val soundFormat: SoundFormat,
    val soundRate: SoundRate,
    val soundSize: SoundSize,
    val soundType: SoundType,
    val body: DefaultAudioTagBody,
    val aacPacketType: AACPacketType? = null,
) :
    FLVData {
    private val size = body.size + if (soundFormat == SoundFormat.AAC) 2 else 1

    override fun getSize(amfVersion: AmfVersion) = size

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((soundFormat.value.toInt() shl 4) or
                    (soundRate.value.toInt() shl 2) or
                    (soundSize.value.toInt() shl 1) or
                    (soundType.value).toInt()).toByte()
        )
        if (soundFormat == SoundFormat.AAC) {
            output.writeByte(aacPacketType!!.value)
        }
        body.encode(output)
    }

    override fun toString(): String {
        return "AudioData(soundFormat=$soundFormat, soundRate=$soundRate, soundSize=$soundSize, soundType=$soundType, body=$body)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): AudioData {
            val byte = source.readByte()
            val soundFormat = SoundFormat.entryOf(((byte.toInt() and 0xF0) shr 4).toByte())
            val soundRate = SoundRate.entryOf(((byte.toInt() and 0x0C) shr 2).toByte())
            val soundSize = SoundSize.entryOf(((byte.toInt() and 0x02) shr 1).toByte())
            val soundType = SoundType.entryOf((byte.toInt() and 0x01).toByte())
            return if (soundFormat == SoundFormat.AAC) {
                val aacPacketType = AACPacketType.entryOf(source.readByte())
                val remainingSize = sourceSize - 2
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = DefaultAudioTagBody.decode(source, remainingSize)
                AudioData(soundFormat, soundRate, soundSize, soundType, body, aacPacketType)
            } else {
                val remainingSize = sourceSize - 1
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = DefaultAudioTagBody.decode(source, remainingSize)
                AudioData(soundFormat, soundRate, soundSize, soundType, body)
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