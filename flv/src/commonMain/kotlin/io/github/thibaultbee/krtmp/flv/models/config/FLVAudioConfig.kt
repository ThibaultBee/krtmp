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
package io.github.thibaultbee.krtmp.flv.models.config

class FLVAudioConfig(
    override val mediaType: MediaType,
    override val bitrateBps: Int,
    val soundRate: SoundRate,
    val soundSize: SoundSize,
    val soundType: SoundType,
) : FLVConfig {
    val soundFormat = SoundFormat.fromMediaType(mediaType)

    init {
        require(mediaType.type == MediaType.Type.AUDIO) { "MimeType must be an audio type" }
    }
}

enum class SoundFormat(val value: Byte, val mediaType: MediaType? = null) {
    PCM(0, MediaType.AUDIO_RAW),
    ADPCM(1),
    MP3(2),
    PCM_LE(3),
    NELLYMOSER_16KHZ(4),
    NELLYMOSER_8KHZ(5),
    NELLYMOSER(6),
    G711_ALAW(7, MediaType.AUDIO_G711A),
    G711_MLAW(8, MediaType.AUDIO_G711U),
    AAC(10, MediaType.AUDIO_AAC),
    SPEEX(11),
    MP3_8K(14),
    DEVICE_SPECIFIC(15);

    companion object {
        fun fromMediaType(mediaType: MediaType) = entries.firstOrNull { it.mediaType == mediaType }
            ?: throw IllegalArgumentException("Invalid MediaType: $mediaType")

        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid SoundFormat value: $value"
            )
    }
}

enum class SoundRate(val value: Byte, val sampleRate: Int) {
    F_5500HZ(0, 5500),
    F_11025HZ(1, 11025),
    F_22050HZ(2, 22050),
    F_44100HZ(3, 44100);

    companion object {
        fun fromSampleRate(sampleRate: Int) = entries.firstOrNull { it.sampleRate == sampleRate }
            ?: throw IllegalArgumentException("Invalid sample rate: $sampleRate")

        fun isSupported(sampleRate: Int) = entries.any { it.sampleRate == sampleRate }

        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid SoundRate value: $value"
            )
    }
}

enum class SoundSize(val value: Byte, val byteFormat: AudioByteFormat) {
    S_8BITS(0, AudioByteFormat.S_8),
    S_16BITS(1, AudioByteFormat.S_16);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid SoundSize value: $value"
            )
    }
}

enum class SoundType(val value: Byte, val numOfChannels: Int) {
    MONO(0, 1),
    STEREO(1, 2);

    companion object {
        fun fromNumOfChannels(numOfChannels: Int) =
            entries.firstOrNull { it.numOfChannels == numOfChannels }
                ?: throw IllegalArgumentException(
                    "Invalid number of channels: $numOfChannels"
                )

        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid SoundType value: $value"
            )
    }
}
