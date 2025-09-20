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

import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlin.math.min

/**
 * A [RawSourceWithSize] from a [Buffer].
 *
 * @param buffer the [Buffer] to wrap
 */
fun RawSourceWithSize(buffer: Buffer) =
    RawSourceWithSize(buffer, buffer.size)

/**
 * A [RawSourceWithSize] from a [ByteArray].
 *
 * @param array the [ByteArray] to wrap
 */
fun RawSourceWithSize(array: ByteArray) =
    RawSourceWithSize(ByteArrayBackedRawSource(array), array.size.toLong())

fun RawSourceWithSize(source: RawSource, byteCount: Int) =
    RawSourceWithSize(source, byteCount.toLong())

/**
 * A [RawSource] with a [byteCount].
 *
 * @param source the [RawSource] to wrap.
 * @param byteCount the number of bytes to read in the [source]
 */
open class RawSourceWithSize(private val source: RawSource, val byteCount: Long) :
    RawSource {
    private var bytesRead = 0L

    init {
        require(byteCount >= 0) { "byteCount must be positive" }
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (bytesRead >= this.byteCount) {
            return -1
        }

        val read = source.readAtMostTo(sink, min(byteCount, this.byteCount - bytesRead))
        bytesRead += read

        return read
    }

    override fun close() {
        source.close()
    }
}