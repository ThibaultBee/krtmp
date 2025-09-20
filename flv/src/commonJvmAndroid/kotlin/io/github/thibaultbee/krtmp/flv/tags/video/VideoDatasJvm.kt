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

import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.flv.util.av.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import java.nio.ByteBuffer

/**
 * Extensions for JVM to create [VideoData] and [ExtendedVideoData] from [ByteBuffer]s.
 */

// Legacy video data

/**
 * Creates a legacy [VideoData] from a [ByteBuffer]s.
 *
 * For AVC/H.264, use [AVCVideoDataFactory] instead.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [ByteBuffer]
 * @return the [VideoData] with the frame
 */
fun VideoData(
    frameType: VideoFrameType,
    codecID: CodecID,
    body: ByteBuffer
) = VideoData(frameType, codecID, ByteBufferBackedRawSource(body), body.remaining())


/**
 * Creates a legacy AVC/H.264 [VideoData] from a [ByteBuffer].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded AVC [ByteBuffer] without AVCC or AnnexB header
 * @param compositionTime the composition time (24 bits). Default is 0.
 * @return the [VideoData]
 */
fun AVCVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    body: ByteBuffer,
    compositionTime: Int = 0
) = codedFrame(
    frameType,
    ByteBufferBackedRawSource(body),
    body.remaining(),
    compositionTime
)


/**
 * Creates a legacy AVC/H.264 [VideoData] for [AVCDecoderConfigurationRecord] from a [ByteBuffer].
 *
 * @param decoderConfigurationRecord the [AVCDecoderConfigurationRecord] as a [ByteBuffer]
 * @return the [VideoData]
 */
fun AVCVideoDataFactory.sequenceStart(
    decoderConfigurationRecord: ByteBuffer
): VideoData = sequenceStart(
    ByteBufferBackedRawSource(decoderConfigurationRecord),
    decoderConfigurationRecord.remaining()
)


/**
 * Creates a legacy AVC/H.264 [VideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [VideoData]
 */
fun AVCVideoDataFactory.sequenceStart(
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
): VideoData {
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
 * @param body the body [ByteBuffer], usually a decoder configuration record
 * @return the [ExtendedVideoData] for sequence start
 */
fun CommonExtendedVideoDataFactory.sequenceStart(
    body: ByteBuffer
) = sequenceStart(
    ByteBufferBackedRawSource(body),
    body.remaining()
)


/**
 * Creates an extended AVC/H.264 [ExtendedVideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData]
 */
fun AVCExtendedVideoDataFactory.sequenceStart(
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
) = sequenceStart(
    sps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    pps.map { ByteBufferBackedRawSource(it) to it.remaining() }
)

/**
 * Creates an extended HEVC/H.264 [ExtendedVideoData] for sequence start from SPS and PPS as [ByteBuffer]s.
 *
 * This method will create a [AVCDecoderConfigurationRecord] from the SPS and PPS NAL units.
 *
 * @param sps the SPS NAL units
 * @param pps the PPS NAL units
 * @return the [ExtendedVideoData]
 */
fun HEVCExtendedVideoDataFactory.sequenceStart(
    vps: List<ByteBuffer>,
    sps: List<ByteBuffer>,
    pps: List<ByteBuffer>,
) = sequenceStart(
    vps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    sps.map { ByteBufferBackedRawSource(it) to it.remaining() },
    pps.map { ByteBufferBackedRawSource(it) to it.remaining() }
)


/**
 * Creates an [ExtendedVideoData] from a [ByteBuffer] with composition time.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param compositionTime the composition time (24 bits).
 * @param body the coded [ByteBuffer]
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrameX
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    compositionTime: Int,
    body: ByteBuffer
) = codedFrame(
    frameType,
    compositionTime,
    ByteBufferBackedRawSource(body),
    body.remaining()
)


/**
 * Creates an [ExtendedVideoData] from a [ByteBuffer] where composition time is implicitly 0.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [ByteBuffer]
 * @return the [ExtendedVideoData] with the frame
 * @see codedFrame
 */
fun AVCHEVCExtendedVideoDataFactory.codedFrameX(
    frameType: VideoFrameType,
    body: ByteBuffer
) = codedFrameX(frameType, ByteBufferBackedRawSource(body), body.remaining())


/**
 * Creates an [ExtendedVideoData] from a [ByteBuffer].
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param body the coded [ByteBuffer]
 * @return the [ExtendedVideoData] with the frame
 */
fun ExtendedVideoDataFactory.codedFrame(
    frameType: VideoFrameType,
    body: ByteBuffer
) = codedFrame(frameType, ByteBufferBackedRawSource(body), body.remaining())


// Multi track video data

/**
 * Creates a [MultitrackVideoTagBody] for one track video data from a [ByteBuffer].
 *
 * @param frameType the frame type
 * @param fourCC the FourCCs
 * @param packetType the frame packet type
 * @param trackID the track ID
 * @param body the coded [ByteBuffer]
 */
fun oneTrackMultitrackExtendedVideoData(
    frameType: VideoFrameType,
    fourCC: VideoFourCC,
    packetType: VideoPacketType,
    trackID: Byte,
    body: ByteBuffer
) = oneTrackMultitrackExtendedVideoData(
    frameType,
    fourCC,
    packetType,
    trackID,
    ByteBufferBackedRawSource(body),
    body.remaining()
)
