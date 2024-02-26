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
package io.github.thibaultbee.krtmp.rtmp.messages

enum class MessageType(val value: Byte) {
    SET_CHUNK_SIZE(0x01),
    ABORT(0x02),
    ACK(0x03),
    USER_CONTROL(0x04),
    WINDOW_ACK_SIZE(0x05),
    SET_PEER_BANDWIDTH(0x06),
    AUDIO(0x08),
    VIDEO(0x09),
    DATA_AMF3(0x0F),
    SHARED_OBJECT_AMF3(0x10),
    COMMAND_AMF3(0x11),
    DATA_AMF0(0x12),
    SHARED_OBJECT_AMF0(0x13),
    COMMAND_AMF0(0x14),
    AGGREGATE(0x16);

    companion object {
        fun entryOf(value: Byte): MessageType {
            try {
                return entries.first { it.value == value }
            } catch (e: Exception) {
                throw IllegalArgumentException("Unknown MessageType: $value", e)
            }
        }
    }
}