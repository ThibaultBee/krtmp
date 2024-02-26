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

import io.github.thibaultbee.krtmp.rtmp.chunk.ChunkStreamId
import kotlinx.io.Buffer

fun UserControl(timestamp: Int, payload: Buffer) = UserControl(
    timestamp,
    UserControl.EventType.from(payload.readShort()),
    Buffer().apply { payload.readAtMostTo(this, payload.size - Short.SIZE_BYTES) })

class UserControl(timestamp: Int, val eventType: EventType, val data: Buffer) :
    Message(
        chunkStreamId = ChunkStreamId.PROTOCOL_CONTROL.value,
        messageStreamId = MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp = timestamp,
        messageType = MessageType.USER_CONTROL,
        payload = Buffer().apply {
            writeShort(eventType.value)
            write(data, data.size)
        }
    ) {

    enum class EventType(val value: Short) {
        STREAM_BEGIN(0x0),
        STREAM_EOF(0x1),
        STREAM_DRY(0x2),
        SET_BUFFER_LENGTH(0x3),
        STREAM_IS_RECORDED(0x4),
        PING_REQUEST(0x6),
        PING_RESPONSE(0x7),
        STREAM_BUFFER_EMPTY(0x1F),
        STREAM_BUFFER_READY(0x20);

        companion object {
            fun from(value: Short): EventType = entries.first { it.value == value }
        }
    }
}
