package io.github.thibaultbee.krtmp.rtmp

import io.github.thibaultbee.krtmp.rtmp.client.RtmpClient
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClientCallbackBuilder
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClientSettings
import io.github.thibaultbee.krtmp.rtmp.extensions.clientHandshake
import io.github.thibaultbee.krtmp.rtmp.extensions.isSecureRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.isTunneledRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.validateRtmp
import io.github.thibaultbee.krtmp.rtmp.server.RtmpServer
import io.github.thibaultbee.krtmp.rtmp.server.RtmpServerCallbackBuilder
import io.github.thibaultbee.krtmp.rtmp.server.RtmpServerSettings
import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLBuilder
import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLProtocol
import io.github.thibaultbee.krtmp.rtmp.util.extensions.startWithScheme
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.http.HttpSocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp.TcpSocket
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.aSocket
import io.ktor.network.tls.tls

/**
 * Builder class for creating RTMP connections, both clients and servers.
 *
 * @param selectorManager the [SelectorManager] to use for socket operations
 */
class RtmpConnectionBuilder(val selectorManager: SelectorManager) {
    private val tcpSocketBuilder = aSocket(selectorManager).tcp()

    /**
     * The socket options used for TCP connections.
     */
    val socketOptions: SocketOptions
        get() = tcpSocketBuilder.options

    /**
     * Connects to the given [urlBuilder] and performs the RTMP handshake.
     *
     * The [urlBuilder] must use the `rtmp`, `rtmps`, `rtmpt` or `rtmpts` protocol.
     *
     * @param urlBuilder the URL to connect to
     * @param configure the settings for the RTMP client
     * @param message the callback to handle RTMP client events
     */
    suspend fun connect(
        urlBuilder: URLBuilder,
        configure: RtmpClientSettings.() -> Unit = {},
        message: RtmpClientCallbackBuilder.() -> Unit = {}
    ): RtmpClient {
        urlBuilder.validateRtmp()
        val socket = if (urlBuilder.protocol.isTunneledRtmp) {
            HttpSocket(urlBuilder)
        } else {
            val tcpSocket = tcpSocketBuilder.connect(urlBuilder.host, urlBuilder.port).apply {
                if (urlBuilder.protocol.isSecureRtmp) {
                    tls(selectorManager.coroutineContext)
                }
            }
            TcpSocket(tcpSocket, urlBuilder)
        }

        return connect(socket, configure, message)
    }

    /**
     * Connects to the given [socket] and performs the RTMP handshake.
     */
    private suspend fun connect(
        socket: ISocket,
        configure: RtmpClientSettings.() -> Unit,
        message: RtmpClientCallbackBuilder.() -> Unit
    ): RtmpClient {
        val settings = RtmpClientSettings().apply { configure() }
        try {
            socket.clientHandshake(settings.clock)
        } catch (t: Throwable) {
            socket.close()
            throw t
        }
        val client = RtmpClient(
            socket,
            settings,
            RtmpClientCallbackBuilder().apply { message() }
        )

        try {
            client.connect(settings.connectInfo)
        } catch (t: Throwable) {
            client.close()
            throw t
        }
        return client
    }

    /**
     * Binds a new [RtmpServer] to the given [localAddress].
     *
     * @param localAddress the local address to bind to. If null, binds to a random port on all interfaces.
     * @param configure the settings for the RTMP server
     * @param message the callback to handle RTMP server events
     * @return a new [RtmpServer] instance
     */
    suspend fun bind(
        localAddress: SocketAddress? = null,
        configure: RtmpServerSettings.() -> Unit = {},
        message: RtmpServerCallbackBuilder.() -> Unit = {}
    ): RtmpServer {
        val serverSocket = tcpSocketBuilder.bind(localAddress)

        return bind(serverSocket, configure, message)
    }

    /**
     * Binds a new [RtmpServer] to the given [serverSocket].
     */
    private fun bind(
        serverSocket: ServerSocket,
        settings: RtmpServerSettings.() -> Unit,
        messages: RtmpServerCallbackBuilder.() -> Unit
    ) = RtmpServer(
        serverSocket,
        RtmpServerSettings().apply { settings() },
        RtmpServerCallbackBuilder().apply { messages() }
    )
}

/**
 * Connects to the given [urlString] and performs the RTMP handshake.
 *
 * The [urlString] must use the `rtmp`, `rtmps`, `rtmpt` or `rtmpts` protocol.
 *
 * @param urlString the RTMP URL to connect to
 * @param configure the settings for the RTMP client
 * @param message the callback to handle RTMP client events
 */
suspend fun RtmpConnectionBuilder.connect(
    urlString: String,
    configure: RtmpClientSettings.() -> Unit = {},
    message: RtmpClientCallbackBuilder.() -> Unit = {}
) = connect(RtmpURLBuilder(urlString), configure, message)

/**
 * Binds a new [RtmpServer] to the given [urlString].
 *
 * The [urlString] must be in the format `tcp://host:port` or `host:port`.
 *
 * @param urlString the URL string to bind to
 * @param configure the settings for the RTMP server
 * @param message the callback to handle RTMP server events
 * @return a new [RtmpServer] instance
 */
suspend fun RtmpConnectionBuilder.bind(
    urlString: String,
    configure: RtmpServerSettings.() -> Unit = {},
    message: RtmpServerCallbackBuilder.() -> Unit = {}
): RtmpServer {
    val url = if (urlString.startWithScheme()) {
        Url(urlString)
    } else {
        Url("rtmp://$urlString")
    }
    val localAddress = InetSocketAddress(
        url.host, if (url.specifiedPort == 0) {
            RtmpURLProtocol.createOrDefault(url.protocol.name).defaultPort
        } else {
            url.port
        }
    )
    return bind(localAddress, configure, message)
}