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
package io.github.thibaultbee.krtmp.rtmp.util

import io.github.thibaultbee.krtmp.rtmp.messages.Message

/**
 * A utils class to store previous messages (write and read)
 */
internal class MessageHistory {
    private val chunkStreamToMessageMap = mutableMapOf<Int, Message>()

    /**
     * Get the previous written message for the provided [message].
     *
     * @param message The message to write
     * @param onPreviousMessage The callback with the previous message
     */
    suspend fun get(
        message: Message,
        onPreviousMessage: suspend Message?.() -> Unit
    ) {
        val previousMessage = chunkStreamToMessageMap[message.chunkStreamId]
        chunkStreamToMessageMap[message.chunkStreamId] = message
        previousMessage.onPreviousMessage()
    }

    /**
     * Get the previous read message using the provided [onPreviousMessages] function.
     *
     * @param onPreviousMessages A function that takes the current map of chunk stream IDs to messages
     * and returns the previous message.
     * @return The previous message as determined by the [onPreviousMessages] function.
     */
    suspend fun get(onPreviousMessages: suspend Map<Int, Message>.() -> Message): Message {
        val message = chunkStreamToMessageMap.onPreviousMessages()
        chunkStreamToMessageMap[message.chunkStreamId] = message
        return message
    }

    /**
     * Get the previous message.
     *
     * @param chunkStreamId The chunk stream ID to get the previous message for.
     * @return The previous message or null if there is none.
     */
    fun get(chunkStreamId: Int): Message? {
        return chunkStreamToMessageMap[chunkStreamId]
    }

    /**
     * Put a message in the history.
     *
     * @param message The message to put.
     */
    fun put(message: Message) {
        chunkStreamToMessageMap[message.chunkStreamId] = message
    }

    fun clear() {
        chunkStreamToMessageMap.clear()
    }
}