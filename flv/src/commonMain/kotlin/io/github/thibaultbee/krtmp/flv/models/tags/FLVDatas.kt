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
package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.flv.models.av.util.AudioSpecificConfig
import io.github.thibaultbee.krtmp.flv.models.av.util.aac.AAC
import io.github.thibaultbee.krtmp.flv.models.av.util.aac.ADTS
import io.github.thibaultbee.krtmp.flv.models.av.util.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.av.util.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.av.util.readBuffer
import io.github.thibaultbee.krtmp.flv.models.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.config.FourCCs
import io.github.thibaultbee.krtmp.flv.models.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.models.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.config.SoundType
import io.github.thibaultbee.krtmp.flv.models.sources.NaluRawSource
import io.github.thibaultbee.krtmp.flv.models.tags.AudioData
import io.github.thibaultbee.krtmp.flv.models.tags.VideoData
import kotlinx.io.RawSource

/**
 * Factories to create [VideoData] and [AudioData].
 */

/**
 * Creates a [VideoData] from a [RawSource] and its size.
 *
 * For AVC/H.264, use [avcVideoData], [avcHeaderVideoData] or [avcEndOfSequence] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 * @return the [VideoData] with the frame
 */
fun VideoData(
    frameType: FrameType,
    codecID: CodecID,
    body: RawSource,
    bodySize: Int,
) = LegacyVideoData(
    frameType = frameType,
    codecID = codecID,
    packetType = AVCPacketType.NALU, // Unused
    compositionTime = 0, // Unused
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an AVC/H.264 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [VideoData] with the AVC frame
 */
fun avcVideoData(
    frameType: FrameType,
    packetType: AVCPacketType,
    body: RawSource,
    bodySize: Int,
    compositionTime: Int = 0
) = LegacyVideoData(
    frameType = frameType,
    codecID = CodecID.AVC,
    packetType = packetType,
    compositionTime = compositionTime,
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an AVC/H.264 [VideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the NAL unit [RawSource].
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [VideoData] with the AVC frame
 */
fun avcVideoData(
    frameType: FrameType, packetType: AVCPacketType, body: NaluRawSource, compositionTime: Int = 0
) = avcVideoData(
    frameType = frameType,
    packetType = packetType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates an AVC/H.264 [VideoData] for SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 * If you want to directly pass [AVCDecoderConfigurationRecord], use [avcVideoData] instead.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [VideoData] with the SPS and PPS
 */
fun avcHeaderVideoData(
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps = sps, pps = pps
    ).readBuffer()

    avcVideoData(
        frameType = FrameType.KEY,
        packetType = AVCPacketType.SEQUENCE_HEADER,
        compositionTime = 0,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates an AVC/H.264 [VideoData] for the end of sequence.
 */
fun avcEndOfSequence() = LegacyVideoData(
    frameType = FrameType.KEY,
    codecID = CodecID.AVC,
    packetType = AVCPacketType.END_OF_SEQUENCE,
    compositionTime = 0,
    body = EmptyVideoTagBody()
)

/**
 * Creates an HEVC/H.265 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded HEVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded HEVC [RawSource]
 * @param compositionTime the composition time (24 bits)
 * @return the [VideoData] with the HEVC frame
 */
fun hevcVideoData(
    frameType: FrameType,
    packetType: PacketType,
    body: RawSource,
    bodySize: Int,
    compositionTime: Int = 0
) = ExtendedVideoData(
    frameType = frameType,
    packetType = packetType,
    fourCC = FourCCs.HEVC,
    body = HEVCVideoTagBody(
        packetType = packetType,
        compositionTime = compositionTime,
        data = body,
        dataSize = bodySize
    )
)

/**
 * Creates an HEVC/H.265 [VideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the NAL unit [RawSource].
 * @param compositionTime the composition time (24 bits)
 * @return the [VideoData] with the HEVC frame
 */
fun hevcVideoData(
    frameType: FrameType, packetType: PacketType, body: NaluRawSource, compositionTime: Int = 0
) = hevcVideoData(
    frameType = frameType,
    packetType = packetType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates an AVC/H.264 [VideoData] for VPS, SPS and PPS.
 *
 * This method will create a [HEVCDecoderConfigurationRecord] from the VPS, SPS and PPS NAL units.
 * If you want to directly pass [HEVCDecoderConfigurationRecord], use [hevcVideoData] instead.
 *
 * @param vps the VPS NAL units
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [VideoData] with the SPS and PPS
 */
fun hevcHeaderVideoData(
    vps: List<NaluRawSource>,
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) {
    val decoderConfigurationRecord = HEVCDecoderConfigurationRecord(
        vps = vps, sps = sps, pps = pps
    ).readBuffer()

    hevcVideoData(
        frameType = FrameType.KEY,
        packetType = PacketType.SEQUENCE_START,
        compositionTime = 0,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates a VP9 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded VP9 [RawSource]
 * @param bodySize the size of the coded VP9 [RawSource]
 * @return the [VideoData] with the VP9 frame
 */
fun vp9VideoData(
    frameType: FrameType,
    packetType: PacketType,
    body: RawSource,
    bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    packetType = packetType,
    fourCC = FourCCs.VP9,
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an AV1 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded VP9 [RawSource]
 * @param bodySize the size of the coded VP9 [RawSource]
 * @return the [VideoData] with the AV1 frame
 */
fun av1VideoData(
    frameType: FrameType,
    packetType: PacketType,
    body: RawSource,
    bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    packetType = packetType,
    fourCC = FourCCs.AV1,
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an [ExtendedVideoData] for the end of sequence.
 *
 * @param fourCC the FourCCs
 */
fun endOfSequence(
    fourCC: FourCCs
) = ExtendedVideoData(
    FrameType.KEY, PacketType.SEQUENCE_END, fourCC,
    EmptyVideoTagBody()
)

/**
 * Creates an AAC [AudioData] from the [AAC.ADTS].
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
 * Creates an AAC audio frame from a [RawSource] and its size.
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