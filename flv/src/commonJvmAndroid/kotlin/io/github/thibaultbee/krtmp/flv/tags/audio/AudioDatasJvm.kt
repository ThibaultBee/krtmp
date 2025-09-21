/*
 * Copyright (C) 2025 Thibault B.
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

import io.github.thibaultbee.krtmp.common.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.util.av.AudioSpecificConfig
import java.nio.ByteBuffer

/**
 * Extensions to create [AudioData] from [ByteBuffer].
 */

// Legacy audio data
/**
 * Creates a [LegacyAudioData] from a [ByteBuffer].
 *
 * For AAC audio data, consider using [AACAudioDataFactory].
 *
 * @param soundFormat the [SoundFormat]
 * @param soundRate the [SoundRate]
 * @param soundSize the [SoundSize]
 * @param soundType the [SoundType]
 * @param data the coded frame as a [ByteBuffer]
 * @return the [LegacyAudioData] with the frame
 */
fun AudioData(
    soundFormat: SoundFormat,
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    data: ByteBuffer
) = AudioData(
    soundFormat,
    soundRate,
    soundSize,
    soundType,
    ByteBufferBackedRawSource(data),
    data.remaining()
)


/**
 * Creates an AAC [LegacyAudioData] for sequence start from a [ByteBuffer].
 *
 * @param audioSpecificConfig the AAC [AudioSpecificConfig] as a [ByteBuffer]
 * @return the [LegacyAudioData] with the sequence start
 */
fun AACAudioDataFactory.sequenceStart(
    audioSpecificConfig: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(audioSpecificConfig),
    audioSpecificConfig.remaining()
)


/**
 * Creates an AAC [LegacyAudioData] for coded frame from a [ByteBuffer].
 *
 * @param data the coded frame as a [ByteBuffer]
 * @return the [LegacyAudioData] with the frame
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
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedAudioData] with the frame
 */
fun ExtendedAudioDataFactory.codedFrame(
    data: ByteBuffer
) = codedFrame(
    ByteBufferBackedRawSource(data),
    data.remaining()
)

/**
 * Creates an [ExtendedAudioData] for sequence start from a [ByteBuffer].
 *
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedAudioData] with the sequence start
 */
fun ExtendedAudioDataFactory.sequenceStart(
    data: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(data),
    data.remaining()
)

/**
 * Creates an [ExtendedAudioData] for multichannel config audio data from a [ByteBuffer].
 *
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedAudioData] with the multichannel config
 */
fun ExtendedAudioDataFactory.multiChannelConfig(
    data: ByteBuffer
) = multiChannelConfig(
    ByteBufferBackedRawSource(data),
    data.remaining()
)

// Multi track audio data

/**
 * Creates a [ExtendedAudioData] for one track audio data from a [ByteBuffer].
 *
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedAudioData] with the one track audio data
 */
fun oneTrackMultitrackExtendedAudioData(
    fourCC: AudioFourCC,
    framePacketType: AudioPacketType,
    trackID: Byte,
    data: ByteBuffer
) = oneTrackMultitrackExtendedAudioData(
    fourCC,
    framePacketType,
    trackID,
    ByteBufferBackedRawSource(data),
    data.remaining()
)
