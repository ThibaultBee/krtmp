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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.util.av.AudioSpecificConfig
import io.github.thibaultbee.krtmp.flv.util.av.aac.AAC
import io.github.thibaultbee.krtmp.flv.util.av.aac.ADTS
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import kotlinx.io.RawSource

/**
 * Creates a legacy AAC [AudioData] from the [AAC.ADTS].
 *
 * @param adts the [ADTS] header
 * @return the [AudioData] with the [ADTS] header
 */
fun aacHeaderAudioData(adts: ADTS): AudioData {
    val audioSpecificConfig = AudioSpecificConfig(adts).readBuffer()

    return AudioData(
        soundFormat = SoundFormat.AAC,
        soundRate = SoundRate.fromSampleRate(adts.sampleRate),
        soundSize = SoundSize.S_16BITS,
        soundType = SoundType.fromNumOfChannels(adts.channelConfiguration.numOfChannel),
        aacPacketType = AACPacketType.SEQUENCE_HEADER,
        body = DefaultAudioTagBody(
            data = audioSpecificConfig,
            dataSize = audioSpecificConfig.size.toInt()
        )
    )
}

/**
 * Creates a legacy AAC audio frame from a [RawSource] and its size.
 *
 * @param soundRate the sound rate
 * @param soundSize the sound size
 * @param soundType the sound type
 * @param aacPacketType the AAC packet type
 * @param data the coded AAC [RawSource]
 * @param dataSize the size of the coded AAC [RawSource]
 * @return the [AudioData] with the AAC frame
 */
fun aacAudioData(
    soundRate: SoundRate,
    soundSize: SoundSize,
    soundType: SoundType,
    aacPacketType: AACPacketType,
    data: RawSource,
    dataSize: Int
) = AudioData(
    soundFormat = SoundFormat.AAC,
    soundRate = soundRate,
    soundSize = soundSize,
    soundType = soundType,
    aacPacketType = aacPacketType,
    body = DefaultAudioTagBody(
        data = data, dataSize = dataSize
    )
)