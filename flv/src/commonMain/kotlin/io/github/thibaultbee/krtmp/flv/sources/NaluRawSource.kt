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
 * Creates a [NaluRawSource] from a [ByteArray].
 *
 * It will extract the NAL unit by removing header (start code 0x00000001 or AVCC).
 *
 * @param array the [ByteArray] to transform
 */
fun NaluRawSource(array: ByteArray): NaluRawSource {
    if (array.isAvcc) {
        return NaluRawSource(
            ByteArrayRawSource(array, AVCC_HEADER_SIZE.toLong()), array.size - AVCC_HEADER_SIZE
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
        buffer, buffer.size.toInt()
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
        source, byteCount - headerSize
    )
}

/**
 * A [RawSourceWithSize] that represents a NAL unit.
 *
 * The purpose of this class is to simplify the handling of frame data for AVC/H.264, HEVC/H.265.
 *
 * The [RawSourceWithSize] will be in the NAL unit in AVCC format.
 *
 * To convert from other format such as AnnexB format (NAL unit with a start code 0x00000001) or no header, use other [NaluRawSource] builder.
 * methods.
 *
 * @param nalu the NAL unit [RawSource] (without AVCC or AnnexB header)
 * @param naluSize the size of the NAL unit
 */
class NaluRawSource
internal constructor(
    val nalu: RawSource, val naluSize: Int
) : RawSourceWithSize(
    MultiRawSource(listOf(Buffer().apply { writeInt(naluSize) }, nalu)),
    naluSize + AVCC_HEADER_SIZE.toLong()
)
