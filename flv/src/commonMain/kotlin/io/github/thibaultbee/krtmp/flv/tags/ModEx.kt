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

import io.github.thibaultbee.krtmp.flv.util.WithValue
import io.github.thibaultbee.krtmp.flv.util.extensions.shl
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import io.github.thibaultbee.krtmp.flv.util.extensions.writeShort
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.experimental.and

open class ModEx<T : WithValue<Byte>>(
    val type: T,
    val size: Int,
    val encode: (Sink) -> Unit
) {
    fun encode(output: Sink, framePacketType: VideoPacketType) {
        val writtenSize = size - 1
        if (writtenSize >= 255) {
            output.writeByte(0xFF.toByte())
            output.writeShort(writtenSize)
        } else {
            output.writeByte((writtenSize).toByte())
        }
        encode(output)
        output.writeByte((type.value shl 4) or framePacketType.value.toInt())
    }

    companion object {
        fun <T : WithValue<Byte>> decode(
            source: Source,
        ): ModEx<T> {
            var modExDataSize = source.readByte() + 1
            if (modExDataSize == 0xFF) {
                modExDataSize = source.readShort() + 1
            }
            val modExData = source.readByteArray(modExDataSize)
            val byte = source.readByte()
            val videoPacketModExType = (byte and 0xF0.toByte()) shl 4
            val framePacketType = VideoPacketType.entryOf(byte and 0x0F.toByte())
            throw NotImplementedError("ModEx decoding not implemented yet")
        }
    }
}

abstract class ModExFactory<T : WithValue<Byte>, U>(val type: T) {
    abstract fun create(value: U): ModEx<T>
}
