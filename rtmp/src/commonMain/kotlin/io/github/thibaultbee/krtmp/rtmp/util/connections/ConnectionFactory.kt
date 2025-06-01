package io.github.thibaultbee.krtmp.rtmp.util.connections

import io.github.thibaultbee.krtmp.rtmp.extensions.isTunneledRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.validateRtmp
import io.github.thibaultbee.krtmp.rtmp.util.connections.http.HttpConnection
import io.github.thibaultbee.krtmp.rtmp.util.connections.tcp.TcpSocketConnection
import io.github.thibaultbee.krtmp.rtmp.util.connections.tcp.TcpSocketFactory
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.SocketOptions

/**
 * Factory for creating connections.
 */
internal class ConnectionFactory(private val tcpSocketFactory: TcpSocketFactory = TcpSocketFactory.default) {
    /**
     * Creates a connection based on the URL scheme.
     *
     * @param urlBuilder The URL builder containing the connection details.
     * @param socketOptions Options for the TCP socket. Only for RTMP and RTMPS connections.
     * @return An instance of [IConnection] for the specified URL scheme.
     */
    suspend fun connect(
        urlBuilder: URLBuilder, socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
    ): IConnection {
        urlBuilder.validateRtmp()
        val connection = if (urlBuilder.protocol.isTunneledRtmp) {
            HttpConnection(urlBuilder)
        } else {
            TcpSocketConnection(tcpSocketFactory.client(urlBuilder, socketOptions))
        }
        return connection
    }
}