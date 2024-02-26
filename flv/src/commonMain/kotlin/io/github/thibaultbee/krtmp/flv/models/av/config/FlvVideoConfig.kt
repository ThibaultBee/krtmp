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
package io.github.thibaultbee.krtmp.flv.models.av.config

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.models.tags.LegacyVideoTag
import io.github.thibaultbee.krtmp.flv.models.tags.VideoTag

class FlvVideoConfig(
    mimeType: MimeType,
    bitrateBps: Int,
    val width: Int,
    val height: Int,
    val frameRate: Int,
) : FlvConfig(mimeType, bitrateBps) {
    val codecID = if (LegacyVideoTag.isSupportedCodec(mimeType)) {
        CodecID.mimetypeOf(mimeType)
    } else {
        null
    }

    init {
        require(mimeType.type == MimeType.Type.VIDEO) { "MimeType must be a video type" }
    }
}

enum class CodecID(val value: Byte, val mimeType: MimeType? = null) {
    SORENSON_H263(2, MimeType.VIDEO_H263),
    SCREEN_1(3),
    VP6(4),
    VP6_ALPHA(5),
    SCREEN_2(6),
    AVC(7, MimeType.VIDEO_AVC);

    companion object {
        fun mimetypeOf(mimeType: MimeType) = entries.first { it.mimeType == mimeType }

        fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}
