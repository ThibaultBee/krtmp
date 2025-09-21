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
import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.tags.video.ManyTrackManyCodecVideoTagBody.OneTrackMultiCodecVideoTagBody
import io.github.thibaultbee.krtmp.flv.util.av.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.av.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import kotlinx.io.RawSource

/**
 * Factories to create [VideoData].
 */

// Legacy video data
/**
 * Creates a [LegacyVideoData] for command.
 *
 * @param codecID the codec ID
 * @param command the video command
 */
fun CommandVideoData(
    codecID: CodecID, command: VideoCommand
) = LegacyVideoData(
    frameType = VideoFrameType.COMMAND, codecID = codecID, body = CommandLegacyVideoTagBody(command)
)


/**
 * Creates a [LegacyVideoData] from a [RawSource].
 *
 * For AVC/H.264, use [AVCVideoDataFactory] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [RawSource]
 * @param dataSize the size of [data]
 * @return the [LegacyVideoData] with the frame
 */
fun VideoData(
    frameType: VideoFrameType,
    codecID: CodecID,
    data: RawSource,
    dataSize: Int,
) = LegacyVideoData(
    frameType = frameType, codecID = codecID, packetType = AVCPacketType.NALU, // Unused
    compositionTime = 0, // Unused
    body = RawVideoTagBody(
        data = data, dataSize = dataSize
    )
)


/**
 * Creates a [LegacyVideoData] from a [ByteArray].
 *
 * For AVC/H.264, use [AVCVideoDataFactory] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteArray]
 * @return the [LegacyVideoData] with the frame
 */
fun VideoData(
    frameType: VideoFrameType, codecID: CodecID, data: ByteArray
) = LegacyVideoData(
    frameType = frameType, codecID = codecID, packetType = AVCPacketType.NALU, // Unused
    compositionTime = 0, // Unused
    body = RawVideoTagBody(
        data = ByteArrayBackedRawSource(data), dataSize = data.size
    )
)

/**
 * Factory to create legacy [VideoData] for AVC/H.264.
 */
class AVCVideoDataFactory {
    /**
     * Creates an AVC/H.264 [LegacyVideoData] for a coded frame from a [RawSource].
     *
     * @param frameType the frame type (key frame or intra frame)
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of [data]
     * @param compositionTime the composition time (24 bits). Default is 0.
     * @return the [LegacyVideoData] with the frame
     */
    fun codedFrame(
        frameType: VideoFrameType, data: RawSource, dataSize: Int, compositionTime: Int = 0
    ) = LegacyVideoData(
        frameType = frameType,
        codecID = CodecID.AVC,
        packetType = AVCPacketType.NALU,
        compositionTime = compositionTime,
        body = RawVideoTagBody(
            data = data, dataSize = dataSize
        )
    )

    /**
     * Creates an AVC/H.264 [LegacyVideoData] for sequence start from a [RawSource].
     *
     * @param decoderConfigurationRecord the [AVCDecoderConfigurationRecord] as a [RawSource]
     * @param decoderConfigurationRecordSize the size of the [decoderConfigurationRecord]
     * @return the [LegacyVideoData] for sequence start
     */
    fun sequenceStart(
        decoderConfigurationRecord: RawSource, decoderConfigurationRecordSize: Int
    ): VideoData = LegacyVideoData(
        frameType = VideoFrameType.KEY,
        codecID = CodecID.AVC,
        packetType = AVCPacketType.SEQUENCE_HEADER,
        compositionTime = 0,
        body = RawVideoTagBody(
            data = decoderConfigurationRecord, dataSize = decoderConfigurationRecordSize
        )
    )

    /**
     * Creates an AVC/H.264 [LegacyVideoData] for sequence end.
     *
     * @return the [LegacyVideoData] for sequence end
     */
    fun sequenceEnd() = LegacyVideoData(
        frameType = VideoFrameType.KEY,
        codecID = CodecID.AVC,
        packetType = AVCPacketType.END_OF_SEQUENCE,
        compositionTime = 0,
        body = EmptyVideoTagBody()
    )
}


/**
 * Creates an AVC/H.264 [LegacyVideoData] for a coded frame from a [ByteArray].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteArray]
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [LegacyVideoData] with the frame
 */
fun AVCVideoDataFactory.codedFrame(
    frameType: VideoFrameType, data: ByteArray, compositionTime: Int = 0
) = LegacyVideoData(
    frameType = frameType,
    codecID = CodecID.AVC,
    packetType = AVCPacketType.NALU,
    compositionTime = compositionTime,
    body = RawVideoTagBody(
        data = ByteArrayBackedRawSource(data), dataSize = data.size
    )
)


/**
 * Creates an AVC/H.264 [LegacyVideoData] for sequence start from a [ByteArray].
 *
 * @param decoderConfigurationRecord the [AVCDecoderConfigurationRecord] as a [ByteArray]
 * @param decoderConfigurationRecordSize the size of the [decoderConfigurationRecord]
 * @return the [LegacyVideoData] for sequence start
 */
fun AVCVideoDataFactory.sequenceStart(
    decoderConfigurationRecord: ByteArray, decoderConfigurationRecordSize: Int
): VideoData = sequenceStart(
    ByteArrayBackedRawSource(decoderConfigurationRecord), decoderConfigurationRecordSize
)


/**
 * Creates an AVC/H.264 [LegacyVideoData] for sequence start from SPS and PPS from [RawSource].
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units and their sizes
 * @param pps the PPS NAL units and their sizes
 * @return the [LegacyVideoData] with the sequence start
 */
fun AVCVideoDataFactory.sequenceStart(
    sps: List<Pair<RawSource, Int>>,
    pps: List<Pair<RawSource, Int>>,
): VideoData {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps, pps
    ).readBuffer()

    return sequenceStart(
        decoderConfigurationRecord, decoderConfigurationRecord.size.toInt()
    )
}


// Extended video data
/**
 * Creates an [ExtendedVideoData] for command.
 *
 * @param packetType the packet type excluding [VideoPacketType.META_DATA]
 * @param command the video command
 * @return the [ExtendedVideoData] with the command
 */
fun CommandExtendedVideoData(
    packetType: VideoPacketType, command: VideoCommand
) = ExtendedVideoData(
    dataDescriptor = ExtendedVideoData.CommandVideoDataDescriptor(
        command = command, packetType = packetType
    )
)

/**
 * Base class for factories to create [ExtendedVideoData] with a predefined [VideoFourCC].
 *
 * @param fourCC the FourCCs.
 */
sealed class CommonExtendedVideoDataFactory(val fourCC: VideoFourCC) {
    /**
     * Creates an [ExtendedVideoData] for the sequence start from a [RawSource]
     *
     * @param decoderConfigurationRecord the decoder configuration record as a [RawSource]
     * @param decoderConfigurationRecordSize the size of the body [decoderConfigurationRecord]
     * @return the [ExtendedVideoData] for sequence start
     */
    fun sequenceStart(
        decoderConfigurationRecord: RawSource, decoderConfigurationRecordSize: Int
    ) = ExtendedVideoData(
        VideoFrameType.KEY, VideoPacketType.SEQUENCE_START, fourCC, RawVideoTagBody(
            data = decoderConfigurationRecord, dataSize = decoderConfigurationRecordSize
        )
    )

    /**
     * Creates an [ExtendedVideoData] for the metadata.
     *
     * @param name the name of the metadata
     * @param value the value of the metadata
     * @return the [ExtendedVideoData] for the metadata
     */
    fun metadata(name: String, value: AmfElement) = ExtendedVideoData(
        VideoFrameType.KEY, VideoPacketType.META_DATA, fourCC, MetadataVideoTagBody(name, value)
    )

    /**
     * Creates an [ExtendedVideoData] for sequence end.
     *
     * @return the [ExtendedVideoData] for sequence end
     */
    fun sequenceEnd() = ExtendedVideoData(
        VideoFrameType.KEY, VideoPacketType.SEQUENCE_END, fourCC, EmptyVideoTagBody()
    )
}


/**
 * Creates an [ExtendedVideoData] for the sequence start from a [ByteArray]
 *
 * @param decoderConfigurationRecord the decoder configuration record as a [ByteArray]
 * @return the [ExtendedVideoData] for sequence start
 */
fun CommonExtendedVideoDataFactory.sequenceStart(
    decoderConfigurationRecord: ByteArray
) = sequenceStart(
    ByteArrayBackedRawSource(decoderConfigurationRecord), decoderConfigurationRecord.size
)


/**
 * Factory to create [ExtendedVideoData] for AVC/H.264.
 */
class AVCExtendedVideoDataFactory : AVCHEVCExtendedVideoDataFactory(VideoFourCC.AVC)

/**
 * Creates an AVC/H.264 [ExtendedVideoData] for sequence start from SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units and their sizes
 * @param pps the PPS NAL units and their sizes
 * @return the [ExtendedVideoData] with the sequence start
 */
fun AVCExtendedVideoDataFactory.sequenceStart(
    sps: List<Pair<RawSource, Int>>,
    pps: List<Pair<RawSource, Int>>,
): ExtendedVideoData {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps, pps
    ).readBuffer()

    return sequenceStart(
        decoderConfigurationRecord, decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates an AVC/H.264 [ExtendedVideoData] for sequence start from SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the sequence start
 */
fun AVCExtendedVideoDataFactory.sequenceStartByteArray(
    sps: List<ByteArray>,
    pps: List<ByteArray>,
) = sequenceStart(
    sps.map { Pair(ByteArrayBackedRawSource(it), it.size) },
    pps.map { Pair(ByteArrayBackedRawSource(it), it.size) })


/**
 * Factory to create [ExtendedVideoData] for HEVC/H.265.
 */
class HEVCExtendedVideoDataFactory : AVCHEVCExtendedVideoDataFactory(VideoFourCC.HEVC)

/**
 * Creates an HEVC/H.265 [ExtendedVideoData] for sequence start from SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param vps the VPS NAL units and their sizes
 * @param sps the SPS NAL units and their sizes
 * @param pps the PPS NAL units and their sizes
 * @return the [ExtendedVideoData] with the sequence start
 */
fun HEVCExtendedVideoDataFactory.sequenceStart(
    vps: List<Pair<RawSource, Int>>,
    sps: List<Pair<RawSource, Int>>,
    pps: List<Pair<RawSource, Int>>,
): ExtendedVideoData {
    val decoderConfigurationRecord = HEVCDecoderConfigurationRecord(
        vps, sps, pps
    ).readBuffer()
    return sequenceStart(
        decoderConfigurationRecord, decoderConfigurationRecord.size.toInt()
    )
}


/**
 * Creates an HEVC/H.265 [ExtendedVideoData] for sequence start from SPS and PPS.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param vps the VPS NAL units
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the sequence start
 */
fun HEVCExtendedVideoDataFactory.sequenceStartByteArray(
    vps: List<ByteArray>,
    sps: List<ByteArray>,
    pps: List<ByteArray>,
) = sequenceStart(
    vps.map { Pair(ByteArrayBackedRawSource(it), it.size) },
    sps.map { Pair(ByteArrayBackedRawSource(it), it.size) },
    pps.map { Pair(ByteArrayBackedRawSource(it), it.size) })


/**
 * Factory to create [ExtendedVideoData] for AVC/H.264 and HEVC/H.265.
 *
 * Use [AVCExtendedVideoDataFactory] or [HEVCExtendedVideoDataFactory] to create a factory for a specific codec.
 *
 * @param fourCC the FourCCs, must be either [VideoFourCC.AVC] or [VideoFourCC.HEVC].
 */
sealed class AVCHEVCExtendedVideoDataFactory(fourCC: VideoFourCC) :
    CommonExtendedVideoDataFactory(fourCC) {
    init {
        require(fourCC == VideoFourCC.AVC || fourCC == VideoFourCC.HEVC) {
            "fourCC must be either AVC or HEVC"
        }
    }

    /**
     * Creates an [ExtendedVideoData] for coded frame from a [RawSource] with composition time.
     *
     * @param frameType the frame type (key frame or intra frame)
     * @param compositionTime the composition time (24 bits).
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedVideoData] with the frame
     * @see codedFrameX
     */
    fun codedFrame(
        frameType: VideoFrameType, compositionTime: Int, data: RawSource, dataSize: Int
    ) = ExtendedVideoData(
        frameType = frameType,
        packetType = VideoPacketType.CODED_FRAMES,
        fourCC = fourCC,
        body = CompositionTimeExtendedVideoTagBody(
            compositionTime = compositionTime, data = data, dataSize = dataSize
        )
    )

    /**
     * Creates an [ExtendedVideoData] for coded frame from a [RawSource] where composition time is implicitly 0.
     *
     * @param frameType the frame type (key frame or intra frame)
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedVideoData] with the frame
     * @see codedFrame
     */
    fun codedFrameX(
        frameType: VideoFrameType, data: RawSource, dataSize: Int
    ) = ExtendedVideoData(
        frameType = frameType,
        packetType = VideoPacketType.CODED_FRAMES_X,
        fourCC = fourCC,
        body = RawVideoTagBody(
            data = data, dataSize = dataSize
        )
    )
}


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteArray] with composition time.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits).
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrameX
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType, compositionTime: Int, data: ByteArray
) = codedFrame(
    frameType = frameType,
    compositionTime = compositionTime,
    data = ByteArrayBackedRawSource(data),
    dataSize = data.size
)


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteArray] where composition time is implicitly 0.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrame
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrameX(
    frameType: VideoFrameType, data: ByteArray
) = codedFrameX(
    frameType = frameType, data = ByteArrayBackedRawSource(data), dataSize = data.size
)


/**
 * Factory to create [ExtendedVideoData] with a predefined [VideoFourCC].
 *
 * For [VideoFourCC.AVC] and [VideoFourCC.HEVC], consider using [AVCHEVCExtendedVideoDataFactory] instead.
 *
 * @param fourCC the FourCCs.
 */
class ExtendedVideoDataFactory(fourCC: VideoFourCC) : CommonExtendedVideoDataFactory(fourCC) {
    init {
        require(fourCC != VideoFourCC.AVC && fourCC != VideoFourCC.HEVC) {
            "For AVC and HEVC, consider using AVCHEVCExtendedVideoDataFactory instead"
        }
    }

    /**
     * Creates an [ExtendedVideoData] for coded frame from a [RawSource].
     *
     * @param frameType the frame type (key frame or intra frame)
     * @param data the coded frame as a [RawSource]
     * @param dataSize the size of the [data]
     * @return the [ExtendedVideoData] with the frame
     */
    fun codedFrame(
        frameType: VideoFrameType, data: RawSource, dataSize: Int
    ) = ExtendedVideoData(
        frameType = frameType,
        packetType = VideoPacketType.CODED_FRAMES,
        fourCC = fourCC,
        body = RawVideoTagBody(
            data = data, dataSize = dataSize
        )
    )
}


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteArray].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedVideoData] with the frame
 */
fun ExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType, data: ByteArray
) = codedFrame(
    frameType = frameType, data = ByteArrayBackedRawSource(data), dataSize = data.size
)

// Multi track video data

/**
 * Creates a [ExtendedVideoData] for one track video data from a [RawSource].
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param packetType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [RawSource]
 * @param dataSize the size of the [data]
 * @return the [ExtendedVideoData] with the one track video data
 */
fun oneTrackMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    packetType: VideoPacketType,
    trackID: Byte,
    data: RawSource,
    dataSize: Int
) = ExtendedVideoData(
    dataDescriptor = ExtendedVideoData.MultitrackVideoDataDescriptor.OneTrackVideoDataDescriptor(
        frameType = frameType,
        fourCC = fourCC,
        framePacketType = packetType,
        body = OneTrackVideoTagBody(
            trackId = trackID, body = RawVideoTagBody(data = data, dataSize = dataSize)
        )
    )
)


/**
 * Creates a [ExtendedVideoData] for one track video data from a [ByteArray].
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param packetType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [ByteArray]
 * @return the [ExtendedVideoData] with the one track video data
 */
fun oneTrackMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    packetType: VideoPacketType,
    trackID: Byte,
    data: ByteArray
) = oneTrackMultitrackExtendedVideoData(
    frameType, fourCC, packetType, trackID, ByteArrayBackedRawSource(data), data.size
)


/**
 * Creates a [ExtendedVideoData] for one track video data.
 *
 * The [ExtendedVideoData.SingleVideoDataDescriptor] comes from a single track [ExtendedVideoData.dataDescriptor].
 *
 * @param trackID the track ID
 * @param dataDescriptor the [ExtendedVideoData.SingleVideoDataDescriptor] containing the video data
 * @return the [ExtendedVideoData] with the one track video data
 */
fun oneTrackMultitrackExtendedVideoData(
    trackID: Byte, dataDescriptor: ExtendedVideoData.SingleVideoDataDescriptor
) = ExtendedVideoData(
    dataDescriptor = ExtendedVideoData.MultitrackVideoDataDescriptor.OneTrackVideoDataDescriptor(
        frameType = dataDescriptor.frameType,
        fourCC = dataDescriptor.fourCC,
        framePacketType = dataDescriptor.packetType,
        body = OneTrackVideoTagBody(
            trackId = trackID, body = dataDescriptor.body
        )
    )
)


/**
 * Creates a [ExtendedVideoData] for a one codec multitrack video data (either one or many tracks).
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param packetType the frame packet type
 * @param tracks the set of [OneTrackVideoTagBody]. If there is only one track in the set it is considered as a one track video data.
 * @return the [ExtendedVideoData] with the multitrack video data
 */
fun oneCodecMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    packetType: VideoPacketType,
    tracks: Set<OneTrackVideoTagBody>
): ExtendedVideoData {
    require(tracks.isNotEmpty()) { "tracks cannot be empty" }
    val packetDescriptor = if (tracks.size == 1) {
        ExtendedVideoData.MultitrackVideoDataDescriptor.OneTrackVideoDataDescriptor(
            frameType = frameType,
            fourCC = fourCC,
            framePacketType = packetType,
            body = tracks.first()
        )
    } else {
        ExtendedVideoData.MultitrackVideoDataDescriptor.ManyTrackVideoDataDescriptor(
            frameType = frameType,
            fourCC = fourCC,
            framePacketType = packetType,
            body = ManyTrackOneCodecVideoTagBody(tracks)
        )
    }

    return ExtendedVideoData(
        dataDescriptor = packetDescriptor
    )
}


/**
 * Creates a [ExtendedVideoData] for a many codec and many track video data.
 *
 * @param frameType the frame type
 * @param packetType the frame packet type
 * @param tracks the set of [OneTrackMultiCodecVideoTagBody]
 * @return the [ExtendedVideoData] with the multitrack video data
 */
fun manyCodecMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    packetType: VideoPacketType,
    tracks: Set<OneTrackMultiCodecVideoTagBody>
) = ExtendedVideoData(
    dataDescriptor = ExtendedVideoData.MultitrackVideoDataDescriptor.ManyTrackManyCodecVideoDataDescriptor(
        frameType = frameType,
        framePacketType = packetType,
        body = ManyTrackManyCodecVideoTagBody(tracks)
    )
)
