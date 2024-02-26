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
package io.github.thibaultbee.krtmp.flv.extensions

import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source

internal fun Sink.writeByte(int: Int) = writeByte(int.toByte())

internal fun Sink.writeShort(int: Int) = writeShort(int.toShort())

internal fun Sink.writeShort(long: Long) = writeShort(long.toShort())

internal fun Sink.writeLong48(long: Long) {
    writeShort(long shr 32)
    writeInt(long.toInt())
}

/**
 * Removes emulation prevention bytes from the source
 */
val Source.rbsp: Buffer
    get() {
        val rbsp = Buffer()

        while (request(1)) {
            val byte1 = readByte()
            if (byte1 == 0.toByte()) {
                if (request(1)) {
                    val byte2 = readByte()
                    if (byte2 == 0.toByte()) {
                        if (request(1)) {
                            val byte3 = readByte()
                            if (byte3 == 3.toByte()) {
                                rbsp.writeByte(byte1)
                                rbsp.writeByte(byte2)
                            } else {
                                rbsp.writeByte(byte1)
                                rbsp.writeByte(byte2)
                                rbsp.writeByte(byte3)
                            }
                        } else {
                            rbsp.writeByte(byte1)
                            rbsp.writeByte(byte2)
                        }
                    } else {
                        rbsp.writeByte(byte1)
                        rbsp.writeByte(byte2)
                    }
                } else {
                    rbsp.writeByte(byte1)
                }
            } else {
                rbsp.writeByte(byte1)
            }
        }
        return rbsp
    }


/**
 * Gets start code size of a [Source].
 */
internal val Source.startCodeSize: Int
    get() {
        if (!request(4)) {
            return 0
        }

        val startBuffer = peek()
        val startCode = startBuffer.readInt()
        return when {
            startCode == 0x00000001 -> 4
            (startCode and 0xFFFFFF00.toInt()) shr 8 == 0x000001 -> 3
            else ->
                0 // No start code
        }
    }

/**
 * Whether the source is in AVCC format
 */
internal val Buffer.isAvcc: Boolean
    get() {
        if (!request(4)) {
            return false
        }

        val startBuffer = peek()

        val avccSize = startBuffer.readInt()
        return avccSize == size.toInt() - Int.SIZE_BYTES
    }
