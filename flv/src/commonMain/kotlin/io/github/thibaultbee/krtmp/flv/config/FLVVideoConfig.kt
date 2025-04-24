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
package io.github.thibaultbee.krtmp.flv.config

class FLVVideoConfig(
    override val mediaType: VideoMediaType,
    override val bitrateBps: Int,
    val width: Int,
    val height: Int,
    val frameRate: Int,
) : FLVConfig<VideoMediaType> {
    val codecID = mediaType.codecID
    val fourCC = mediaType.fourCCs
    val metadataType = codecID?.value?.toInt() ?: fourCC!!.value.code
}


enum class CodecID(val value: Byte) {
    SORENSON_H263(2),
    SCREEN_1(3),
    VP6(4),
    VP6_ALPHA(5),
    SCREEN_2(6),
    AVC(7);

    companion object {
        fun entryOf(value: Byte) = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Unsupported CodecID: $value")
    }
}

/**
 * FourCC object
 *
 * Only enhanced RTMP FourCC exists.
 */
enum class VideoFourCC(val value: FourCC) {
    VP8(
        FourCC(
            'v', 'p', '0', '8'
        )
    ),
    VP9(FourCC('v', 'p', '0', '9')),
    AV1(
        FourCC(
            'a', 'v', '0', '1'
        )
    ),
    AVC(
        AVCHEVCFourCC(
            'a', 'v', 'c', '1'
        )
    ),
    HEVC(AVCHEVCFourCC('h', 'v', 'c', '1'));

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun codeOf(value: Int) = entries.firstOrNull { it.value.code == value }
            ?: throw IllegalArgumentException("Unsupported video FourCC: ${value.toHexString()}")
    }
}

class AVCHEVCFourCC(
    a: Char, b: Char, c: Char, d: Char
) : FourCC(a, b, c, d)

/**
 * A meta class for [CodecID] and [VideoFourCC].
 */
enum class VideoMediaType(val codecID: CodecID?, val fourCCs: VideoFourCC?) {
    SCREEN_1(CodecID.SCREEN_1, null),
    VP6(CodecID.VP6, null),
    VP6_ALPHA(CodecID.VP6_ALPHA, null),
    SCREEN_2(CodecID.SCREEN_2, null),
    SORENSON_H263(CodecID.SORENSON_H263, null),
    AVC(CodecID.AVC, VideoFourCC.AVC),
    HEVC(null, VideoFourCC.HEVC),
    VP8(null, VideoFourCC.VP8),
    VP9(null, VideoFourCC.VP9),
    AV1(null, VideoFourCC.AV1);

    companion object {
        fun fromFourCC(fourCC: VideoFourCC): VideoMediaType? {
            return entries.firstOrNull { it.fourCCs == fourCC }
        }

        fun fromCodecID(codecID: CodecID): VideoMediaType? {
            return entries.firstOrNull { it.codecID == codecID }
        }
    }
}
