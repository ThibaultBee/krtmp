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

import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import kotlinx.io.Buffer

internal fun SetPeerBandwidth(
    timestamp: Int,
    chunkStreamId: Int,
    payload: Buffer
): SetPeerBandwidth =
    SetPeerBandwidth(
        timestamp,
        payload.readInt(),
        PeerBandwidthLimitType.from(payload.readByte()),
        chunkStreamId
    )

internal class SetPeerBandwidth(
    timestamp: Int,
    windowSize: Int,
    limitType: PeerBandwidthLimitType,
    chunkStreamId: Int = ChunkStreamId.PROTOCOL_CONTROL.value
) :
    Message(
        chunkStreamId = chunkStreamId,
        messageStreamId = MessageStreamId.PROTOCOL_CONTROL.value,
        timestamp = timestamp,
        messageType = MessageType.SET_PEER_BANDWIDTH,
        payload = Buffer().apply {
            writeInt(windowSize)
            writeByte(limitType.value)
        }
    )

enum class PeerBandwidthLimitType(val value: Byte) {
    HARD(0x00),
    SOFT(0x01),
    DYNAMIC(0x02);

    companion object Companion {
        fun from(findValue: Byte): PeerBandwidthLimitType = entries.first { it.value == findValue }
    }
}
