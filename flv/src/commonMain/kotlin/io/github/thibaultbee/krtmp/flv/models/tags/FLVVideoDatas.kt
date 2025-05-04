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

import io.github.thibaultbee.krtmp.flv.models.av.util.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.av.util.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.av.util.readBuffer
import io.github.thibaultbee.krtmp.flv.models.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.config.FourCCs
import io.github.thibaultbee.krtmp.flv.models.sources.NaluRawSource
import kotlinx.io.RawSource

/**
 * Factories to create [VideoData] and [AudioData].
 */

// Legacy video data

/**
 * Creates a legacy video command data from a [VideoCommand].
 *
 * @param codecID the codec ID
 * @param command the video command
 */
fun CommandVideoData(
    codecID: CodecID, command: VideoCommand
) = LegacyVideoData(
    frameType = VideoFrameType.COMMAND, codecID = codecID, body = CommandVideoTagBody(command)
)

/**
 * Creates a legacy [VideoData] from a [RawSource] and its size.
 *
 * For AVC/H.264, use [avcVideoData], [avcHeaderVideoData] or [avcEndOfSequenceVideoData] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 * @return the [VideoData] with the frame
 */
fun VideoData(
    frameType: VideoFrameType,
    codecID: CodecID,
    body: RawSource,
    bodySize: Int,
) = LegacyVideoData(
    frameType = frameType, codecID = codecID, packetType = AVCPacketType.NALU, // Unused
    compositionTime = 0, // Unused
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates a legacy AVC/H.264 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [VideoData] with the AVC frame
 */
fun avcVideoData(
    frameType: VideoFrameType,
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
 * Creates a legacy AVC/H.264 [VideoData] from a [NaluRawSource].
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
    frameType: VideoFrameType,
    packetType: AVCPacketType,
    body: NaluRawSource,
    compositionTime: Int = 0
) = avcVideoData(
    frameType = frameType,
    packetType = packetType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates a legacy AVC/H.264 [VideoData] for SPS and PPS.
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
        frameType = VideoFrameType.KEY,
        packetType = AVCPacketType.SEQUENCE_HEADER,
        compositionTime = 0,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates a legacy AVC/H.264 [VideoData] for the end of sequence.
 */
fun avcEndOfSequenceVideoData() = LegacyVideoData(
    frameType = VideoFrameType.KEY,
    codecID = CodecID.AVC,
    packetType = AVCPacketType.END_OF_SEQUENCE,
    compositionTime = 0,
    body = EmptyVideoTagBody()
)

// Extended video data
/**
 * Creates an extended coded frame AVC/H.264 [VideoData] from a [RawSource] and its size.
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @return the [VideoData] with the AVC frame
 */
fun avcExtendedCodedFrameVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    fourCC = FourCCs.AVC,
    packetType = VideoPacketType.CODED_FRAMES,
    body = AVCHEVCCodedFrameVideoTagBody(
        compositionTime = compositionTime, data = body, dataSize = bodySize
    )
)

/**
 * Creates an extended coded frame AVC/H.264 [VideoData] from a [NaluRawSource].
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the NAL unit [RawSource].
 * @return the [VideoData] with the AVC frame
 */
fun avcExtendedCodedFrameVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: NaluRawSource
) = avcExtendedCodedFrameVideoData(
    frameType = frameType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates an extended AVC/H.264 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @return the [VideoData] with the AVC frame
 */
fun avcExtendedVideoData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    fourCC = FourCCs.AVC,
    packetType = packetType,
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an extended AVC/H.264 [VideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the NAL unit [RawSource].
 * @return the [VideoData] with the AVC frame
 */
fun avcExtendedVideoData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: NaluRawSource
) = avcExtendedVideoData(
    frameType = frameType, packetType = packetType, body = body.nalu, bodySize = body.naluSize
)

/**
 * Creates an extended AVC/H.264 [VideoData] for SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 * If you want to directly pass [AVCDecoderConfigurationRecord], use [avcVideoData] instead.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [VideoData] with the SPS and PPS
 */
fun avcHeaderExtendedVideoData(
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps = sps, pps = pps
    ).readBuffer()

    avcExtendedVideoData(
        frameType = VideoFrameType.KEY,
        packetType = VideoPacketType.SEQUENCE_START,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}

/**
 * Creates an HEVC/H.265 coded frame [VideoData] from a [RawSource] and its size.
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the coded HEVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded HEVC [RawSource]
 * @return the [VideoData] with the HEVC frame
 */
fun hevcCodedFrameVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    packetType = VideoPacketType.CODED_FRAMES,
    fourCC = FourCCs.HEVC,
    body = AVCHEVCCodedFrameVideoTagBody(
        compositionTime = compositionTime, data = body, dataSize = bodySize
    )
)


/**
 * Creates an HEVC/H.265 [VideoData] from a [NaluRawSource].
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the NAL unit [RawSource].
 * @return the [VideoData] with the HEVC frame
 */
fun hevcCodedFrameVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: NaluRawSource
) = hevcCodedFrameVideoData(
    frameType = frameType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize,
)

/**
 * Creates an HEVC/H.265 [VideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the coded HEVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded HEVC [RawSource]
 * @return the [VideoData] with the HEVC frame
 */
fun hevcVideoData(
    frameType: VideoFrameType,
    packetType: VideoPacketType,
    body: RawSource,
    bodySize: Int,
) = ExtendedVideoData(
    frameType = frameType,
    packetType = packetType,
    fourCC = FourCCs.HEVC,
    body = DefaultVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an HEVC/H.265 [VideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the NAL unit [RawSource].
 * @return the [VideoData] with the HEVC frame
 */
fun hevcVideoData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: NaluRawSource
) = hevcVideoData(
    frameType = frameType, packetType = packetType, body = body.nalu, bodySize = body.naluSize
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
        frameType = VideoFrameType.KEY,
        packetType = VideoPacketType.SEQUENCE_START,
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
    frameType: VideoFrameType, packetType: VideoPacketType, body: RawSource, bodySize: Int
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
    frameType: VideoFrameType, packetType: VideoPacketType, body: RawSource, bodySize: Int
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
fun endOfSequenceVideoData(
    fourCC: FourCCs
) = ExtendedVideoData(
    VideoFrameType.KEY, VideoPacketType.SEQUENCE_END, fourCC, EmptyVideoTagBody()
)
