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
package io.github.thibaultbee.krtmp.flv.bitreaders

import kotlinx.io.Source

/**
 * A reader for reading bits from a [Source] designed for H26X bitstreams.
 *
 * @param source the source to read from
 */
class H26XBitReader(source: Source) : BitReader(source) {
    fun readUE(): Int {
        val leadingZeros = numOfLeadingZeros()

        return if (leadingZeros > 0) {
            (1 shl leadingZeros) - 1 + readInt(leadingZeros)
        } else {
            0
        }
    }

    fun readSE(): Int {
        val value = readUE()
        val sign = (value and 0x1 shl 1) - 1
        return (value shr 1) + (value and 0x1) * sign
    }

    private fun numOfLeadingZeros(): Int {
        var leadingZeros = 0
        while (!readBoolean()) {
            leadingZeros++
        }
        return leadingZeros
    }
}