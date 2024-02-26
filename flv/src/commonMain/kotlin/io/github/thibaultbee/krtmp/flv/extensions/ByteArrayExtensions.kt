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


/**
 * Gets start code size of a [ByteArray].
 */
internal val ByteArray.startCodeSize: Int
    get() {
        return if (size >= 4 && this[0] == 0x00.toByte() && this[1] == 0x00.toByte()
            && this[2] == 0x00.toByte() && this[3] == 0x01.toByte()
        ) {
            4
        } else if (size >= 3 && this[0] == 0x00.toByte() && this[1] == 0x00.toByte()
            && this[2] == 0x01.toByte()
        ) {
            3
        } else {
            0
        }
    }

/**
 * Checks if a [ByteArray] is in AVCC format.
 */
internal val ByteArray.isAvcc: Boolean
    get() {
        return if (this.size < 4) {
            false
        } else {
            val length =
                (this[0].toInt() shl 24) or (this[1].toInt() shl 16) or (this[2].toInt() shl 8) or this[3].toInt()
            return length == this.size - 4
        }
    }