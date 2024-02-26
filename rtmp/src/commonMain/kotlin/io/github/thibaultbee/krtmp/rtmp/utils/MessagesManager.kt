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
package io.github.thibaultbee.krtmp.rtmp.utils

import io.github.thibaultbee.krtmp.rtmp.messages.Message
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A utils class to store previous messages (write and read)
 */
internal class MessagesManager {
    private val writeMutex = Mutex()
    private val writeChunkStreamMessageMap = mutableMapOf<Int, Message>()

    private val readMutex = Mutex()
    private val readChunkStreamMessageMap = mutableMapOf<Int, Message>()

    /**
     * Get the previous written message for the provided [message].
     *
     * @param message The message to write
     * @param onPreviousMessage The callback with the previous message
     */
    suspend fun getPreviousWrittenMessage(
        message: Message,
        onPreviousMessage: suspend (Message?) -> Unit
    ) {
        writeMutex.withLock {
            val previousMessage = writeChunkStreamMessageMap[message.chunkStreamId]
            onPreviousMessage(previousMessage)
            writeChunkStreamMessageMap[message.chunkStreamId] = message
        }
    }

    suspend fun getPreviousReadMessage(getPreviousMessages: suspend (Map<Int, Message>) -> Message): Message {
        readMutex.withLock {
            val message = getPreviousMessages(readChunkStreamMessageMap)
            readChunkStreamMessageMap[message.chunkStreamId] = message
            return message
        }
    }

    fun clear() {
        runBlocking {
            writeMutex.withLock {
                writeChunkStreamMessageMap.clear()
            }
            readMutex.withLock {
                readChunkStreamMessageMap.clear()
            }
        }
    }
}