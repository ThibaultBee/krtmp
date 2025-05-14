/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.util.connections

import io.github.thibaultbee.krtmp.rtmp.extensions.isSecureRtmp
import io.ktor.http.URLBuilder
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.connection
import io.ktor.network.sockets.isClosed
import io.ktor.network.tls.tls
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.CountedByteReadChannel
import io.ktor.utils.io.CountedByteWriteChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

internal class TcpConnection internal constructor(
    private val urlBuilder: URLBuilder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {},
) : IConnection {
    private val selectorManager = SelectorManager(dispatcher)
    private val tcpSocket = aSocket(selectorManager).tcp()
    private var connection: Connection? = null
    private val input by lazy {
        connection?.let { CountedByteReadChannel(it.input) }
            ?: throw IllegalStateException("Trying to get input without connection")
    }
    private val output by lazy {
        connection?.let { CountedByteWriteChannel(it.output) }
            ?: throw IllegalStateException("Trying to get output without connection")
    }

    override val coroutineContext: CoroutineContext
        get() = connection?.socket?.socketContext
            ?: throw IllegalStateException("Connection is closed")

    override val isClosed: Boolean
        get() = connection?.socket?.isClosed ?: true

    override val totalBytesRead: Long
        get() = input.totalBytesRead

    override val totalBytesWritten: Long
        get() = output.totalBytesWritten

    override suspend fun connect() {
        var socket = tcpSocket.connect(urlBuilder.host, urlBuilder.port, socketOptions)
        if (urlBuilder.protocol.isSecureRtmp) {
            socket = socket.tls(dispatcher)
        }
        connection = socket.connection()
    }

    override suspend fun write(
        length: Long,
        block: suspend (ByteWriteChannel) -> Unit
    ) {
        require(!isClosed) { "Connection is closed" }
        block(output)
        output.flush()
    }

    override suspend fun <T> read(block: suspend (ByteReadChannel) -> T): T {
        require(!isClosed) { "Connection is closed" }
        return block(input)
    }

    override suspend fun close() {
        selectorManager.close()
    }
}