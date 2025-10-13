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
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ReadableMessageSocket
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Received RTMP messages through a [ReadableMessageSocket].
 *
 * It also manages the chunk size and message history for proper chunking.
 *
 * @param socket The socket to send messages through.
 */
internal class MessageReadChannel(
    private val socket: ReadableMessageSocket
) {
    private val messageHistory = MessageHistory()
    private val messageMutex = Mutex()

    var chunkSize: Int = RtmpConstants.DEFAULT_CHUNK_SIZE
        private set

    suspend fun read(): Message = messageMutex.withLock {
        val message = socket.read(chunkSize) { chunkStreamId ->
            messageHistory.get(chunkStreamId)
        }
        messageHistory.put(message)
        if (message is SetChunkSize) {
            chunkSize = message.chunkSize
        }
        return message
    }

    fun close() {
        messageHistory.clear()
    }
}