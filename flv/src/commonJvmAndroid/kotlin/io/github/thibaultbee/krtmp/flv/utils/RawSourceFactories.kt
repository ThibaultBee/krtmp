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
package io.github.thibaultbee.krtmp.flv.utils

import io.github.thibaultbee.krtmp.common.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.flv.util.extensions.isAvcc
import io.github.thibaultbee.krtmp.flv.util.extensions.startCodeSize
import io.github.thibaultbee.krtmp.flv.util.transformToAVCC
import kotlinx.io.RawSource
import java.nio.ByteBuffer

/**
 * Creates a [RawSource] in AVCC format from a [ByteBuffer].
 *
 * @param buffer the [ByteBuffer] to transform
 * @return a pair of [RawSource] in AVCC format and its size (including the AVCC header size)
 */
fun transformToAVCC(buffer: ByteBuffer): Pair<RawSource, Int> {
    if (buffer.isAvcc) {
        return ByteBufferBackedRawSource(buffer) to buffer.remaining()
    }

    // Convert AnnexB start code to AVCC format
    val startCodeSize = buffer.startCodeSize
    buffer.position(buffer.position() + startCodeSize)
    return transformToAVCC(
        ByteBufferBackedRawSource(buffer), buffer.remaining()
    )
}