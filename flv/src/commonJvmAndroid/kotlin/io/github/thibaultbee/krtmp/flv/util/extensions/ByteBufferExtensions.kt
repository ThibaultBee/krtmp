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
package io.github.thibaultbee.krtmp.flv.util.extensions

import java.nio.ByteBuffer

/**
 * Get start code size of [ByteBuffer].
 */
internal val ByteBuffer.startCodeSize: Int
    get() {
        return if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
            && this.get(2) == 0x00.toByte() && this.get(3) == 0x01.toByte()
        ) {
            4
        } else if (this.get(0) == 0x00.toByte() && this.get(1) == 0x00.toByte()
            && this.get(2) == 0x01.toByte()
        ) {
            3
        } else {
            0
        }
    }

/**
 * Whether [ByteBuffer] is AVCC/HVCC or not.
 * AVCC/HVCC frames start with a the frame size.
 */
internal val ByteBuffer.isAvcc: Boolean
    get() {
        val size = this.getInt(0)
        return size == (this.remaining() - 4)
    }