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
package io.github.thibaultbee.krtmp.rtmp.util.channels

import io.github.thibaultbee.krtmp.rtmp.RtmpConstants
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.SetChunkSize
import io.github.thibaultbee.krtmp.rtmp.util.MessageHistory
import io.github.thibaultbee.krtmp.rtmp.util.sockets.WritableMessageSocket
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Send RTMP messages through a [WritableMessageSocket] with a configurable timeout.
 *
 * It also manages the chunk size and message history for proper chunking.
 *
 * @param socket The socket to send messages through.
 */
internal class MessageWriteChannel(
    private val socket: WritableMessageSocket
) {
    private val messageHistory = MessageHistory()
    private val messageMutex = Mutex()

    var chunkSize: Int = RtmpConstants.DEFAULT_CHUNK_SIZE
        private set

    suspend fun write(message: Message) = messageMutex.withLock {
        val previousMessage = messageHistory.get(message.chunkStreamId)
        messageHistory.put(message)
        socket.write(message, chunkSize, previousMessage)
        if (message is SetChunkSize) {
            chunkSize = message.chunkSize
        }
    }

    fun close() {
        messageHistory.clear()
    }
}