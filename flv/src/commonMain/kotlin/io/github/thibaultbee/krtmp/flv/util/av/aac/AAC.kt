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
package io.github.thibaultbee.krtmp.flv.util.av.aac

import io.github.thibaultbee.krtmp.flv.bitreaders.BitReader
import io.github.thibaultbee.krtmp.flv.util.av.AudioObjectType
import kotlinx.io.Source

/**
 * AAC related classes
 */
object AAC {
    fun ADTS(source: Source): ADTS {
        val bitReader = BitReader(source)
        require(source.request(2)) { "Not enough data to read ADTS header till protection absence. Expects 7 or 9." }

        // Check header
        require(bitReader.readShort(12) == 0xFFF.toShort()) { "Invalid ADTS header. Invalid sync word" }
        // Check layer
        require(bitReader.readShort(3) == 0.toShort()) { "Invalid ADTS header. Only 0 expected in the layer" }
        val protectionAbsent = bitReader.readBoolean()
        val remainingSize = if (protectionAbsent) {
            5
        } else {
            7
        }
        require(bitReader.request(remainingSize)) { "Not enough data to read ADTS header. Expects ${remainingSize + 2}." }

        val profile = AudioObjectType.entryOf(bitReader.readInt(2) + 1)
        val samplingFrequencyIndex = ADTS.SamplingFrequencyIndex.entryOf(bitReader.readInt(4))

        bitReader.readBoolean() // Private bit

        val channelConfiguration = ADTS.ChannelConfiguration.entryOf(bitReader.readShort(3))

        bitReader.readByte(4) // Originality, Home, Copyright
        val frameLength = bitReader.readInt(13) - if (protectionAbsent) {
            ADTS.HEADER_SIZE
        } else {
            ADTS.HEADER_WITH_CRC_SIZE
        }

        return ADTS(
            protectionAbsent,
            profile,
            samplingFrequencyIndex.sampleRate,
            channelConfiguration,
            frameLength
        )
    }
}

/**
 * ADTS header
 *
 * @param protectionAbsent No CRC protection
 * @param profile the [AudioObjectType]
 * @param sampleRate the sampling frequency
 * @param channelConfiguration the channel configuration
 * @param frameLength the length of the payload
 */
data class ADTS(
    val protectionAbsent: Boolean, // No CRC protection
    val profile: AudioObjectType,
    val sampleRate: Int,
    val channelConfiguration: ChannelConfiguration,
    val frameLength: Int
) {
    companion object {
        internal const val HEADER_SIZE = 7
        internal const val HEADER_WITH_CRC_SIZE = 9
    }

    /**
     * Sampling frequency index
     */
    enum class SamplingFrequencyIndex(val value: Int, val sampleRate: Int) {
        F_96000HZ(0, 96000),
        F_88200HZ(1, 88200),
        F_64000HZ(2, 64000),
        F_48000HZ(3, 48000),
        F_44100HZ(4, 44100),
        F_32000HZ(5, 32000),
        F_24000HZ(6, 24000),
        F_22050HZ(7, 22050),
        F_16000HZ(8, 16000),
        F_12000HZ(9, 12000),
        F_11025HZ(10, 11025),
        F_8000HZ(11, 8000),
        F_7350HZ(12, 7350),
        EXPLICIT(15, -1);

        companion object {
            /**
             * Returns the audio object type from the value.
             *
             * @param value the value
             * @return the audio object type
             */
            fun entryOf(value: Int) = entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported SamplingFrequencyIndex: $value")
        }
    }

    /**
     * Channel configuration
     */
    enum class ChannelConfiguration(val value: Short, val numOfChannel: Int) {
        SPECIFIC(0, -1),
        CHANNEL_1(1, 1),
        CHANNEL_2(2, 2),
        CHANNEL_3(3, 3),
        CHANNEL_4(4, 4),
        CHANNEL_5(5, 5),
        CHANNEL_6(6, 6),
        CHANNEL_8(7, 8);

        companion object {
            /**
             * Returns the audio object type from the value.
             *
             * @param value the value
             * @return the audio object type
             */
            fun entryOf(value: Short) = entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unsupported ChannelConfiguration: $value")

            /**
             * Returns the audio object type from the number of channels.
             *
             * @param numOfChannel the number of channels
             * @return the audio object type
             */
            fun fromNumOfChannel(numOfChannel: Int) =
                entries.firstOrNull { it.numOfChannel == numOfChannel }
                    ?: throw IllegalArgumentException("Unsupported number of channels: $numOfChannel")
        }
    }
}
