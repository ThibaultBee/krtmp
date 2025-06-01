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

import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.extensions.serverHandshake
import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
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
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.SetPeerBandwidth
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.streamer.RtmpSocket
import io.github.thibaultbee.krtmp.rtmp.streamer.RtmpSocketCallback
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCodePlayStart
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelStatus
import io.github.thibaultbee.krtmp.rtmp.util.connections.tcp.TcpSocketConnection
import io.github.thibaultbee.krtmp.rtmp.util.connections.tcp.TcpSocketFactory
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.ServerSocket
import kotlinx.coroutines.job
import kotlin.coroutines.cancellation.CancellationException

suspend fun RtmpServer(
    url: String,
    callback: RtmpServerCallback,
    settings: RtmpServerSettings = RtmpServerSettings,
) =
    RtmpServer(URLBuilder(url), callback, settings)

suspend fun RtmpServer(
    urlBuilder: URLBuilder,
    callback: RtmpServerCallback,
    settings: RtmpServerSettings = RtmpServerSettings,
): RtmpServer =
    RtmpServer(
        TcpSocketFactory.default.server(urlBuilder),
        callback,
        settings
    )

/**
 * The RTMP server.
 */
class RtmpServer internal constructor(
    private val serverSocket: ServerSocket,
    private val callback: RtmpServerCallback,
    private val settings: RtmpServerSettings
) {
    suspend fun listen() {
        while (serverSocket.socketContext.isActive) {
            try {
                val socket = serverSocket.accept()
                val connection = TcpSocketConnection(socket)
                connection.serverHandshake(settings.clock)
                KrtmpLogger.i(TAG, "New connection: ${connection.urlBuilder}")

                val streamer = RtmpSocket(
                    connection,
                    settings,
                    RtmpServerCallbackImpl.Factory(callback)
                )
                streamer.coroutineContext.job.join()
            } catch (t: CancellationException) {
                KrtmpLogger.e(TAG, "Cancelling server")
            } catch (t: Throwable) {
                KrtmpLogger.e(TAG, "Error with connection", t)
            } finally {
                KrtmpLogger.i(TAG, "Connection closed: waiting for now connection")
            }
        }
    }

    fun close() {
        serverSocket.close()
    }

    companion object {
        private const val TAG = "RtmpServer"
    }
}

internal class RtmpServerCallbackImpl(
    private val streamer: RtmpSocket,
    private val callback: RtmpServerCallback
) : RtmpSocketCallback {
    override suspend fun onCommand(command: Command) {
        when (command.name) {
            COMMAND_CONNECT_NAME -> {
                val result = try {
                    callback.onConnect(command)

                    val ackSize = 2_500_000 // TODO
                    val bandwidth = 2_500_000 // TODO
                    val type = SetPeerBandwidth.LimitType.DYNAMIC // TODO
                    KrtmpLogger.i(TAG, "Set window acknowledgement size: $ackSize")
                    KrtmpLogger.i(TAG, "Set peer bandwidth: $bandwidth type: 2")
                    streamer.replyConnect(ackSize, bandwidth, type)
                } catch (t: Throwable) {
                    streamer.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            1,
                            streamer.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_CREATE_STREAM_NAME -> {
                try {
                    callback.onCreateStream(command)
                    streamer.writeAmfMessage(
                        Result(
                            command.messageStreamId,
                            command.transactionId,
                            streamer.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                } catch (t: Throwable) {
                    streamer.writeAmfMessage(
                        Error(
                            command.messageStreamId,
                            command.transactionId,
                            streamer.settings.clock.nowInMs,
                            null,
                            null
                        )
                    )
                }
            }

            COMMAND_RELEASE_STREAM_NAME -> {
                callback.onReleaseStream(command)
            }

            COMMAND_DELETE_STREAM_NAME -> {
                callback.onDeleteStream(command)
            }

            COMMAND_PUBLISH_NAME -> {
                val streamKey = command.arguments[0].toString()
                KrtmpLogger.i(
                    TAG,
                    "Publishing stream: $streamKey for ${command.arguments[1]}"
                )
                callback.onPublish(command)
                streamer.writeAmfMessage(
                    Command.OnStatus(
                        command.messageStreamId,
                        command.transactionId,
                        streamer.settings.clock.nowInMs,
                        Command.OnStatus.NetStreamOnStatusInformation(
                            level = NetStreamOnStatusLevelStatus,
                            code = NetStreamOnStatusCodePlayStart,
                            description = "$streamKey is now published",
                            details = streamKey
                        )
                    )
                )
            }

            COMMAND_PLAY_NAME -> {
                callback.onPlay(command)
            }

            COMMAND_FCPUBLISH_NAME -> {
                callback.onFCPublish(command)
            }

            COMMAND_FCUNPUBLISH_NAME -> {
                callback.onFCUnpublish(command)
            }

            else -> {
                callback.onUnknownCommandMessage(command)
            }
        }
    }

    override suspend fun onData(data: DataAmf) {
        when (data) {
            is DataAmf.SetDataFrame -> {
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
                callback.onAudio(message)
            }

            is Video -> {
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
        private val callback: RtmpServerCallback
    ) : RtmpSocketCallback.Factory {
        override fun create(streamer: RtmpSocket): RtmpSocketCallback =
            RtmpServerCallbackImpl(streamer, callback)
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
    fun onSetDataFrame(setDataFrame: DataAmf.SetDataFrame) = Unit
    fun onAudio(audio: Audio) = Unit
    fun onVideo(video: Video) = Unit
    fun onUnknownMessage(message: Message) = Unit
    fun onUnknownCommandMessage(command: Command) = Unit
    fun onUnknownDataMessage(data: DataAmf) = Unit
}
