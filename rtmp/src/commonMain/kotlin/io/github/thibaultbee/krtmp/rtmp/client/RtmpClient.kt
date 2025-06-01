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

import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.extensions.clientHandshake
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.streamer.ConnectInformation
import io.github.thibaultbee.krtmp.rtmp.streamer.RtmpSettings
import io.github.thibaultbee.krtmp.rtmp.streamer.RtmpSocket
import io.github.thibaultbee.krtmp.rtmp.streamer.RtmpSocketCallback
import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLBuilder
import io.github.thibaultbee.krtmp.rtmp.util.connections.ConnectionFactory
import io.github.thibaultbee.krtmp.rtmp.util.connections.IConnection
import io.ktor.http.URLBuilder

suspend fun RtmpClient(url: String, settings: RtmpSettings = RtmpSettings) =
    RtmpClient(RtmpURLBuilder(url), settings)

suspend fun RtmpClient(
    urlBuilder: URLBuilder,
    settings: RtmpSettings = RtmpSettings,
): RtmpClient {
    val connection = ConnectionFactory().connect(urlBuilder)
    connection.clientHandshake(settings.clock)
    return RtmpClient(connection, settings)
}

internal fun RtmpClient(
    connection: IConnection,
    settings: RtmpSettings
): RtmpClient {
    return RtmpClient(
        RtmpSocket(
            connection,
            settings,
            RtmpClientCallback.Factory(),
        )
    )
}

class RtmpClient internal constructor(private val streamer: RtmpSocket) {
    suspend fun connect(connectInformation: ConnectInformation = ConnectInformation) =
        streamer.connect(connectInformation)

    suspend fun close() =
        streamer.close()
}

internal class RtmpClientCallback(
    private val streamer: RtmpSocket
) : RtmpSocketCallback {
    override suspend fun onMessage(message: Message) {
        KrtmpLogger.i("TAG", "onMessage: $message")
        // TODO
    }

    class Factory : RtmpSocketCallback.Factory {
        override fun create(streamer: RtmpSocket): RtmpSocketCallback {
            return RtmpClientCallback(streamer)
        }
    }
}