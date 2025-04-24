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
package io.github.thibaultbee.krtmp.flv.models.config

class FLVVideoConfig(
    override val mediaType: MediaType,
    override val bitrateBps: Int,
    val width: Int,
    val height: Int,
    val frameRate: Int,
) : FLVConfig {
    val codecID = if (CodecID.isSupported(mediaType)) {
        CodecID.fromMediaType(mediaType)
    } else {
        null
    }

    init {
        require(mediaType.type == MediaType.Type.VIDEO) { "MimeType must be a video type" }
    }
}


enum class CodecID(val value: Byte, val mediaType: MediaType? = null) {
    SORENSON_H263(2, MediaType.VIDEO_H263),
    SCREEN_1(3),
    VP6(4),
    VP6_ALPHA(5),
    SCREEN_2(6),
    AVC(7, MediaType.VIDEO_AVC);

    companion object {
        fun entryOf(value: Byte) = entries.firstOrNull { it.value == value }
            ?: throw IllegalArgumentException("Unsupported CodecID: $value")

        fun isSupported(mediaType: MediaType): Boolean {
            return entries.any { it.mediaType == mediaType }
        }

        fun fromMediaType(mediaType: MediaType): CodecID {
            return entries.firstOrNull { it.mediaType == mediaType }
                ?: throw IllegalArgumentException("Unsupported MediaType: $mediaType")
        }
    }
}

