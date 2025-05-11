package io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp

import io.github.thibaultbee.krtmp.rtmp.extensions.isSecureRtmp
import io.ktor.http.URLBuilder
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal class TcpSocketFactory(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val selectorManager = SelectorManager(dispatcher)

    suspend fun client(
        urlBuilder: URLBuilder, socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ) = aSocket(selectorManager).tcp()
        .connect(urlBuilder.host, urlBuilder.port, socketOptions).apply {
            if (urlBuilder.protocol.isSecureRtmp) {
                tls(dispatcher)
            }
        }

    suspend fun server(
        urlBuilder: URLBuilder, socketOptions: SocketOptions.AcceptorOptions.() -> Unit = {}
    ) = aSocket(selectorManager).tcp().bind(urlBuilder.host, urlBuilder.port, socketOptions)

    suspend fun server(
        localAddress: SocketAddress?, socketOptions: SocketOptions.AcceptorOptions.() -> Unit = {}
    ) = aSocket(selectorManager).tcp().bind(localAddress, socketOptions)


    fun close() {
        selectorManager.close()
    }

    companion object {
        internal val default = TcpSocketFactory(Dispatchers.IO)
    }
}