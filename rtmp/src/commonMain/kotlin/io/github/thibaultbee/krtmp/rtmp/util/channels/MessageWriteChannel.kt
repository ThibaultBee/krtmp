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
import io.github.thibaultbee.krtmp.rtmp.util.Processable
import io.github.thibaultbee.krtmp.rtmp.util.sockets.WritableMessageSocket
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Send RTMP messages through a [WritableMessageSocket] with a configurable timeout.
 *
 * It uses a [Channel] to queue messages and a coroutine to send them sequentially.
 *
 * It also manages the chunk size and message history for proper chunking.
 *
 * @param receiveChannel The channel to queue messages.
 * @param socket The socket to send messages through.
 * @param getTimeoutInMs A function that returns the timeout in milliseconds for a message. If the function returns null, no timeout is applied.
 */
internal class MessageWriteChannel(
    private val socket: WritableMessageSocket,
    private val getTimeoutInMs: (Message) -> Long?,
    private val receiveChannel: Channel<Processable<Message>> = Channel(Channel.UNLIMITED),
) {
    private val messageHistory = MessageHistory()

    var chunkSize: Int = RtmpConstants.DEFAULT_CHUNK_SIZE
        private set

    init {
        socket.launch {
            receiveChannel.consumeEach { processableMessage ->
                val timeoutInMs = getTimeoutInMs(processableMessage.data)
                try {
                    if (timeoutInMs != null) {
                        withTimeout(timeoutInMs) {
                            writeMessage(processableMessage.data)
                            processableMessage.processed.complete(Unit)
                        }
                    } else {
                        writeMessage(processableMessage.data)
                        processableMessage.processed.complete(Unit)
                    }
                } catch (t: Throwable) {
                    processableMessage.processed.completeExceptionally(t)
                }
            }
        }
    }

    private suspend fun writeMessage(message: Message) {
        val previousMessage = messageHistory.get(message.chunkStreamId)
        messageHistory.put(message)
        socket.write(message, chunkSize, previousMessage)
        if (message is SetChunkSize) {
            chunkSize = message.chunkSize
        }
    }

    suspend fun send(message: Message): Deferred<Unit> {
        val processableMessage = Processable(message)
        receiveChannel.send(processableMessage)
        return processableMessage.processed
    }

    fun close() {
        receiveChannel.cancel()
        messageHistory.clear()
    }
}