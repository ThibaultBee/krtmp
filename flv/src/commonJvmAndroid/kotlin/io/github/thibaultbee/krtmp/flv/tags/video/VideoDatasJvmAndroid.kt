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
package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.common.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.util.av.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import java.nio.ByteBuffer

/**
 * Extensions for JVM to create [VideoData] from [ByteBuffer].
 */

// Legacy video data

/**
 * Creates a [LegacyVideoData] from a [ByteBuffer].
 *
 * For AVC/H.264, use [AVCVideoDataFactory] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteBuffer]
 * @return the [LegacyVideoData] with the frame
 */
fun VideoData(
    frameType: VideoFrameType,
    codecID: CodecID,
    data: ByteBuffer
) = VideoData(frameType, codecID, ByteBufferBackedRawSource(data), data.remaining())


/**
 * Creates an AVC/H.264 [LegacyVideoData] fpr a coded frame from a [ByteBuffer].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteBuffer] in AVCC format (length + NALU)
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [LegacyVideoData] with the frame
 */
fun AVCVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    data: ByteBuffer,
    compositionTime: Int = 0
) = codedFrame(
    frameType,
    ByteBufferBackedRawSource(data),
    data.remaining(),
    compositionTime
)


/**
 * Creates an AVC/H.264 [LegacyVideoData] for sequence start from a [ByteBuffer].
 *
 * @param decoderConfigurationRecord the [AVCDecoderConfigurationRecord] as a [ByteBuffer]
 * @return the [LegacyVideoData] with the sequence start
 */
fun AVCVideoDataFactory.sequenceStart(
    decoderConfigurationRecord: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(decoderConfigurationRecord),
    decoderConfigurationRecord.remaining()
)


/**
 * Creates an AVC/H.264 [LegacyVideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [LegacyVideoData] with the sequence start
 */
fun AVCVideoDataFactory.sequenceStartByteBuffer(
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
): LegacyVideoData {
    val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
        sps.map { ByteBufferBackedRawSource(it) to it.remaining() },
        pps.map { ByteBufferBackedRawSource(it) to it.remaining() }
    ).readBuffer()

    return sequenceStart(
        decoderConfigurationRecord,
        decoderConfigurationRecord.size.toInt()
    )
}


// Extended video data

/**
 * Creates an [ExtendedVideoData] for the sequence start.
 *
 * @param decoderConfigurationRecord the decoder configuration record as a [ByteBuffer]
 * @return the [ExtendedVideoData] with the sequence start
 */
fun CommonExtendedVideoDataFactory.sequenceStart(
    decoderConfigurationRecord: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(decoderConfigurationRecord),
    decoderConfigurationRecord.remaining()
)


/**
 * Creates an AVC/H.264 [ExtendedVideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the sequence start
 */
fun AVCExtendedVideoDataFactory.sequenceStartByteBuffer(
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
) = sequenceStart(
    sps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    pps.map { ByteBufferBackedRawSource(it) to it.remaining() }
)

/**
 * Creates an HEVC/H.265 [ExtendedVideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData] with the sequence start
 */
fun HEVCExtendedVideoDataFactory.sequenceStartByteBuffer(
    vps: List<ByteBuffer>,
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
) = sequenceStart(
    vps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    sps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    pps.map { ByteBufferBackedRawSource(it) to it.remaining() }
)


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteBuffer] with composition time.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits).
 * @param data the coded frame as a [ByteBuffer] in AVCC format (length + NALU)
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrameX
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    data: ByteBuffer,
    compositionTime: Int
) = codedFrame(
    frameType,
    ByteBufferBackedRawSource(data),
    data.remaining(),
    compositionTime
)


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteBuffer] where composition time is implicitly 0.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteBuffer] in AVCC format (length + NALU)
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrame
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrameX(
    frameType: VideoFrameType,
    data: ByteBuffer
) = codedFrameX(frameType, ByteBufferBackedRawSource(data), data.remaining())


/**
 * Creates an [ExtendedVideoData] for coded frame from a [ByteBuffer].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedVideoData] with the frame
 */
fun ExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    data: ByteBuffer
) = codedFrame(frameType, ByteBufferBackedRawSource(data), data.remaining())


// Multi track video data

/**
 * Creates a [MultitrackVideoTagBody] for one track video data from a [ByteBuffer].
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param packetType the frame packet type
 * @param trackID the track ID
 * @param data the coded frame as a [ByteBuffer]
 * @return the [ExtendedVideoData] with the one track video data
 */
fun oneTrackMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    packetType: VideoPacketType,
    trackID: Byte,
    data: ByteBuffer
) = oneTrackMultitrackExtendedVideoData(
    frameType,
    fourCC,
    packetType,
    trackID,
    ByteBufferBackedRawSource(data),
    data.remaining()
)
