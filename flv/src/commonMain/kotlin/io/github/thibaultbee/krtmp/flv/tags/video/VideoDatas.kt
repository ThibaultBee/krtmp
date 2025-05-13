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
package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.sources.NaluRawSource
import io.github.thibaultbee.krtmp.flv.tags.video.ManyTrackManyCodecVideoTagBody.OneTrackMultiCodecVideoTagBody
import io.github.thibaultbee.krtmp.flv.util.av.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.av.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import kotlinx.io.RawSource

/**
 * Factories to create [VideoData] and [AudioData].
 */

// Legacy video data

/**
 * Creates a legacy video command data.
 *
 * @param codecID the codec ID
 * @param command the video command
 */
fun CommandLegacyVideoData(
    codecID: CodecID, command: VideoCommand
) = LegacyVideoData(
    frameType = VideoFrameType.COMMAND, codecID = codecID, body = CommandLegacyVideoTagBody(command)
)

/**
 * Creates a legacy [LegacyVideoData] from a [RawSource] and its size.
 *
 * For AVC/H.264, use [avcLegacyVideoData], [avcHeaderLegacyVideoData] or [avcEndOfSequenceLegacyVideoData] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 * @return the [LegacyVideoData] with the frame
 */
fun LegacyVideoData(
    frameType: VideoFrameType,
    codecID: CodecID,
    body: RawSource,
    bodySize: Int,
) = LegacyVideoData(
    frameType = frameType, codecID = codecID, packetType = AVCPacketType.NALU, // Unused
    compositionTime = 0, // Unused
    body = RawVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates a legacy AVC/H.264 [LegacyVideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [LegacyVideoData] with the AVC frame
 */
fun avcLegacyVideoData(
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
    body = RawVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates a legacy AVC/H.264 [LegacyVideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param body the NAL unit [RawSource].
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [LegacyVideoData] with the AVC frame
 */
fun avcLegacyVideoData(
    frameType: VideoFrameType,
    packetType: AVCPacketType,
    body: NaluRawSource,
    compositionTime: Int = 0
) = avcLegacyVideoData(
    frameType = frameType,
    packetType = packetType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates a legacy AVC/H.264 [LegacyVideoData] for SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 * If you want to directly pass [AVCDecoderConfigurationRecord], use [avcLegacyVideoData] instead.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [LegacyVideoData] with the SPS and PPS
 */
fun avcHeaderLegacyVideoData(
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps = sps, pps = pps
    ).readBuffer()

    avcLegacyVideoData(
        frameType = VideoFrameType.KEY,
        packetType = AVCPacketType.SEQUENCE_HEADER,
        compositionTime = 0,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates a legacy AVC/H.264 [LegacyVideoData] for the end of sequence.
 */
fun avcEndOfSequenceLegacyVideoData() = LegacyVideoData(
    frameType = VideoFrameType.KEY,
    codecID = CodecID.AVC,
    packetType = AVCPacketType.END_OF_SEQUENCE,
    compositionTime = 0,
    body = EmptyVideoTagBody()
)

// Extended video data
/**
 * Creates an extended video command data.
 *
 * @param packetType the packet type excluding [VideoPacketType.META_DATA]
 * @param command the video command
 * @return the [ExtendedVideoData] with the command
 */
fun CommandExtendedVideoData(
    packetType: VideoPacketType, command: VideoCommand
) = ExtendedVideoData(
    packetDescriptor = ExtendedVideoData.CommandVideoPacketDescriptor(
        command = command, packetType = packetType
    )
)

/**
 * Creates an extended coded frame AVC/H.264 [ExtendedVideoData] from a [RawSource] and its size.
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @return the [ExtendedVideoData] with the AVC frame
 */
fun avcCodedFrameExtendedVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    fourCC = VideoFourCC.AVC,
    packetType = VideoPacketType.CODED_FRAMES,
    body = CompositionTimeExtendedVideoTagBody(
        compositionTime = compositionTime, data = body, dataSize = bodySize
    )
)

/**
 * Creates an extended coded frame AVC/H.264 [ExtendedVideoData] from a [NaluRawSource].
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the NAL unit [RawSource].
 * @return the [ExtendedVideoData] with the AVC frame
 */
fun avcCodedFrameExtendedVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: NaluRawSource
) = avcCodedFrameExtendedVideoData(
    frameType = frameType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize
)

/**
 * Creates an extended AVC/H.264 [ExtendedVideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the coded AVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded AVC [RawSource]
 * @return the [ExtendedVideoData] with the AVC frame
 */
fun avcExtendedVideoData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    fourCC = VideoFourCC.AVC,
    packetType = packetType,
    body = RawVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an extended AVC/H.264 [ExtendedVideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the NAL unit [RawSource].
 * @return the [ExtendedVideoData] with the AVC frame
 */
fun avcExtendedVideoData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: NaluRawSource
) = avcExtendedVideoData(
    frameType = frameType, packetType = packetType, body = body.nalu, bodySize = body.naluSize
)

/**
 * Creates an extended AVC/H.264 [ExtendedVideoData] for SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 * If you want to directly pass [AVCDecoderConfigurationRecord], use [avcExtendedVideoData] instead.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the SPS and PPS
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
 * Creates an HEVC/H.265 coded frame [ExtendedVideoData] from a [RawSource] and its size.
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the coded HEVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded HEVC [RawSource]
 * @return the [ExtendedVideoData] with the HEVC frame
 */
fun hevcCodedFrameExtendedVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: RawSource, bodySize: Int
) = ExtendedVideoData(
    frameType = frameType,
    packetType = VideoPacketType.CODED_FRAMES,
    fourCC = VideoFourCC.HEVC,
    body = CompositionTimeExtendedVideoTagBody(
        compositionTime = compositionTime, data = body, dataSize = bodySize
    )
)


/**
 * Creates an HEVC/H.265 [ExtendedVideoData] from a [NaluRawSource].
 *
 * Packet type is forced to [VideoPacketType.CODED_FRAMES].
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits)
 * @param body the NAL unit [RawSource].
 * @return the [ExtendedVideoData] with the HEVC frame
 */
fun hevcCodedFrameExtendedVideoData(
    frameType: VideoFrameType, compositionTime: Int, body: NaluRawSource
) = hevcCodedFrameExtendedVideoData(
    frameType = frameType,
    compositionTime = compositionTime,
    body = body.nalu,
    bodySize = body.naluSize,
)

/**
 * Creates an HEVC/H.265 [ExtendedVideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the coded HEVC [RawSource] without AVCC or AnnexB header
 * @param bodySize the size of the coded HEVC [RawSource]
 * @return the [ExtendedVideoData] with the HEVC frame
 */
fun hevcVideoExtendedData(
    frameType: VideoFrameType,
    packetType: VideoPacketType,
    body: RawSource,
    bodySize: Int,
) = ExtendedVideoData(
    frameType = frameType,
    packetType = packetType,
    fourCC = VideoFourCC.HEVC,
    body = RawVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an HEVC/H.265 [ExtendedVideoData] from a [NaluRawSource].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type excluding [VideoPacketType.CODED_FRAMES]
 * @param body the NAL unit [RawSource].
 * @return the [ExtendedVideoData] with the HEVC frame
 */
fun hevcVideoExtendedData(
    frameType: VideoFrameType, packetType: VideoPacketType, body: NaluRawSource
) = hevcVideoExtendedData(
    frameType = frameType, packetType = packetType, body = body.nalu, bodySize = body.naluSize
)


/**
 * Creates an AVC/H.264 [ExtendedVideoData] for VPS, SPS and PPS.
 *
 * This method will create a [HEVCDecoderConfigurationRecord] from the VPS, SPS and PPS NAL units.
 * If you want to directly pass [HEVCDecoderConfigurationRecord], use [hevcVideoExtendedData] instead.
 *
 * @param vps the VPS NAL units
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the SPS and PPS
 */
fun hevcHeaderExtendedVideoData(
    vps: List<NaluRawSource>,
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) {
    val decoderConfigurationRecord = HEVCDecoderConfigurationRecord(
        vps = vps, sps = sps, pps = pps
    ).readBuffer()

    hevcVideoExtendedData(
        frameType = VideoFrameType.KEY,
        packetType = VideoPacketType.SEQUENCE_START,
        body = decoderConfigurationRecord,
        bodySize = decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates a [ExtendedVideoData] from a [RawSource] and its size.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type
 * @param fourCC the FourCCs
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 * @return the [ExtendedVideoData] with the frame
 */
fun extendedVideoData(
    frameType: VideoFrameType,
    packetType: VideoPacketType,
    fourCC: VideoFourCC,
    body: RawSource,
    bodySize: Int
) = ExtendedVideoData(
    frameType = frameType, packetType = packetType, fourCC = fourCC, body = RawVideoTagBody(
        data = body, dataSize = bodySize
    )
)

/**
 * Creates an [ExtendedVideoData] for the end of sequence.
 *
 * @param fourCC the FourCCs
 * @return the [ExtendedVideoData] with the end of sequence
 */
fun endOfSequenceExtendedVideoData(
    fourCC: VideoFourCC
) = ExtendedVideoData(
    VideoFrameType.KEY, VideoPacketType.SEQUENCE_END, fourCC, EmptyVideoTagBody()
)

/**
 * Creates an [ExtendedVideoData] for the metadata.
 *
 * @param fourCC the FourCCs
 * @param name the name of the metadata
 * @param value the value of the metadata
 */
fun metadataExtendedVideoData(
    fourCC: VideoFourCC,
    name: String,
    value: AmfElement
) = ExtendedVideoData(
    VideoFrameType.KEY, VideoPacketType.META_DATA, fourCC, MetadataVideoTagBody(name, value)
)

/**
 * Creates a [MultitrackVideoTagBody] for one track video data.
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param trackID the track ID
 * @param body the coded [RawSource]
 * @param bodySize the size of the coded [RawSource]
 */
fun oneTrackMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    framePacketType: VideoPacketType,
    trackID: Byte,
    body: RawSource,
    bodySize: Int
) = ExtendedVideoData(
    packetDescriptor = ExtendedVideoData.MultitrackVideoPacketDescriptor.OneTrackVideoPacketDescriptor(
        frameType = frameType,
        fourCC = fourCC,
        framePacketType = framePacketType,
        body = OneTrackVideoTagBody(
            trackId = trackID, body = RawVideoTagBody(data = body, dataSize = bodySize)
        )
    )
)

/**
 * Creates a [MultitrackVideoTagBody] for a one codec multitrack video data (either one or many tracks).
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackVideoTagBody]. If there is only one track in the set it is considered as a one track video data.
 */
fun oneCodecMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    framePacketType: VideoPacketType,
    tracks: Set<OneTrackVideoTagBody>
) {
    val packetDescriptor = if (tracks.size == 1) {
        ExtendedVideoData.MultitrackVideoPacketDescriptor.OneTrackVideoPacketDescriptor(
            frameType = frameType,
            fourCC = fourCC,
            framePacketType = framePacketType,
            body = tracks.first()
        )
    } else if (tracks.size > 1) {
        ExtendedVideoData.MultitrackVideoPacketDescriptor.ManyTrackVideoPacketDescriptor(
            frameType = frameType,
            fourCC = fourCC,
            framePacketType = framePacketType,
            body = ManyTrackOneCodecVideoTagBody(tracks)
        )
    } else {
        throw IllegalArgumentException("No track in the set")
    }

    ExtendedVideoData(
        packetDescriptor = packetDescriptor
    )
}

/**
 * Creates a [MultitrackVideoTagBody] for a many codec and many track video data.
 *
 * @param frameType the frame type
 * @param framePacketType the frame packet type
 * @param tracks the set of [OneTrackMultiCodecVideoTagBody]
 */
fun manyCodecMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    framePacketType: VideoPacketType,
    tracks: Set<OneTrackMultiCodecVideoTagBody>
) = ExtendedVideoData(
    packetDescriptor = ExtendedVideoData.MultitrackVideoPacketDescriptor.ManyTrackManyCodecVideoPacketDescriptor(
        frameType = frameType,
        framePacketType = framePacketType,
        body = ManyTrackManyCodecVideoTagBody(tracks)
    )
)