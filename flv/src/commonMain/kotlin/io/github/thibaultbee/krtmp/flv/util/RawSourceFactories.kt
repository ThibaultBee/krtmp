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
package io.github.thibaultbee.krtmp.flv.util

import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.sources.MultiRawSource
import io.github.thibaultbee.krtmp.flv.util.extensions.isAvcc
import io.github.thibaultbee.krtmp.flv.util.extensions.startCodeSize
import kotlinx.io.Buffer
import kotlinx.io.RawSource

private const val AVCC_HEADER_SIZE = 4

/**
 * Creates a [RawSource] in AVCC format from a [ByteArray].
 *
 * @param array the [ByteArray] to transform
 * @return a pair of [RawSource] in AVCC format and its size (including the AVCC header size)
 */
fun transformToAVCC(array: ByteArray): Pair<RawSource, Int> {
    if (array.isAvcc) {
        return ByteArrayBackedRawSource(array) to array.size
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = array.startCodeSize
    return transformToAVCC(
        ByteArrayBackedRawSource(array, startCodeSize.toLong()), array.size - startCodeSize
    )
}


/**
 * Creates a [RawSource] in AVCC format from a [Buffer].
 *
 * @param buffer the [Buffer] to transform
 * @return a pair of [RawSource] in AVCC format and its size (including the AVCC header size)
 */
fun transformToAVCC(buffer: Buffer): Pair<RawSource, Int> {
    if (buffer.isAvcc) {
        return buffer to buffer.size.toInt()
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = buffer.startCodeSize
    // Skip start code
    buffer.skip(startCodeSize.toLong())
    return transformToAVCC(
        buffer, buffer.size.toInt()
    )
}

/**
 * Creates a [RawSource] in AVCC format from a NAL unit [RawSource].
 *
 * The purpose of this class is to simplify the handling of frame data for AVC/H.264, HEVC/H.265.
 *
 * The [RawSource] will be in the NAL unit in AVCC format.
 *
 * To convert from other format such as AnnexB format (NAL unit with a start code 0x00000001) or no header, use other [transformToAVCC] builder.
 * methods.
 *
 * @param nalu the NAL unit [RawSource] (without AVCC or AnnexB header)
 * @param naluSize the size of the [nalu]
 * @return a pair of [RawSource] in AVCC format and its size (including the AVCC header size)
 */
internal fun transformToAVCC(
    nalu: RawSource, naluSize: Int
) = MultiRawSource(Buffer().apply { writeInt(naluSize) }, nalu) to naluSize + AVCC_HEADER_SIZE

