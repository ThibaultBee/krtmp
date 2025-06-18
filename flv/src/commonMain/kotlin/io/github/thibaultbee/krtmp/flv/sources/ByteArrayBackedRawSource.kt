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
package io.github.thibaultbee.krtmp.flv.sources

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

/**
 * A [RawSource] that reads from a [ByteArray].
 */
class ByteArrayBackedRawSource(
    private val array: ByteArray,
    private val startIndex: Long = 0,
    private val length: Long = array.size.toLong() - startIndex
) :
    RawSource {
    private var position = startIndex

    val isExhausted: Boolean
        get() = position >= (length - 1)

    init {
        require(startIndex >= 0) { "Start index must be a positive value" }
        require(length >= 0) { "Length must be a positive value" }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (byteCount < 0) {
            throw IllegalArgumentException("byteCount < 0: $byteCount")
        }

        if (isExhausted) {
            return -1
        }

        val bytesToRead = min(length - position, position + byteCount)
        sink.write(array, position.toInt(), (position + bytesToRead).toInt())
        position += bytesToRead.toInt()
        return bytesToRead
    }

    override fun close() {
        // Nothing to do
    }
}
