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
package io.github.thibaultbee.krtmp.rtmp.client

import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message

/**
 * Callback interface for RTMP client events.
 */
interface RtmpClientCallback {
    /**
     * Called when a message is received.
     *
     * @param message the received message
     */
    suspend fun onMessage(message: Message) = Unit

    /**
     * Called when a command is received.
     *
     * @param command the received command
     */
    suspend fun onCommand(command: Command) = Unit

    /**
     * Called when data is received.
     *
     * @param data the received data
     */
    suspend fun onData(data: DataAmf) = Unit

    /**
     * Called when a reconnect request is received.
     *
     * This is used to handle reconnection logic.
     *
     * @param command the command containing the reconnect request
     */
    suspend fun onReconnectRequest(command: Command.OnStatus) = Unit
}


/**
 * Builder class for creating [RtmpClientCallback] instances with lambda functions.
 */
class RtmpClientCallbackBuilder : RtmpClientCallback {
    private var messageBlock: Message.() -> Unit = {}
    private var commandBlock: Command.() -> Unit = {}
    private var dataBlock: DataAmf.() -> Unit = {}
    private var reconnectRequestBlock: Command.OnStatus.() -> Unit = {}

    fun message(block: Message.() -> Unit) {
        messageBlock = block
    }

    fun command(block: Command.() -> Unit) {
        commandBlock = block
    }

    fun data(block: DataAmf.() -> Unit) {
        dataBlock = block
    }

    fun reconnectRequest(block: Command.OnStatus.() -> Unit) {
        reconnectRequestBlock = block
    }

    override suspend fun onMessage(message: Message) = message.messageBlock()

    override suspend fun onCommand(command: Command) = command.commandBlock()

    override suspend fun onData(data: DataAmf) = data.dataBlock()

    override suspend fun onReconnectRequest(command: Command.OnStatus) =
        command.reconnectRequestBlock()
}