package io.github.thibaultbee.krtmp.rtmp.util.connections

import io.github.thibaultbee.krtmp.rtmp.extensions.clientHandshake
import io.github.thibaultbee.krtmp.rtmp.extensions.isTunneledRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.validateRtmp
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.SocketOptions

/**
 * Factory for creating connections.
 *
 * @param socketOptions Options for the TCP socket. Only for RTMP and RTMPS connections.
 */
class ConnectionFactory(
    private val clock: RtmpClock = RtmpClock.Default(),
    private val socketOptions: SocketOptions.TCPClientSocketOptions.() -> Unit = {}
) {
    /**
     * Creates a connection based on the URL scheme.
     *
     * @param urlBuilder The URL builder containing the connection details.
     * @return An instance of [IConnection] for the specified URL scheme.
     */
    suspend fun create(urlBuilder: URLBuilder): IConnection {
        urlBuilder.validateRtmp()
        val connection = if (urlBuilder.protocol.isTunneledRtmp) {
            HttpConnection(urlBuilder)
        } else {
            TcpConnection(urlBuilder, socketOptions = socketOptions)
        }
        connection.connect()
        connection.clientHandshake(clock)
        return connection
    }
}