/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.messages.chunk

internal enum class ChunkStreamId(val value: Int) {
    PROTOCOL_CONTROL(0x02), // Mandatory value the following could be dynamic
    COMMAND_CHANNEL(0x03),
    AUDIO_CHANNEL(0x04),
    VIDEO_CHANNEL(0x05);

    companion object {
        /**
         * Get chunk stream ID from value
         *
         * @param value the chunk stream ID value
         * @return the chunk stream ID
         */
        fun entryOf(value: Int): ChunkStreamId {
            return entries.first { it.value == value }
        }
    }

}