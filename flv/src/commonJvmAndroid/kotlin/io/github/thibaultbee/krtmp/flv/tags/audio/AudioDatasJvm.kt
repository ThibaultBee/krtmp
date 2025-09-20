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
import io.github.thibaultbee.krtmp.flv.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.flv.tags.video.ExtendedVideoData
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.flv.util.av.AudioSpecificConfig
import java.nio.ByteBuffer

/**
 * Extensions to create [VideoData] and [ExtendedVideoData] from [ByteBuffer]s.
 */

// Legacy audio data
/**
 * Creates a legacy [AudioData] from its parameters and a [ByteBuffer].
 *
 * For AAC audio data, consider using [AACAudioDataFactory].
 *
 * @param soundFormat the [SoundFormat]
 * @param soundRate the [SoundRate]
 * @param soundSize the [SoundSize]
 * @param soundType the [SoundType]
 * @param body the coded [ByteBuffer]
 * @return the [AudioData]
 */
fun AudioData(
    soundFormat: SoundFormat,
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    body: ByteBuffer
) = AudioData(
    soundFormat,
    soundRate,
    soundSize,
    soundType,
    ByteBufferBackedRawSource(body),
    body.remaining()
)


/**
 * Creates a legacy AAC [AudioData] for sequence header from a [ByteBuffer].
 *
 * @param audioSpecificConfig the AAC [AudioSpecificConfig] as a [ByteBuffer]
 * @return the [AudioData]
 */
fun AACAudioDataFactory.sequenceStart(
    audioSpecificConfig: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(audioSpecificConfig),
    audioSpecificConfig.remaining()
)


/**
 * Creates a legacy AAC audio frame from a [ByteBuffer].
 *
 * @param data the coded AAC [ByteBuffer]
 * @return the [LegacyAudioData]
 */
fun AACAudioDataFactory.codedFrame(
    data: ByteBuffer
) = codedFrame(
    ByteBufferBackedRawSource(data),
    data.remaining()
)

// Extended audio data

/**
 * Creates an [ExtendedAudioData] for coded frame from a [ByteBuffer].
 *
 * @param body the [ByteBuffer] of the audio data
 */
fun ExtendedAudioDataFactory.codedFrame(
    body: ByteBuffer
) = codedFrame(
    ByteBufferBackedRawSource(body),
    body.remaining()
)

/**
 * Creates an [ExtendedAudioData] for sequence start from a [ByteBuffer].
 *
 * @param body the [ByteBuffer] of the audio data
 */
fun ExtendedAudioDataFactory.sequenceStart(
    body: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(body),
    body.remaining()
)

/**
 * Creates an [ExtendedAudioData] for multichannel config audio data from a [ByteBuffer].
 *
 * @param body the [ByteBuffer] of the audio data
 */
fun ExtendedAudioDataFactory.multiChannelConfig(
    body: ByteBuffer
) = multiChannelConfig(
    ByteBufferBackedRawSource(body),
    body.remaining()
)

// Multi track audio data

/**
 * Creates a [MultitrackAudioTagBody] for one track audio data from a [ByteBuffer].
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param body the coded [ByteBuffer]
 */
fun oneTrackMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    trackID: Byte,
    body: ByteBuffer
) = oneTrackMultitrackExtendedAudioData(
    fourCC,
    framePacketType,
    trackID,
    ByteBufferBackedRawSource(body),
    body.remaining()
)
