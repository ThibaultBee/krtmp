/*
 * Copyright (C) 2023 Thibault B.
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

/**
 * FourCC object
 *
 * Just AV1, VP9 and HEVC are described because these are the only FourCC required for enhanced RTMP.
 */

enum class FourCCs(val value: FourCC) {
    AV1(
        FourCC(
            'a', 'v', '0', '1', MediaType.VIDEO_AV1
        )
    ),
    VP9(FourCC('v', 'p', '0', '9', MediaType.VIDEO_VP9)),
    HEVC(FourCC('h', 'v', 'c', '1', MediaType.VIDEO_HEVC));

    companion object {
        fun mimeTypeOf(mediaType: MediaType) =
            entries.first { it.value.mediaType == mediaType }

        fun mimeTypeOf(mimeType: String) =
            entries.first { it.value.mediaType.value == mimeType }

        fun codeOf(value: Int) = entries.first { it.value.code == value }
    }
}

/**
 * FourCC is a 4 bytes code used to identify a codec.
 */
data class FourCC(val a: Char, val b: Char, val c: Char, val d: Char, val mediaType: MediaType) {

    /**
     * FourCC code
     */
    val code = (a.code shl 24) or (b.code shl 16) or (c.code shl 8) or d.code

    override fun toString(): String {
        return "$a$b$c$d"
    }
}