package io.github.thibaultbee.krtmp.rtmp.utils.connections

import io.github.thibaultbee.krtmp.rtmp.extensions.isSecureRtmp
import io.github.thibaultbee.krtmp.rtmp.messages.Message
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
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext

class TcpConnection(
    private val urlBuilder: URLBuilder,
    private val dispatcher: CoroutineDispatcher,
    private val socketOptions: SocketOptions.PeerSocketOptions.() -> Unit = {},
) : IConnection {
    private val tcpSocket = aSocket(SelectorManager(dispatcher)).tcp()
    private var connection: Connection? = null

    override val coroutineContext: CoroutineContext
        get() = connection?.socket?.coroutineContext
            ?: throw IllegalStateException("Connection is closed")

    override val isClosed: Boolean
        get() = connection?.socket?.isClosed ?: true

    override val totalBytesRead: Long
        get() = connection?.input?.totalBytesRead ?: 0

    override val totalBytesWritten: Long
        get() = connection?.output?.totalBytesWritten ?: 0

    @OptIn(InternalCoroutinesApi::class)
    override val closedCause: Throwable?
        get() = connection?.socket?.socketContext?.getCancellationException()?.cause

    override fun invokeOnCompletion(handler: CompletionHandler) {
        connection!!.socket.socketContext.invokeOnCompletion(handler)
    }

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
        connection?.output?.let {
            block(it)
            it.flush()
        }
    }

    override suspend fun <T> read(block: suspend (ByteReadChannel) -> T): T {
        require(!isClosed) { "Connection is closed" }
        connection?.input?.let {
            return block(it)
        }
        throw IllegalStateException("Connection is closed")
    }

    override suspend fun close() {
        connection?.socket?.close()
    }
}