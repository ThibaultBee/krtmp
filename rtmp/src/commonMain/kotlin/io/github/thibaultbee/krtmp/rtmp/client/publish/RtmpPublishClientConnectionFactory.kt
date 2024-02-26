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
package io.github.thibaultbee.krtmp.rtmp.client.publish

import io.github.thibaultbee.krtmp.rtmp.extensions.clientHandshake
import io.github.thibaultbee.krtmp.rtmp.extensions.isSecureRtmp
import io.github.thibaultbee.krtmp.rtmp.extensions.validateRtmp
import io.github.thibaultbee.krtmp.rtmp.utils.RtmpURLBuilder
import io.ktor.http.URLBuilder
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.connection
import io.ktor.network.tls.tls
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * A factory that creates [RtmpPublishClient].
 * @param settings the RTMP settings. By default it creates a configuration for a RTMP client.
 * @param dispatcher the coroutine dispatcher to use for the connection. By default it uses [Dispatchers.IO].
 */
class RtmpPublishClientConnectionFactory(
    private val settings: RtmpPublishClient.Settings = RtmpPublishClient.Settings,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Connects to the server. It establishes a TCP socket connection. You still have to execute
     * [RtmpPublishClient.connect] afterwards.
     *
     * @param url the RTMP url
     * @return a [RtmpPublishClient]
     */
    suspend fun connect(url: String): RtmpPublishClient {
        return connect(RtmpURLBuilder(url))
    }

    /**
     * Connects to the server. It establishes a TCP socket connection. You still have to execute
     * [RtmpPublishClient.connect] afterwards.
     *
     * @param urlBuilder the RTMP url builder
     * @return a [RtmpPublishClient]
     */
    suspend fun connect(
        urlBuilder: URLBuilder
    ): RtmpPublishClient {
        urlBuilder.validateRtmp()

        var socket = aSocket(SelectorManager(dispatcher)).tcp()
            .connect(urlBuilder.host, urlBuilder.port, settings.socketOptions)
        if (urlBuilder.protocol.isSecureRtmp) {
            socket = socket.tls(dispatcher)
        }
        return connect(socket, urlBuilder)
    }

    private suspend fun connect(
        socket: Socket, urlBuilder: URLBuilder
    ): RtmpPublishClient {
        val connection = socket.connection()
        connection.clientHandshake(settings.clock)
        return RtmpPublishClient(
            urlBuilder,
            connection,
            settings
        )
    }
}