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
package io.github.thibaultbee.krtmp.flv.sources

import io.github.thibaultbee.krtmp.flv.util.extensions.isAvcc
import io.github.thibaultbee.krtmp.flv.util.extensions.startCodeSize
import kotlinx.io.Buffer
import kotlinx.io.RawSource

private const val AVCC_HEADER_SIZE = 4

/**
 * Creates a [RawSource] in AVCC format from a [ByteArray].
 *
 * @param array the [ByteArray] to transform
 */
fun avccRawSource(array: ByteArray): RawSourceWithSize {
    if (array.isAvcc) {
        return avccRawSource(
            ByteArrayBackedRawSource(array, AVCC_HEADER_SIZE.toLong()),
            array.size - AVCC_HEADER_SIZE
        )
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = array.startCodeSize
    return avccRawSource(
        ByteArrayBackedRawSource(array, startCodeSize.toLong()), array.size - startCodeSize
    )
}


/**
 * Creates a [RawSource] in AVCC format from a [Buffer].
 *
 * @param buffer the [Buffer] to transform
 */
fun avccRawSource(buffer: Buffer): RawSourceWithSize {
    if (buffer.isAvcc) {
        buffer.skip(AVCC_HEADER_SIZE.toLong())
        return avccRawSource(buffer, buffer.size.toInt())
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = buffer.startCodeSize
    // Remove start code
    buffer.skip(startCodeSize.toLong())
    return avccRawSource(
        buffer, buffer.size.toInt()
    )
}


/**
 * Creates a [RawSource] in AVCC format from a [RawSource].
 *
 * @param source the [RawSource] to wrap
 * @param byteCount the number of bytes of the [source]
 * @param headerSize the size of the header to remove (0 if no header). Header could be AVCC (4 bytes) or AnnexB (often 4 bytes)
 */
fun avccRawSource(source: RawSource, byteCount: Int, headerSize: Int): RawSourceWithSize {
    if (headerSize == 0) {
        return avccRawSource(source, byteCount)
    }

    // Remove header
    source.readAtMostTo(Buffer(), headerSize.toLong())

    return avccRawSource(
        source, byteCount - headerSize
    )
}

/**
 * A [RawSourceWithSize] that represents a NAL unit.
 *
 * The purpose of this class is to simplify the handling of frame data for AVC/H.264, HEVC/H.265.
 *
 * The [RawSource] will be in the NAL unit in AVCC format.
 *
 * To convert from other format such as AnnexB format (NAL unit with a start code 0x00000001) or no header, use other [avccRawSource] builder.
 * methods.
 *
 * @param nalu the NAL unit [RawSource] (without AVCC or AnnexB header)
 * @param naluSize the size of the NAL unit
 */
private fun avccRawSource(
    nalu: RawSource, naluSize: Int
) = RawSourceWithSize(
    MultiRawSource(Buffer().apply { writeInt(naluSize) }, nalu),
    naluSize + AVCC_HEADER_SIZE.toLong()
)
