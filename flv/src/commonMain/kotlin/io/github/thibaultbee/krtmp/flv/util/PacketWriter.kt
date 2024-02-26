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
package io.github.thibaultbee.krtmp.flv.util

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.readByteArray

/**
 * A convenient class for output format.
 *
 * It will transform the packet to the desired format (ByteArray, Buffer, Sink, RawSource).
 */
abstract class PacketWriter {
    /**
     * Writes the frame to a [Sink].
     *
     * @param output the [Sink] to write to
     */
    abstract fun write(output: Sink)
}

/**
 * Reads the frame as a [Buffer].
 *
 * @return the [Buffer] containing the frame
 */
fun PacketWriter.readBuffer(): Buffer {
    return Buffer().apply { write(this) }
}

/**
 * Reads the frame as a [ByteArray].
 *
 * @return the [ByteArray] containing the frame
 */
fun PacketWriter.readByteArray(): ByteArray {
    return readBuffer().readByteArray()
}