/*
 * Copyright (C) 2022 Thibault B.
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

import io.github.thibaultbee.krtmp.flv.util.extensions.toInt
import kotlinx.io.Sink
import kotlinx.io.Source

class FLVHeader(val hasAudio: Boolean, val hasVideo: Boolean) {
    fun encode(output: Sink) {
        output.writeByte(0x46) // 'F'
        output.writeByte(0x4C) // 'L'
        output.writeByte(0x56) // 'V'
        output.writeByte(0x01) // Version
        output.writeByte((hasVideo.toInt() or (hasAudio.toInt() shl 2)).toByte())
        output.writeInt(DATA_OFFSET)
    }

    companion object {
        private const val DATA_OFFSET = 9

        fun decode(input: Source): FLVHeader {
            require(input.request(DATA_OFFSET.toLong())) { "Not enough data to read FlvHeader" }
            require(input.readByte() == 0x46.toByte()) { "Invalid FlvHeader. Expected F." }
            require(input.readByte() == 0x4C.toByte()) { "Invalid FlvHeader. Expected L." }
            require(input.readByte() == 0x56.toByte()) { "Invalid FlvHeader. Expected V." }

            require(input.readByte() == 0x01.toByte()) { "Invalid FlvHeader. Expected version 1 but." }
            val byte = input.readByte().toInt()
            val hasAudio = (byte and 0x04) != 0
            val hasVideo = (byte and 0x01) != 0

            require(input.readInt() == DATA_OFFSET) { "Invalid FlvHeader. Expected DATA_OFFSET to be $DATA_OFFSET." }

            return FLVHeader(hasAudio, hasVideo)
        }
    }
}