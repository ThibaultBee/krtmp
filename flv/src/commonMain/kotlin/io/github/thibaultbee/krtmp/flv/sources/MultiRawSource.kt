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

/**
 * Creates a [MultiRawSource] from a single [RawSource].
 */
internal fun MultiRawSource(vararg sources: RawSource) =
    MultiRawSource(sources.toList())

/**
 * A [RawSource] that reads from multiple [RawSource]s in sequence.
 *
 * @param sources List of [RawSource] to read from.
 */
internal class MultiRawSource(private val sources: List<RawSource>) : RawSource {
    private var currentSourceIndex = 0
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        var readSize = 0L
        // All sources have been read
        if (currentSourceIndex == sources.size) {
            return -1
        }
        while (readSize < byteCount) {
            val read = sources[currentSourceIndex].readAtMostTo(sink, byteCount - readSize)
            if (read == -1L) {
                currentSourceIndex++
                if (currentSourceIndex == sources.size) {
                    break
                } else {
                    continue
                }
            }

            readSize += read
            if (readSize == byteCount) {
                break
            }

            currentSourceIndex++
            if (currentSourceIndex == sources.size) {
                break
            }
        }
        return readSize
    }

    /**
     * Closes all [RawSource]s.
     */
    override fun close() {
        sources.forEach { it.close() }
    }
}
