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
package io.github.thibaultbee.krtmp.rtmp.server

import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnection
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnectionCallback
import io.github.thibaultbee.krtmp.rtmp.extensions.serverHandshake
import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CLOSE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CONNECT_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_CREATE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_DELETE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_FCUNPUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PLAY_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_PUBLISH_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Companion.COMMAND_RELEASE_STREAM_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Error
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Result
import io.github.thibaultbee.krtmp.rtmp.messages.CommandOnFCPublish
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf.Companion.DATAAMF_SET_DATA_FRAME_NAME
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.SetPeerBandwidth
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCodePublishFailed
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCodePublishStart
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelError
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelStatus
import io.github.thibaultbee.krtmp.rtmp.util.extensions.startWithScheme
import io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp.TcpSocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.tcp.TcpSocketFactory
import io.ktor.http.Url
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.SocketAddress
import kotlinx.coroutines.job
import kotlin.coroutines.cancellation.CancellationException

suspend fun RtmpServer(
    urlString: String? = null,
    callback: RtmpServerCallback,
    settings: RtmpServerSettings = RtmpServerSettings,
): RtmpServer {
    return if (urlString == null) {
        return RtmpServer(localAddress = null, callback = callback, settings = settings)
    } else {
        val url = if (urlString.startWithScheme()) {
            Url(urlString)
        } else {
            Url("tcp://$urlString")
        }
        RtmpServer(
            InetSocketAddress(url.host, url.port), callback, settings
        )
    }
}

suspend fun RtmpServer(
    localAddress: SocketAddress? = null,
    callback: RtmpServerCallback,
    settings: RtmpServerSettings = RtmpServerSettings,
) = RtmpServer(
    TcpSocketFactory.default.server(localAddress), callback, settings
)

/**
 * The RTMP server.
 */
class RtmpServer internal constructor(
    private val serverSocket: ServerSocket,
    private val callback: RtmpServerCallback,
    private val settings: RtmpServerSettings
) {
    /**
     * Starts the RTMP server and listens for incoming connections.
     */
    suspend fun start() {
        while (serverSocket.socketContext.isActive) {
            try {
                val clientTcpSocket = serverSocket.accept()
                KrtmpLogger.i(TAG, "New client connection: ${clientTcpSocket.remoteAddress}")
                val connection = TcpSocket(clientTcpSocket)
                connection.serverHandshake(settings.clock)

                val rtmpConnection = RtmpConnection(
                    connection,
                    settings,
                    RtmpServerConnectionCallbackImpl.Factory(callback, settings)
                )
                rtmpConnection.coroutineContext.job.join()
            } catch (t: CancellationException) {
                KrtmpLogger.e(TAG, "Cancelling server")
            } catch (t: Throwable) {
                KrtmpLogger.e(TAG, "Error with connection", t)
                KrtmpLogger.i(TAG, "Waiting for new connection")
            }
        }
    }

    /**
     * Stops the RTMP server.
     */
    fun stop() {
        serverSocket.close()
    }

    companion object {
        private const val TAG = "RtmpServer"
    }
}

internal class RtmpServerConnectionCallbackImpl(
    private val socket: RtmpConnection,
    private val callback: RtmpServerCallback,
    private val settings: RtmpServerSettings
) : RtmpConnectionCallback {
    override suspend fun onCommand(command: Command) {
        when (command.name) {
            COMMAND_CONNECT_NAME -> {
                try {
                    callback.onConnect(command)

                    val ackSize = 2_500_000 // TODO
                    val bandwidth = 2_500_000 // TODO
                    val type = SetPeerBandwidth.LimitType.DYNAMIC // TODO
                    KrtmpLogger.i(TAG, "Set window acknowledgement size: $ackSize")
                    KrtmpLogger.i(TAG, "Set peer bandwidth: $bandwidth type: 2")
                    socket.replyConnect(ackSize, bandwidth, type)
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Error(
                            command.messageStreamId, 1, socket.settings.clock.nowInMs, null, null
                        )
                    )
                }
            }

            COMMAND_CREATE_STREAM_NAME -> {
                try {
                    callback.onCreateStream(command)
                    val streamId = settings.streamIdProvider.create()
                    require(streamId > 0) { "Stream ID must be greater than 0" }
                    require((streamId != 2)) { "Stream ID must not be 2, reserved for control messages" }
                    socket.writeAmfMessage(
                        Result(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            AmfNumber(streamId.toDouble())
                        )
                    )
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_RELEASE_STREAM_NAME -> {
                try {
                    callback.onReleaseStream(command)
                    socket.writeAmfMessage(
                        Result(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            AmfNumber(1.0)
                        )
                    )
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_DELETE_STREAM_NAME -> {
                callback.onDeleteStream(command)
                // Delete the stream ID from the provider
                try {
                    require(command.arguments.isNotEmpty()) {
                        "deleteStream command must have at least one argument (stream ID)"
                    }
                    val streamId = (command.arguments[0] as AmfNumber).value.toInt()
                    settings.streamIdProvider.delete(streamId)
                } catch (t: Throwable) {
                    KrtmpLogger.e(TAG, "Error deleting stream ID", t)
                }
            }

            COMMAND_PUBLISH_NAME -> {
                try {
                    require(command.arguments.size >= 2) {
                        "publish command must have at least two arguments (stream key and type)"
                    }
                    val streamKey = command.arguments[0].toString()
                    KrtmpLogger.i(
                        TAG, "Publishing stream: $streamKey for ${command.arguments[1]}"
                    )
                    callback.onPublish(command)
                    socket.writeAmfMessage(
                        Command.OnStatus(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            Command.OnStatus.NetStreamOnStatusInformation(
                                level = NetStreamOnStatusLevelStatus,
                                code = NetStreamOnStatusCodePublishStart,
                                description = "$streamKey is now published"
                            )
                        )
                    )
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Command.OnStatus(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            Command.OnStatus.NetStreamOnStatusInformation(
                                level = NetStreamOnStatusLevelError,
                                code = NetStreamOnStatusCodePublishFailed,
                                description = "Publish failed"
                            )
                        )
                    )
                }
            }

            COMMAND_PLAY_NAME -> {
                try {
                    callback.onPlay(command)
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_FCPUBLISH_NAME -> {
                try {
                    callback.onFCPublish(command)
                    socket.writeAmfMessage(
                        CommandOnFCPublish(command.transactionId, socket.settings.clock.nowInMs)
                    )
                } catch (t: Throwable) {
                    socket.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            command.transactionId,
                            socket.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_FCUNPUBLISH_NAME -> {
                callback.onFCUnpublish(command)
            }

            COMMAND_CLOSE_STREAM_NAME -> {
                callback.onCloseStream(command)
            }

            else -> {
                callback.onUnknownCommandMessage(command)
            }
        }
    }

    override suspend fun onData(data: DataAmf) {
        when (data.name) {
            DATAAMF_SET_DATA_FRAME_NAME -> {
                callback.onSetDataFrame(data)
            }

            else -> {
                callback.onUnknownDataMessage(data)
            }
        }
    }

    override suspend fun onMessage(message: Message) {
        when (message) {
            is Audio -> {
                require(settings.streamIdProvider.hasStreamId(message.messageStreamId)) {
                    "Audio message must have a valid stream ID"
                }
                callback.onAudio(message)
            }

            is Video -> {
                require(settings.streamIdProvider.hasStreamId(message.messageStreamId)) {
                    "Video message must have a valid stream ID"
                }
                callback.onVideo(message)
            }

            else -> {
                callback.onUnknownMessage(message)
            }
        }
    }

    companion object {
        private const val TAG = "RtmpServerCallbackImpl"
    }

    internal class Factory(
        private val callback: RtmpServerCallback, private val settings: RtmpServerSettings
    ) : RtmpConnectionCallback.Factory {
        override fun create(streamer: RtmpConnection): RtmpConnectionCallback =
            RtmpServerConnectionCallbackImpl(streamer, callback, settings)
    }
}

interface RtmpServerCallback {
    fun onConnect(connect: Command) = Unit
    fun onCreateStream(createStream: Command) = Unit
    fun onReleaseStream(releaseStream: Command) = Unit
    fun onDeleteStream(deleteStream: Command) = Unit
    fun onPublish(publish: Command) = Unit
    fun onPlay(play: Command) = Unit
    fun onFCPublish(fcPublish: Command) = Unit
    fun onFCUnpublish(fcUnpublish: Command) = Unit
    fun onCloseStream(closeStream: Command) = Unit
    fun onSetDataFrame(setDataFrame: DataAmf) = Unit
    fun onAudio(audio: Audio) = Unit
    fun onVideo(video: Video) = Unit
    fun onUnknownMessage(message: Message) = Unit
    fun onUnknownCommandMessage(command: Command) = Unit
    fun onUnknownDataMessage(data: DataAmf) = Unit
}
