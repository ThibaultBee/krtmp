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
package io.github.thibaultbee.krtmp.flv.models.bitreaders

import kotlinx.io.Source
import kotlin.math.ceil

//TODO: Improve performances
/**
 * A reader for reading bits from a [Source].
 *
 * @param source the source to read from
 */
open class BitReader(private val source: Source) {
    private var currentByte: Byte = source.readByte()
    private var currentPosition: Int = 0
    private var endOfSource = false

    fun require(numBits: Int) {
        require(!endOfSource) { "No bit left to read" }
        val remainingBits = Byte.SIZE_BITS - currentPosition
        if (numBits > remainingBits) {
            val remainingBytes = ceil((numBits - remainingBits) / Byte.SIZE_BITS.toDouble()).toLong()
            source.require(remainingBytes)
        }
    }

    fun request(numBits: Int): Boolean {
        return try {
            require(numBits)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readBoolean() = readLong(1) == 1L

    fun readByte(numBits: Int): Byte {
        require(numBits in 1..8) { "Number of bits must be between 1 and 8" }
        return readInt(numBits).toByte()
    }

    fun readShort(numBits: Int): Short {
        require(numBits in 1..16) { "Number of bits must be between 1 and 16" }
        return readInt(numBits).toShort()
    }

    fun readInt(numBits: Int): Int {
        require(numBits in 1..32) { "Number of bits must be between 1 and 32" }
        return readLong(numBits).toInt()
    }

    fun readLong(numBits: Int): Long {
        require(numBits in 1..64) { "Number of bits must be between 1 and 64" }
        require(!endOfSource) { "No bit left to read" }
        var byteRead = 0
        var result = 0L

        while (byteRead < numBits) {
            val bitsToRead = numBits - byteRead
            val bitsLeftInByte = 8 - currentPosition
            val bitsToReadInByte = if (bitsToRead < bitsLeftInByte) bitsToRead else bitsLeftInByte
            val bitsToShift = bitsLeftInByte - bitsToReadInByte
            val mask = (1 shl bitsToReadInByte) - 1
            val bits = (currentByte.toInt() shr bitsToShift and mask).toLong()
            result = result shl bitsToReadInByte or bits
            byteRead += bitsToReadInByte
            currentPosition += bitsToReadInByte
            if (currentPosition == 8) {
                try {
                    currentByte = source.readByte()
                    currentPosition = 0
                } catch (e: Exception) {
                    if (byteRead < numBits) {
                        throw e
                    } else {
                        endOfSource = true
                        break
                    }
                }
            }
        }

        return result
    }
}