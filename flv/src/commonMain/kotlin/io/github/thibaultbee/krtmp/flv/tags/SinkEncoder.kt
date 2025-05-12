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
package io.github.thibaultbee.krtmp.flv.tags

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.readByteArray


/**
 * Interface a [Sink] encoder
 */
interface SinkEncoder {
    /**
     * The size of the data in bytes.
     */
    val size: Int

    /**
     * Encodes the data into the specified output stream.
     *
     * @param output The output stream to write the encoded data to.
     */
    fun encode(output: Sink)
}

/**
 * Reads the tag as a [Buffer].
 *
 * @return the [Buffer] containing the tag
 */
fun SinkEncoder.readBuffer(): Buffer {
    return Buffer().apply { encode(this) }
}

/**
 * Reads the tag as a [ByteArray].
 *
 * @return the [ByteArray] containing the tag
 */
fun SinkEncoder.readByteArray(): ByteArray {
    return readBuffer().readByteArray()
}