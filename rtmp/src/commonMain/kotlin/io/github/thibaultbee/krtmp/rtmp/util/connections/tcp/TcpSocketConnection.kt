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
package io.github.thibaultbee.krtmp.rtmp.util.connections.tcp

import io.github.thibaultbee.krtmp.rtmp.util.connections.IConnection
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.connection
import io.ktor.network.sockets.isClosed
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.CountedByteReadChannel
import io.ktor.utils.io.CountedByteWriteChannel
import kotlin.coroutines.CoroutineContext

internal fun TcpSocketConnection(
    socket: Socket
): TcpSocketConnection = TcpSocketConnection(socket.connection())

/**
 * TCP connection implementation of [IConnection].
 */
internal open class TcpSocketConnection(
    private val connection: Connection
) : IConnection {

   override val urlBuilder = URLBuilder(connection.socket.remoteAddress.toString())
    private val input by lazy {
        CountedByteReadChannel(connection.input)
    }
    private val output by lazy {
        CountedByteWriteChannel(connection.output)
    }

    override val coroutineContext: CoroutineContext
        get() = connection.socket.socketContext

    override val isClosed: Boolean
        get() = connection.socket.isClosed

    override val totalBytesRead: Long
        get() = input.totalBytesRead

    override val totalBytesWritten: Long
        get() = output.totalBytesWritten

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
        connection.socket.close()
    }
}