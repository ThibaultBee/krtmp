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
import io.github.thibaultbee.krtmp.flv.sources.ByteArrayRawSource
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.RawFLVTag
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnection
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnectionCallback
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpSettings
import io.github.thibaultbee.krtmp.rtmp.connection.write
import io.github.thibaultbee.krtmp.rtmp.extensions.clientHandshake
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObjectBuilder
import io.github.thibaultbee.krtmp.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLBuilder
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.SocketFactory
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source

/**
 * Creates a new [RtmpClient] with the given URL string and settings.
 *
 * @param urlString the RTMP URL to connect to
 * @param callback the callback to handle RTMP client events
 * @param settings the settings for the RTMP client
 * @return a new [RtmpClient] instance
 */
suspend fun RtmpClient(
    urlString: String,
    callback: DefaultRtmpClientCallback = DefaultRtmpClientCallback(),
    settings: RtmpSettings = RtmpSettings
) =
    RtmpClient(RtmpURLBuilder(urlString), callback, settings)

/**
 * Creates a new [RtmpClient] with the given URL and settings.
 *
 * @param url the RTMP URL to connect to
 * @param callback the callback to handle RTMP client events
 * @param settings the settings for the RTMP client
 * @return a new [RtmpClient] instance
 */
suspend fun RtmpClient(
    url: Url,
    callback: DefaultRtmpClientCallback = DefaultRtmpClientCallback(),
    settings: RtmpSettings = RtmpSettings
) =
    RtmpClient(RtmpURLBuilder(url), callback, settings)

/**
 * Creates a new [RtmpClient] with the given [URLBuilder] and settings.
 *
 * Use [RtmpURLBuilder] to create the [URLBuilder].
 *
 * @param urlBuilder the [URLBuilder] to connect to
 * @param callback the callback to handle RTMP client events
 * @param settings the settings for the RTMP client
 * @return a new [RtmpClient] instance
 */
suspend fun RtmpClient(
    urlBuilder: URLBuilder,
    callback: RtmpClientCallback = DefaultRtmpClientCallback(),
    settings: RtmpSettings = RtmpSettings,
): RtmpClient {
    val connection = SocketFactory().connect(urlBuilder)
    try {
        connection.clientHandshake(settings.clock)
    } catch (t: Throwable) {
        connection.close()
        throw t
    }
    return RtmpClient(connection, callback, settings)
}

internal fun RtmpClient(
    connection: ISocket,
    callback: RtmpClientCallback,
    settings: RtmpSettings
): RtmpClient {
    return RtmpClient(
        RtmpConnection(
            connection,
            settings,
            RtmpClientConnectionCallback.Factory(callback),
        )
    )
}

/**
 * The RTMP client.
 */
class RtmpClient internal constructor(
    private val connection: RtmpConnection
) :
    CoroutineScope by connection {
    /**
     * Whether the connection is closed.
     */
    val isClosed: Boolean
        get() = connection.isClosed

    /**
     * Connects to the server.
     *
     * @param block a block to configure the [ConnectObjectBuilder]
     * @return the [Command.Result] send by the server
     */
    suspend fun connect(block: ConnectObjectBuilder.() -> Unit = {}) =
        connection.connect(block)

    /**
     * Creates a stream.
     *
     * @return the [Command.Result] send by the server
     *
     * @see [deleteStream]
     */
    suspend fun createStream() = connection.createStream()

    /**
     * Publishes the stream.
     *
     * @param type the publish type
     * @return the [Command.OnStatus] send by the server
     */
    suspend fun publish(
        type: StreamPublishType = StreamPublishType.LIVE
    ) = connection.publish(type)

    /**
     * Deletes the stream.
     *
     * @return the [Command.Result] send by the server
     */
    suspend fun deleteStream() = connection.deleteStream()

    /**
     * Closes the connection and cleans up resources.
     */
    suspend fun close() = connection.close()

    /**
     * Writes the SetDataFrame from [OnMetadata.Metadata].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param metadata the on metadata to send
     */
    suspend fun writeSetDataFrame(metadata: OnMetadata.Metadata) =
        connection.writeSetDataFrame(metadata)

    /**
     * Writes the SetDataFrame from a [ByteArray].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     */
    suspend fun writeSetDataFrame(onMetadata: ByteArray) = connection.writeSetDataFrame(
        ByteArrayRawSource(onMetadata), onMetadata.size
    )

    /**
     * Writes the SetDataFrame from a [Buffer].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     */
    suspend fun writeSetDataFrame(onMetadata: RawSource, size: Int) =
        connection.writeSetDataFrame(onMetadata, size)

    /**
     * Writes an audio frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param array the audio frame to write
     */
    suspend fun writeAudio(timestamp: Int, array: ByteArray) =
        connection.writeAudio(timestamp, array)

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param array the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, array: ByteArray) =
        connection.writeVideo(timestamp, array)

    /**
     * Writes an audio frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the audio frame to write
     */
    suspend fun writeAudio(timestamp: Int, source: RawSource, sourceSize: Int) =
        connection.writeAudio(timestamp, source, sourceSize)

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, source: RawSource, sourceSize: Int) =
        connection.writeVideo(timestamp, source, sourceSize)

    /**
     * Writes a raw audio, video or script frame from a [Source].
     *
     * The frame must be in the FLV format.
     *
     * @param source the frame to write
     */
    suspend fun write(source: Source) = connection.write(source)

    /**
     * Writes a [FLVData].
     *
     * @param timestampMs the timestamp of the frame in milliseconds
     * @param data the frame to write
     */
    suspend fun write(timestampMs: Int, data: FLVData) = connection.write(timestampMs, data)

    /**
     * Writes a [FLVTag].
     *
     * @param tag the FLV tag to write
     */
    suspend fun write(tag: FLVTag) = connection.write(tag)

    /**
     * Writes a [RawFLVTag].
     *
     * @param rawTag the FLV tag to write
     */
    suspend fun write(rawTag: RawFLVTag) = connection.write(rawTag)

    /**
     * Writes a custom [Command].
     *
     * @param command the command to write
     */
    suspend fun write(command: Command) = connection.writeAmfMessage(command)
}

internal class RtmpClientConnectionCallback(
    private val socket: RtmpConnection,
    private val callback: RtmpClientCallback
) : RtmpConnectionCallback {
    override suspend fun onMessage(message: Message) {
        callback.onMessage(message)
    }

    override suspend fun onCommand(command: Command) {
        callback.onCommand(command)
    }

    override suspend fun onData(data: DataAmf) {
        callback.onData(data)
    }

    class Factory(private val callback: RtmpClientCallback) : RtmpConnectionCallback.Factory {
        override fun create(streamer: RtmpConnection): RtmpConnectionCallback {
            return RtmpClientConnectionCallback(streamer, callback)
        }
    }
}

class DefaultRtmpClientCallback : RtmpClientCallback {
    override suspend fun onMessage(message: Message) {
        KrtmpLogger.i(TAG, "Received message: $message")
    }

    override suspend fun onCommand(command: Command) {
        KrtmpLogger.i(TAG, "Received command: $command")
    }

    override suspend fun onData(data: DataAmf) {
        KrtmpLogger.i(TAG, "Received data: $data")
    }

    companion object {
        /**
         * Default instance of [DefaultRtmpClientCallback].
         */
        private const val TAG = "DefaultRtmpClientCallback"
    }
}

/**
 * Callback interface for RTMP client events.
 */
interface RtmpClientCallback {
    /**
     * Called when a message is received.
     *
     * @param message the received message
     */
    suspend fun onMessage(message: Message)

    /**
     * Called when a command is received.
     *
     * @param command the received command
     */
    suspend fun onCommand(command: Command)

    /**
     * Called when data is received.
     *
     * @param data the received data
     */
    suspend fun onData(data: DataAmf)
}