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
package io.github.thibaultbee.krtmp.flv.models.sources

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

class ByteArrayRawSource(private val array: ByteArray, startIndex: Long = 0) : RawSource {
    private var position = startIndex
    private val size = array.size.toLong()

    val isExhausted: Boolean
        get() = position >= (size - 1)

    private fun checkBounds(index: Long, byteCount: Long) {
        if (byteCount < 0) {
            throw IllegalArgumentException("byteCount < 0: $byteCount")
        }
        if (index < 0) {
            throw IndexOutOfBoundsException()
        }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        checkBounds(position, byteCount)

        if (isExhausted) {
            return -1
        }

        val clampedByteCount = min(size - position, position + byteCount)
        sink.write(array, position.toInt(), (position + clampedByteCount).toInt())
        position += clampedByteCount.toInt()
        return clampedByteCount
    }

    override fun close() {
        // Nothing to do
    }
}
