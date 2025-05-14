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
package io.github.thibaultbee.krtmp.flv.config

class FLVAudioConfig(
    override val mediaType: AudioMediaType,
    override val bitrateBps: Int,
    val soundRate: SoundRate,
    val soundSize: SoundSize,
    val soundType: SoundType,
) : FLVConfig<AudioMediaType> {
    val soundFormat = mediaType.soundFormat
    val fourCC = mediaType.fourCCs
    val metadataType = soundFormat?.value?.toInt() ?: fourCC!!.value.code
}

enum class SoundFormat(val value: Byte) {
    PCM(0),
    ADPCM(1),
    MP3(2),
    PCM_LE(3),
    NELLYMOSER_16KHZ(4),
    NELLYMOSER_8KHZ(5),
    NELLYMOSER(6),
    G711_ALAW(7),
    G711_MLAW(8),
    EX_HEADER(9),
    AAC(10),
    SPEEX(11),
    MP3_8K(14),
    DEVICE_SPECIFIC(15);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid SoundFormat value: $value"
            )
    }
}


/**
 * FourCC object
 *
 * Only enhanced RTMP FourCC exists.
 */
enum class AudioFourCC(val value: FourCC) {
    AC3(
        FourCC(
            'a', 'c', '-', '3'
        )
    ),
    EAC3(FourCC('e', 'c', '-', '3')),
    OPUS(
        FourCC(
            'O', 'p', 'u', 's'
        )
    ),
    MP3(
        FourCC(
            '.', 'm', 'p', '3'
        )
    ),
    FLAC(FourCC('f', 'L', 'a', 'C')),
    AAC(
        FourCC(
            'm', 'p', '4', 'a'
        )
    );

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun codeOf(value: Int) = entries.firstOrNull { it.value.code == value }
            ?: throw IllegalArgumentException("Invalid audio FourCC code: ${value.toHexString()}")
    }
}


/**
 * A meta class for [SoundFormat] and [AudioFourCC].
 */
enum class AudioMediaType(val soundFormat: SoundFormat?, val fourCCs: AudioFourCC?) {
    PCM(SoundFormat.PCM, null),
    ADPCM(SoundFormat.ADPCM, null),
    MP3(SoundFormat.MP3, AudioFourCC.MP3),
    PCM_LE(SoundFormat.PCM_LE, null),
    NELLYMOSER_16KHZ(SoundFormat.NELLYMOSER_16KHZ, null),
    NELLYMOSER_8KHZ(SoundFormat.NELLYMOSER_8KHZ, null),
    NELLYMOSER(SoundFormat.NELLYMOSER, null),
    G711_ALAW(SoundFormat.G711_ALAW, AudioFourCC.AC3),
    G711_MLAW(SoundFormat.G711_MLAW, AudioFourCC.EAC3),
    AAC(SoundFormat.AAC, AudioFourCC.AAC),
    SPEEX(SoundFormat.SPEEX, null),
    MP3_8K(SoundFormat.MP3_8K, null),
    DEVICE_SPECIFIC(SoundFormat.DEVICE_SPECIFIC, null),
    AC3(null, AudioFourCC.AC3),
    EAC3(null, AudioFourCC.EAC3),
    OPUS(null, AudioFourCC.OPUS),
    FLAC(null, AudioFourCC.FLAC);


    companion object {
        fun fromFourCC(fourCC: AudioFourCC) =
            entries.firstOrNull { it.fourCCs == fourCC } ?: throw IllegalArgumentException(
                "Invalid audio FourCC: $fourCC"
            )

        fun fromSoundFormat(soundFormat: SoundFormat) =
            entries.firstOrNull { it.soundFormat == soundFormat }
                ?: throw IllegalArgumentException("Invalid SoundFormat: $soundFormat")
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
