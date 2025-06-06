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

internal fun Acknowledgement(timestamp: Int, chunkStreamId: Int, payload: Buffer) =
    Acknowledgement(timestamp, payload.readInt(), chunkStreamId)

internal class Acknowledgement(
    timestamp: Int,
    val sequenceNumber: Int,
    chunkStreamId: Int = ChunkStreamId.PROTOCOL_CONTROL.value
) :
    Message(
        chunkStreamId = chunkStreamId,
        messageStreamId = MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp = timestamp,
        messageType = MessageType.ACK,
        payload = Buffer().apply {
            writeInt(sequenceNumber)
        }
    ) {
    override fun toString(): String {
        return "Acknowledgement(timestamp=$timestamp, sequenceNumber=$sequenceNumber, chunkStreamId=$chunkStreamId)"
    }
}
