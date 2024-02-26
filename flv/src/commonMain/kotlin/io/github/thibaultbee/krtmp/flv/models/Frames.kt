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
package io.github.thibaultbee.krtmp.flv.models

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.extensions.isAvcc
import io.github.thibaultbee.krtmp.flv.extensions.startCodeSize
import io.github.thibaultbee.krtmp.flv.models.av.utils.AudioSpecificConfig
import io.github.thibaultbee.krtmp.flv.models.av.utils.aac.AAC
import io.github.thibaultbee.krtmp.flv.models.av.utils.aac.ADTS
import io.github.thibaultbee.krtmp.flv.models.av.utils.avc.AVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.av.utils.hevc.HEVCDecoderConfigurationRecord
import io.github.thibaultbee.krtmp.flv.models.sources.ByteArrayRawSource
import io.github.thibaultbee.krtmp.flv.models.sources.MultiRawSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource

private const val AVCC_HEADER_SIZE = 4

/**
 * Creates a [NaluRawSource] from a [ByteArray].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param array the [ByteArray] to transform
 */
fun NaluRawSource(array: ByteArray): NaluRawSource {
    if (array.isAvcc) {
        return NaluRawSource(
            ByteArrayRawSource(array, AVCC_HEADER_SIZE.toLong()),
            array.size - AVCC_HEADER_SIZE
        )
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = array.startCodeSize
    return NaluRawSource(
        ByteArrayRawSource(array, startCodeSize.toLong()), array.size - startCodeSize
    )
}

/**
 * Creates a [NaluRawSource] from a [Buffer].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param buffer the [Buffer] to transform
 */
fun NaluRawSource(buffer: Buffer): NaluRawSource {
    if (buffer.isAvcc) {
        buffer.skip(AVCC_HEADER_SIZE.toLong())
        return NaluRawSource(buffer, buffer.size.toInt())
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = buffer.startCodeSize
    // Remove start code
    buffer.skip(startCodeSize.toLong())
    return NaluRawSource(
        buffer,
        buffer.size.toInt()
    )
}

/**
 * Creates a [NaluRawSource] from a [RawSource].
 *
 * It will extract the NAL unit by removing the header.
 *
 * @param source the [RawSource] to wrap
 * @param byteCount the number of bytes of the [source]
 * @param headerSize the size of the header to remove (0 if no header). Header could be AVCC (4 bytes) or AnnexB (often 4 bytes)
 */
fun NaluRawSource(source: RawSource, byteCount: Int, headerSize: Int): NaluRawSource {
    if (headerSize == 0) {
        return NaluRawSource(source, byteCount)
    }

    // Remove header
    source.readAtMostTo(Buffer(), headerSize.toLong())

    return NaluRawSource(
        source,
        byteCount - headerSize
    )
}

/**
 * A [SizedRawSource] that represents a NAL unit.
 *
 * The purpose of this class is to simplify the handling of frame data for AVC/H.264, HEVC/H.265.
 *
 * The [SizedRawSource] will be in the NAL unit in AVCC format.
 *
 * To convert from other format such as AnnexB format (NAL unit with a start code 0x00000001) or no header, use other [NaluRawSource] builder.
 * methods.
 *
 * @param nalu the NAL unit [RawSource] (without AVCC or AnnexB header)
 * @param naluSize the size of the NAL unit
 */
class NaluRawSource
internal constructor(
    val nalu: RawSource,
    val naluSize: Int
) : SizedRawSource(
    MultiRawSource(listOf(Buffer().apply { writeInt(naluSize) }, nalu)),
    naluSize + AVCC_HEADER_SIZE.toLong()
)


/**
 * A single frame of a media.
 *
 * @param data the frame data
 * @param mimeType the frame mime type
 * @param timestampMs the frame timestamp in milliseconds
 * @param descriptor the frame descriptor (AVC (AVCDecoderConfigurationRecord), HEVC, VP9, AV1, AAC, ...)
 */
sealed class Frame(
    val data: SizedRawSource,
    val mimeType: MimeType,
    val timestampMs: Int,
    val descriptor: SizedRawSource?,
) {
    val isVideo = mimeType.type == MimeType.Type.VIDEO
    val isAudio = mimeType.type == MimeType.Type.AUDIO
}

/**
 * A video frame.
 */
class VideoFrame
internal constructor(
    data: SizedRawSource,
    mimeType: MimeType,
    timestampMs: Int,
    val isKeyFrame: Boolean,
    descriptor: SizedRawSource?
) :
    Frame(
        data = data,
        mimeType = mimeType,
        timestampMs = timestampMs,
        descriptor = descriptor,
    )

/**
 * Creates an AVC/H.264 video frame from sps and pps.
 */
fun AVCFrame(
    data: NaluRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) = AVCFrame(
    data = data,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    avcc = SizedRawSource(
        AVCDecoderConfigurationRecord(
            sps,
            pps
        ).readBuffer()
    ),
)

/**
 * Creates an AVC/H.264 video frame from an AVCDecoderConfigurationRecord.
 *
 * @param avcc the AVCDecoderConfigurationRecord. It send each time it present. It is mandatory for the first frame.
 */
fun AVCFrame(
    data: NaluRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    avcc: SizedRawSource?,
) = VideoFrame(
    data = data,
    mimeType = MimeType.VIDEO_AVC,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    descriptor = avcc,
)

/**
 * Creates an HEVC/H.265 video frame from a [NaluRawSource].
 */
fun HEVCFrame(
    data: NaluRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    vps: List<NaluRawSource>,
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
) = HEVCFrame(
    data = data,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    hvcc = SizedRawSource(HEVCDecoderConfigurationRecord(vps, sps, pps).readBuffer())
)

/**
 * Creates an HEVC/H.265 video frame from a [NaluRawSource].
 *
 * @param hvcc the HEVCDecoderConfigurationRecord. It send each time it present. It is mandatory for the first frame.
 */
fun HEVCFrame(
    data: NaluRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    hvcc: SizedRawSource?,
) = VideoFrame(
    data = data,
    mimeType = MimeType.VIDEO_HEVC,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    descriptor = hvcc,
)

/**
 * Creates a VP9 video frame from a [NaluRawSource].
 *
 * @param decoderConfigurationRecord the VP9DecoderConfigurationRecord. It send each time it present. It is mandatory for the first frame.
 */
fun VP9Frame(
    data: SizedRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    decoderConfigurationRecord: SizedRawSource?,
) = VideoFrame(
    data = data,
    mimeType = MimeType.VIDEO_VP9,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    descriptor = decoderConfigurationRecord,
)

/**
 * Creates an AV1 video frame from a [NaluRawSource].
 *
 * @param decoderConfigurationRecord the AV1DecoderConfigurationRecord. It send each time it present. It is mandatory for the first frame.
 */
fun AV1Frame(
    data: SizedRawSource,
    timestampMs: Int,
    isKeyFrame: Boolean,
    decoderConfigurationRecord: SizedRawSource?,
) = VideoFrame(
    data = data,
    mimeType = MimeType.VIDEO_AV1,
    timestampMs = timestampMs,
    isKeyFrame = isKeyFrame,
    descriptor = decoderConfigurationRecord,
)

/**
 * An audio frame.
 */
class AudioFrame
internal constructor(
    data: SizedRawSource,
    mimeType: MimeType,
    timestampMs: Int,
    descriptor: SizedRawSource?,
) :
    Frame(data = data, mimeType = mimeType, timestampMs = timestampMs, descriptor = descriptor)

/**
 * Creates an AAC audio frame from a [NaluRawSource] and [AAC.ADTS].
 */
fun AACFrame(data: SizedRawSource, timestampMs: Int, adts: ADTS) =
    AACFrame(
        data, timestampMs,
        SizedRawSource(AudioSpecificConfig(adts).readBuffer())
    )

/**
 * Creates an AAC audio frame from a [NaluRawSource].
 */
fun AACFrame(data: SizedRawSource, timestampMs: Int, audioSpecificConfig: SizedRawSource? = null) =
    AudioFrame(data, MimeType.AUDIO_AAC, timestampMs, audioSpecificConfig)