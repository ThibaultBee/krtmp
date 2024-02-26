/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.av.utils

import io.github.thibaultbee.krtmp.flv.models.packets.PacketWriter
import io.github.thibaultbee.krtmp.flv.models.av.utils.aac.AAC
import io.github.thibaultbee.krtmp.flv.models.av.utils.aac.ADTS
import kotlinx.io.Sink

/**
 * Audio Specific Config from [AAC.ADTS]
 *
 * @param adts the ADTS
 * @return the AudioSpecificConfig
 */
fun AudioSpecificConfig(adts: ADTS): AudioSpecificConfig {
    return AudioSpecificConfig(
        adts.profile,
        adts.sampleRate,
        adts.channelConfiguration
    )
}

/**
 * Audio Specific Config from ISO 14496 Part 3 Table 1.13 – Syntax of AudioSpecificConfig
 */
data class AudioSpecificConfig(
    val profile: AudioObjectType,
    val sampleRate: Int,
    val channelConfiguration: ADTS.ChannelConfiguration
) : PacketWriter() {
    /**
     * Write AudioSpecificConfig to [Sink]
     *
     * @param output the output sink
     */
    override fun writeToSink(output: Sink) {
        val frequencyIndex = ADTS.SamplingFrequencyIndex.entryOf(sampleRate)
        if (profile.value <= 0x1F) {
            output.writeByte(
                ((profile.value shl 3)
                        or (frequencyIndex.value shr 1)).toByte()
            )
        } else {
            throw NotImplementedError("Codec not supported")
        }
        if (frequencyIndex == ADTS.SamplingFrequencyIndex.EXPLICIT) {
            throw NotImplementedError("Explicit frequency is not supported")
        }
        output.writeByte(
            (((frequencyIndex.value and 0x01) shl 7) or (channelConfiguration.value.toInt() shl 3)).toByte()
        )
    }
}
