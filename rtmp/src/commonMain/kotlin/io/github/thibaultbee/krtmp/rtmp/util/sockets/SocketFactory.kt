package io.github.thibaultbee.krtmp.rtmp.util.sockets

import io.github.thibaultbee.krtmp.rtmp.extensions.isTunneledRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.validateRtmp
import io.github.thibaultbee.krtmp.rtmp.util.sockets.http.HttpSocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp.TcpSocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp.TcpSocketFactory
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.SocketOptions

/**
 * Factory for creating connections.
 */
internal class SocketFactory(private val tcpSocketFactory: TcpSocketFactory = TcpSocketFactory.default) {
    /**
     * Creates a connection based on the URL scheme.
     *
     * @param urlBuilder The URL builder containing the connection details.
     * @param socketOptions Options for the TCP socket. Only for RTMP and RTMPS connections.
     * @return An instance of [ISocket] for the specified URL scheme.
     */
    suspend fun connect(
        urlBuilder: URLBuilder, socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ): ISocket {
        urlBuilder.validateRtmp()
        val connection = if (urlBuilder.protocol.isTunneledRtmp) {
            HttpSocket(urlBuilder)
        } else {
            TcpSocket(tcpSocketFactory.client(urlBuilder, socketOptions), urlBuilder)
        }
        return connection
    }
}