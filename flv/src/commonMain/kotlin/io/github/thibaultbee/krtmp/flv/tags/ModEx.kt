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
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import io.github.thibaultbee.krtmp.flv.util.extensions.shl
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import io.github.thibaultbee.krtmp.flv.util.extensions.writeShort
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.experimental.and

open class ModEx<T : WithValue<Byte>, V>(val type: T, val value: V)

internal interface ModExCodec<T : WithValue<Byte>, V> {
    val type: T
    val size: Int
    fun encode(output: Sink, value: V)
    fun decode(source: Source): ModEx<T, V>
}

internal class ModExEncoder<T : WithValue<Byte>>(
    private val codec: Set<ModExCodec<T, *>>,
    private val modExPacketType: Byte
) {
    private fun codecOf(type: Byte): ModExCodec<T, *> {
        return codec.firstOrNull { it.type.value == type }
            ?: throw IllegalArgumentException("No codec found for type: $type")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <V> codecOf(type: T): ModExCodec<T, V> {
        return (codec.firstOrNull { it.type == type }
            ?: throw IllegalArgumentException("No codec found for type: $type")) as ModExCodec<T, V>
    }

    /**
     * Gets the size of a set of [ModEx] objects.
     */
    internal fun getSize(
        modExs: Set<ModEx<T, *>>,
    ) = modExs.sumOf { getSize(it) }

    private fun <V> getSize(
        modEx: ModEx<T, V>,
    ): Int {
        val modExSize = codecOf<V>(modEx.type).size
        return modExSize + 1 + if (modExSize >= 255) {
            3
        } else {
            1
        }
    }

    /**
     * Encodes a set of ModEx objects.
     */
    internal fun encode(output: Sink, modExs: Set<ModEx<T, *>>, nextPacketType: Byte) {
        modExs.forEachIndexed { index, modEx ->
            encodeOne(
                output, modEx, if (index == modExs.size - 1) {
                    nextPacketType
                } else {
                    modExPacketType
                }
            )
        }
    }

    /**
     * Encodes a single ModEx object.
     */
    private fun <V> encodeOne(
        output: Sink, modEx: ModEx<T, V>, nextPacketType: Byte
    ) {
        val codec = codecOf<V>(modEx.type)
        val writtenSize = codec.size - 1
        if (writtenSize >= 255) {
            output.writeByte(0xFF.toByte())
            output.writeShort(writtenSize)
        } else {
            output.writeByte((writtenSize).toByte())
        }
        codec.encode(output, modEx.value)
        output.writeByte((modEx.type.value shl 4) or nextPacketType.toInt())
    }

    /**
     * A data class that holds a set of decoded ModEx data and the next packet type.
     */
    data class ModExDatas<T : WithValue<Byte>>(
        val modExs: Set<ModEx<T, *>>,
        val nextPacketType: Byte
    )

    internal fun decode(
        source: Source,
    ): ModExDatas<T> {
        val modExs = mutableSetOf<ModEx<T, *>>()
        var nextPacketType = modExPacketType

        while (nextPacketType == modExPacketType) {
            val modEx = decodeOne(source)
            modExs.add(modEx.modExs)
            nextPacketType = modEx.nextPacketType
        }

        return ModExDatas(modExs, nextPacketType)
    }

    /**
     * A data class that holds a single decoded ModEx data and the next packet type.
     */
    data class ModExData<T : WithValue<Byte>, V>(
        val modExs: ModEx<T, V>,
        val nextPacketType: Byte,
    )

    private fun decodeOne(
        source: Source,
    ): ModExData<T, *> {
        val modExDataSize = source.readByte() + 1
        if (modExDataSize == 0xFF) {
            source.readShort()
        }
        val modExData = source.readSource(modExDataSize.toLong())

        val byte = source.readByte()
        val packetModExType = ((byte and 0xF0.toByte()) shl 4).toByte()
        val codec = codecOf(packetModExType)

        return ModExData(
            codec.decode(modExData),
            (byte and 0x0F)
        )
    }
}
