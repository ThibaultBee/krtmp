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
package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.tags.audio.ManyTrackManyCodecAudioTagBody.OneTrackMultiCodecAudioTagBody
import io.github.thibaultbee.krtmp.flv.util.av.AudioSpecificConfig
import io.github.thibaultbee.krtmp.flv.util.av.aac.AAC
import io.github.thibaultbee.krtmp.flv.util.av.aac.ADTS
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import kotlinx.io.RawSource

/**
 * Factories to create [AudioData].
 */

// Legacy audio data

/**
 * Creates a [LegacyAudioData] from a [RawSource].
 *
 * For AAC audio data, consider using [AACAudioDataFactory].
 *
 * @param soundFormat the [SoundFormat]
 * @param soundRate the [SoundRate]
 * @param soundSize the [SoundSize]
 * @param soundType the [SoundType]
 * @param data the coded frame as a [RawSource]
 * @param dataSize the size of the [data]
 * @return the [LegacyAudioData] with the frame
 */
fun AudioData(
    soundFormat: SoundFormat,
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    data: RawSource,
    dataSize: Int
) = LegacyAudioData(soundFormat, soundRate, soundSize, soundType, RawAudioTagBody(data, dataSize))


/**
 * Creates a [LegacyAudioData] from a [ByteArray].
 *
 * For AAC audio data, consider using [AACAudioDataFactory].
 *
 * @param soundFormat the [SoundFormat]
 * @param soundRate the [SoundRate]
 * @param soundSize the [SoundSize]
 * @param soundType the [SoundType]
 * @param data the coded frame as a [ByteArray]
 * @return the [LegacyAudioData] with the frame
 */
fun AudioData(
    soundFormat: SoundFormat,
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    data: ByteArray
) = AudioData(
    soundFormat,
    soundRate,
    soundSize,
    soundType,
    ByteArrayBackedRawSource(data),
    data.size
)

/**
 * Factory to create AAC [AudioData].
 *
 * The [soundRate] and [soundType] should match the FLV AAC configuration but as the decoder ignores them,
 * and extracts the correct values from the [AudioSpecificConfig].
 *
 * @param soundSize the [SoundSize]
 * @param soundRate the [SoundRate]. According to the FLV specification, it should be 44.1kHz.
 * @param soundType the [SoundType]. According to the FLV specification, it should be stereo.
 */
class AACAudioDataFactory(
    val soundSize: SoundSize,
    val soundRate: SoundRate = SoundRate.F_44100HZ,
    val soundType: SoundType = SoundType.STEREO
) {
    /**
     * Creates an AAC [LegacyAudioData] for coded frame from a [RawSource].
     *
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [LegacyAudioData] with the frame
     */
    fun codedFrame(
        data: RawSource,
        dataSize: Int
    ) = LegacyAudioData(
        soundFormat = SoundFormat.AAC,
        soundRate = soundRate,
        soundSize = soundSize,
        soundType = soundType,
        aacPacketType = AACPacketType.RAW,
        body = RawAudioTagBody(
            data = data, dataSize = dataSize
        )
    )

    /**
     * Creates an AAC [LegacyAudioData] for sequence header from a [RawSource].
     *
     * @param audioSpecificConfig the AAC [AudioSpecificConfig] as a [RawSource]
     * @param audioSpecificConfigSize the size of the [audioSpecificConfig]
     * @return the [LegacyAudioData] with the sequence start
     */
    fun sequenceStart(
        audioSpecificConfig: RawSource,
        audioSpecificConfigSize: Int
    ): LegacyAudioData {
        return LegacyAudioData(
            soundFormat = SoundFormat.AAC,
            soundRate = soundRate,
            soundSize = soundSize,
            soundType = soundType,
            aacPacketType = AACPacketType.SEQUENCE_HEADER,
            body = RawAudioTagBody(
                data = audioSpecificConfig,
                dataSize = audioSpecificConfigSize
            )
        )
    }
}


/**
 * Creates an AAC [LegacyAudioData] for coded frame from a [ByteArray].
 *
 * @param data the coded frame as a [ByteArray]
 * @return the [LegacyAudioData] with the frame
 */
fun AACAudioDataFactory.codedFrame(
    data: ByteArray
) = codedFrame(
    ByteArrayBackedRawSource(data),
    data.size
)


/**
 * Creates an AAC [LegacyAudioData] for sequence start from the [AAC.ADTS].
 *
 * @param adts the [ADTS] header
 * @return the [LegacyAudioData] with the sequence start
 */
fun AACAudioDataFactory.sequenceStart(adts: ADTS): LegacyAudioData {
    require(SoundType.fromNumOfChannels(adts.channelConfiguration.numOfChannel) == soundType) {
        "ADTS channel configuration (${adts.channelConfiguration.numOfChannel} channels) does not match the factory sound type ($soundType)"
    }
    require(SoundRate.fromSampleRate(adts.sampleRate) == soundRate) {
        "ADTS sample rate (${adts.sampleRate} Hz) does not match the factory sound rate ($soundRate)"
    }
    val audioSpecificConfig = AudioSpecificConfig(adts).readBuffer()

    return sequenceStart(
        audioSpecificConfig,
        audioSpecificConfig.size.toInt()
    )
}


/**
 * Creates an AAC [LegacyAudioData] for sequence start from a [ByteArray].
 *
 * @param audioSpecificConfig the AAC [AudioSpecificConfig] as a [ByteArray]
 * @return the [AudioData] with the sequence start
 */
fun AACAudioDataFactory.sequenceStart(
    audioSpecificConfig: ByteArray
) = sequenceStart(
    ByteArrayBackedRawSource(audioSpecificConfig),
    audioSpecificConfig.size
)

// Extended audio data

/**
 * Factory to create single track [ExtendedAudioData].
 */
class ExtendedAudioDataFactory(
    val fourCC: AudioFourCC
) {
    /**
     * Creates an [ExtendedAudioData] for coded frame from a [RawSource].
     *
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedAudioData] with the frame
     */
    fun codedFrame(
        data: RawSource,
        dataSize: Int
    ) = ExtendedAudioData(
        dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.CODED_FRAME,
            fourCC = fourCC,
            body = RawAudioTagBody(data = data, dataSize = dataSize)
        )
    )

    /**
     * Creates an [ExtendedAudioData] for sequence start from a [RawSource].
     *
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedAudioData] with the sequence start
     */
    fun sequenceStart(
        data: RawSource,
        dataSize: Int
    ) = ExtendedAudioData(
        dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.SEQUENCE_START,
            fourCC = fourCC,
            body = RawAudioTagBody(data = data, dataSize = dataSize)
        )
    )

    /**
     * Creates an [ExtendedAudioData] for sequence end.
     *
     * @return the [ExtendedAudioData] with the sequence end
     */
    fun sequenceEnd() = ExtendedAudioData(
        dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.SEQUENCE_END,
            fourCC = fourCC,
            body = EmptyAudioTagBody()
        )
    )

    /**
     * Creates an [ExtendedAudioData] for multichannel config audio data from a [RawSource].
     *
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedAudioData] with the multichannel config
     */
    fun multiChannelConfig(
        data: RawSource,
        dataSize: Int
    ) = ExtendedAudioData(
        dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.MULTICHANNEL_CONFIG,
            fourCC = fourCC,
            body = RawAudioTagBody(data = data, dataSize = dataSize)
        )
    )

    /**
     * Creates an [ExtendedAudioData] for unspecified multichannel config audio data.
     *
     * @param fourCC the FourCCs
     * @param channelCount the number of channels
     * @return the [ExtendedAudioData] with the multichannel config
     */
    fun multiChannelConfig(
        fourCC: AudioFourCC,
        channelCount: Byte
    ) = ExtendedAudioData(
        dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.MULTICHANNEL_CONFIG,
            fourCC = fourCC,
            body = MultichannelConfigAudioTagBody.UnspecifiedMultichannelConfigAudioTagBody(
                channelCount = channelCount
            )
        )
    )
}


/**
 * Creates an [ExtendedAudioData] for coded frame from a [ByteArray].
 *
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedAudioData] with the frame
 */
fun ExtendedAudioDataFactory.codedFrame(
    data: ByteArray
) = ExtendedAudioData(
    dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
        packetType = AudioPacketType.CODED_FRAME,
        fourCC = fourCC,
        body = RawAudioTagBody(data = ByteArrayBackedRawSource(data), dataSize = data.size)
    )
)


/**
 * Creates an [ExtendedAudioData] for sequence start from a [ByteArray].
 *
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedAudioData] with the sequence start
 */
fun ExtendedAudioDataFactory.sequenceStart(
    data: ByteArray
) = ExtendedAudioData(
    dataDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
        packetType = AudioPacketType.SEQUENCE_START,
        fourCC = fourCC,
        body = RawAudioTagBody(data = ByteArrayBackedRawSource(data), dataSize = data.size)
    )
)

// Multi track audio data

/**
 * Creates a [ExtendedAudioData] for one track audio data from a [RawSource].
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [RawSource]
 * @param dataSize the size of the [data]
 * @return the [ExtendedAudioData] with the one track audio data
 */
fun oneTrackMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    trackID: Byte,
    data: RawSource,
    dataSize: Int
) = ExtendedAudioData(
    dataDescriptor = ExtendedAudioData.MultitrackAudioDataDescriptor.OneTrackAudioDataDescriptor(
        fourCC = fourCC,
        framePacketType = framePacketType,
        body = OneTrackAudioTagBody(
            trackId = trackID, body = RawAudioTagBody(data = data, dataSize = dataSize)
        )
    )
)


/**
 * Creates a [ExtendedAudioData] for one track audio data from a [ByteArray].
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedAudioData] with the one track audio data
 */
fun oneTrackMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    trackID: Byte,
    data: ByteArray
) = oneTrackMultitrackExtendedAudioData(
    fourCC,
    framePacketType,
    trackID,
    ByteArrayBackedRawSource(data),
    data.size
)


/**
 * Creates a [ExtendedAudioData] for one track audio data.
 *
 * @param trackID the track ID
 * @param dataDescriptor the [ExtendedAudioData.SingleAudioDataDescriptor] containing the audio data
 * @return the [ExtendedAudioData] with the one track audio data
 */
fun oneTrackMultitrackExtendedAudioData(
    trackID: Byte,
    dataDescriptor: ExtendedAudioData.SingleAudioDataDescriptor
) = ExtendedAudioData(
    dataDescriptor = ExtendedAudioData.MultitrackAudioDataDescriptor.OneTrackAudioDataDescriptor(
        fourCC = dataDescriptor.fourCC,
        framePacketType = dataDescriptor.packetType,
        body = OneTrackAudioTagBody(
            trackId = trackID, body = dataDescriptor.body
        )
    )
)


/**
 * Creates a [ExtendedAudioData] for a one codec multitrack audio data (either one or many tracks).
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackAudioTagBody]. If there is only one track in the set it is considered as a one track audio data.
 * @return the [ExtendedAudioData] with the one codec multitrack audio data
 */
fun oneCodecMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    tracks: Set<OneTrackAudioTagBody>
): ExtendedAudioData {
    val packetDescriptor = if (tracks.size == 1) {
        ExtendedAudioData.MultitrackAudioDataDescriptor.OneTrackAudioDataDescriptor(
            fourCC = fourCC,
            framePacketType = framePacketType,
            body = tracks.first()
        )
    } else if (tracks.size > 1) {
        ExtendedAudioData.MultitrackAudioDataDescriptor.ManyTrackAudioDataDescriptor(
            fourCC = fourCC,
            framePacketType = framePacketType,
            body = ManyTrackOneCodecAudioTagBody(tracks)
        )
    } else {
        throw IllegalArgumentException("No track in the set")
    }

    return ExtendedAudioData(
        dataDescriptor = packetDescriptor
    )
}


/**
 * Creates a [ExtendedAudioData] for a many codec and many track audio data.
 *
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackMultiCodecAudioTagBody]
 * @return the [ExtendedAudioData] with the many codec and many track audio data
 */
fun manyCodecMultitrackExtendedAudioData(
    framePacketType: AudioPacketType,
    tracks: Set<OneTrackMultiCodecAudioTagBody>
) = ExtendedAudioData(
    dataDescriptor = ExtendedAudioData.MultitrackAudioDataDescriptor.ManyTrackManyCodecAudioDataDescriptor(
        framePacketType = framePacketType,
        body = ManyTrackManyCodecAudioTagBody(tracks)
    )
)
