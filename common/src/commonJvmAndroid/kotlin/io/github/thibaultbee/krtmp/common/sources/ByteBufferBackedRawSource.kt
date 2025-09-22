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
package io.github.thibaultbee.krtmp.common.sources

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.write
import java.nio.ByteBuffer

/**
 * A [RawSource] that reads from a [ByteBuffer].
 *
 * @param buffer the [ByteBuffer] to wrap
 */
class ByteBufferBackedRawSource(private val buffer: ByteBuffer) : RawSource {
    private var isClosed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (isClosed) {
            throw IllegalStateException("Source is closed")
        }
        require(byteCount >= 0) { "byteCount must be non-negative" }

        if (!buffer.hasRemaining()) {
            return -1
        }

        val bytesToRead = minOf(byteCount, buffer.remaining().toLong())
        val previousLimit = buffer.limit()
        buffer.limit(buffer.position() + bytesToRead.toInt())

        sink.write(buffer)

        buffer.limit(previousLimit)
        return bytesToRead
    }

    override fun close() {
        isClosed = true
    }
}