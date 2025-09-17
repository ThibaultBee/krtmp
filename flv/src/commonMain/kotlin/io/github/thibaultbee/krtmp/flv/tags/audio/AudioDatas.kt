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

/**
 * Creates a legacy [AudioData] from its parameters.
 *
 * For AAC audio data, consider using [AACAudioDataFactory].
 *
 * @param soundFormat the [SoundFormat]
 * @param soundRate the [SoundRate]
 * @param soundSize the [SoundSize]
 * @param soundType the [SoundType]
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 * @return the [AudioData]
 */
fun AudioData(
    soundFormat: SoundFormat,
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    body: RawSource,
    bodySize: Int
) = LegacyAudioData(soundFormat, soundRate, soundSize, soundType, RawAudioTagBody(body, bodySize))

/**
 * Factory to create AAC [AudioData].
 */
class AACAudioDataFactory(
    val soundRate: SoundRate,
    val soundSize: SoundSize,
    val soundType: SoundType
) {
    /**
     * Creates a legacy AAC [AudioData] for sequence header from a [RawSource] and its size.
     *
     * @param audioSpecificConfig the AAC [AudioSpecificConfig] as a [RawSource]
     * @param audioSpecificConfigSize the size of the AAC [AudioSpecificConfig]
     * @return the [AudioData]
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

    /**
     * Creates a legacy AAC audio frame from a [RawSource] and its size.
     *
     * @param data the coded AAC [RawSource]
     * @param dataSize the size of the coded AAC [RawSource]
     * @return the [LegacyAudioData]
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
}

/**
 * Creates a legacy AAC [LegacyAudioData] for sequence header from the [AAC.ADTS].
 *
 * @param adts the [ADTS] header
 * @return the [LegacyAudioData]
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

// Extended audio data

/**
 * Factory to create single track [ExtendedAudioData].
 */
class ExtendedAudioDataFactory(
    val fourCC: AudioFourCC
) {
    /**
     * Creates an [ExtendedAudioData] from the given [RawSource] and its size.
     *
     * @param body the [RawSource] of the audio data
     * @param bodySize the size of the [RawSource]
     */
    fun codedFrame(
        body: RawSource,
        bodySize: Int
    ) = ExtendedAudioData(
        packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.CODED_FRAME,
            fourCC = fourCC,
            body = RawAudioTagBody(data = body, dataSize = bodySize)
        )
    )

    fun sequenceStart(
        body: RawSource,
        bodySize: Int
    ) = ExtendedAudioData(
        packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.SEQUENCE_START,
            fourCC = fourCC,
            body = RawAudioTagBody(data = body, dataSize = bodySize)
        )
    )

    fun sequenceEnd() = ExtendedAudioData(
        packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.SEQUENCE_END,
            fourCC = fourCC,
            body = EmptyAudioTagBody()
        )
    )

    fun multiChannelConfig(
        body: RawSource,
        bodySize: Int
    ) = ExtendedAudioData(
        packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.MULTICHANNEL_CONFIG,
            fourCC = fourCC,
            body = RawAudioTagBody(data = body, dataSize = bodySize)
        )
    )

    /**
     * Creates an [ExtendedAudioData] for unspecified multichannel config audio data.
     *
     * @param fourCC the FourCCs
     * @param channelCount the number of channels
     */
    fun multiChannelConfig(
        fourCC: AudioFourCC,
        channelCount: Byte
    ) = ExtendedAudioData(
        packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
            packetType = AudioPacketType.MULTICHANNEL_CONFIG,
            fourCC = fourCC,
            body = MultichannelConfigAudioTagBody.UnspecifiedMultichannelConfigAudioTagBody(
                channelCount = channelCount
            )
        )
    )
}

// Multi track audio data

/**
 * Creates a [MultitrackAudioTagBody] for one track audio data.
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 */
fun oneTrackMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    trackID: Byte,
    body: RawSource,
    bodySize: Int
) = ExtendedAudioData(
    packetDescriptor = ExtendedAudioData.MultitrackAudioDataDescriptor.OneTrackAudioDataDescriptor(
        fourCC = fourCC,
        framePacketType = framePacketType,
        body = OneTrackAudioTagBody(
            trackId = trackID, body = RawAudioTagBody(data = body, dataSize = bodySize)
        )
    )
)

/**
 * Creates a [MultitrackAudioTagBody] for a one codec multitrack audio data (either one or many tracks).
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackAudioTagBody]. If there is only one track in the set it is considered as a one track audio data.
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
        packetDescriptor = packetDescriptor
    )
}

/**
 * Creates a [MultitrackAudioTagBody] for a many codec and many track audio data.
 *
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackMultiCodecAudioTagBody]
 */
fun manyCodecMultitrackExtendedAudioData(
    framePacketType: AudioPacketType,
    tracks: Set<OneTrackMultiCodecAudioTagBody>
) = ExtendedAudioData(
    packetDescriptor = ExtendedAudioData.MultitrackAudioDataDescriptor.ManyTrackManyCodecAudioDataDescriptor(
        framePacketType = framePacketType,
        body = ManyTrackManyCodecAudioTagBody(tracks)
    )
)