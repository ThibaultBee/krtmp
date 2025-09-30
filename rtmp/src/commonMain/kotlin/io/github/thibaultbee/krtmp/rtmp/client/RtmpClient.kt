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

import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.FLVTagRawBody
import io.github.thibaultbee.krtmp.flv.tags.script.Metadata
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnection
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpConnectionCallback
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpSettings
import io.github.thibaultbee.krtmp.rtmp.connection.write
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObjectBuilder
import io.github.thibaultbee.krtmp.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.krtmp.rtmp.util.NetConnectionConnectCodeReconnect
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import io.ktor.network.sockets.ASocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source

/**
 * Creates an RTMP client.
 *
 * @param connection the socket connection to use
 * @param settings the RTMP settings to use
 * @param callback the callback to handle RTMP events
 * @return the created [RtmpClient]
 */
internal fun RtmpClient(
    connection: ISocket,
    settings: RtmpSettings,
    callback: RtmpClientCallback
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
    CoroutineScope by connection, ASocket by connection {
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
    internal suspend fun connect(block: ConnectObjectBuilder.() -> Unit = {}) =
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
    override fun close() = connection.close()

    /**
     * Writes the SetDataFrame from [Metadata].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param metadata the on metadata to send
     * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
     */
    suspend fun writeSetDataFrame(metadata: Metadata, timestampMs: Int = 0) =
        connection.writeSetDataFrame(metadata, timestampMs)

    /**
     * Writes the SetDataFrame from a [Buffer].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     * @param onMetadataSize the size of the on metadata
     * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
     */
    suspend fun writeSetDataFrame(
        onMetadata: RawSource,
        onMetadataSize: Int,
        timestampMs: Int = 0
    ) =
        connection.writeSetDataFrame(onMetadata, onMetadataSize, timestampMs)

    /**
     * Writes an audio frame from a [RawSource] and its size.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param source the audio frame to write
     * @param sourceSize the size of the audio frame
     * @param timestampMs the timestamp of the frame in milliseconds
     */
    suspend fun writeAudio(source: RawSource, sourceSize: Int, timestampMs: Int) =
        connection.writeAudio(source, sourceSize, timestampMs)

    /**
     * Writes a video frame from a [RawSource] and its size.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param source the video frame to write
     * @param sourceSize the size of the video frame
     * @param timestampMs the timestamp of the frame in milliseconds
     */
    suspend fun writeVideo(source: RawSource, sourceSize: Int, timestampMs: Int) =
        connection.writeVideo(source, sourceSize, timestampMs)

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
     * @param data the frame to write
     * @param timestampMs the timestamp of the frame in milliseconds
     */
    suspend fun write(data: FLVData, timestampMs: Int) = connection.write(data, timestampMs)

    /**
     * Writes a [FLVTag].
     *
     * @param tag the FLV tag to write
     */
    suspend fun write(tag: FLVTag) = connection.write(tag)

    /**
     * Writes a [FLVTagRawBody].
     *
     * @param rawTag the FLV tag to write
     */
    suspend fun write(rawTag: FLVTagRawBody) = connection.write(rawTag)

    /**
     * Writes a custom [Command].
     *
     * @param command the command to write
     */
    suspend fun write(command: Command) = connection.writeAmfMessage(command)

    override fun dispose() {
        try {
            close()
        } catch (_: Throwable) {
        }
    }
}

/**
 * Writes the SetDataFrame from a [ByteArray].
 * It must be called after [publish] and before sending audio or video frames.
 *
 * Expected AMF format is the one set in [RtmpSettings.amfVersion].
 *
 * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
 * @param onMetadata the on metadata to send
 */
suspend fun RtmpClient.writeSetDataFrame(timestampMs: Int, onMetadata: ByteArray) =
    writeSetDataFrame(
        timestampMs = timestampMs,
        onMetadata = ByteArrayBackedRawSource(onMetadata),
        onMetadataSize = onMetadata.size
    )

/**
 * Writes an audio frame from a [ByteArray].
 *
 * The frame must be wrapped in a FLV body.
 *
 * @param array the audio frame to write
 * @param timestampMs the timestamp of the frame in milliseconds
 */
suspend fun RtmpClient.writeAudio(array: ByteArray, timestampMs: Int) =
    writeAudio(ByteArrayBackedRawSource(array), array.size, timestampMs)

/**
 * Writes a video frame from a [ByteArray].
 *
 * The frame must be wrapped in a FLV body.
 *
 * @param array the video frame to write
 * @param timestampMs the timestamp of the frame in milliseconds
 */
suspend fun RtmpClient.writeVideo(array: ByteArray, timestampMs: Int) =
    writeVideo(ByteArrayBackedRawSource(array), array.size, timestampMs)


internal class RtmpClientConnectionCallback(
    private val socket: RtmpConnection,
    private val callback: RtmpClientCallback
) : RtmpConnectionCallback {
    override suspend fun onMessage(message: Message) {
        callback.onMessage(message)
    }

    override suspend fun onCommand(command: Command) {
        try {
            if (command is Command.OnStatus) {
                // Handle reconnect request
                val infoObject = command.decodeInformation()
                if (infoObject.code == NetConnectionConnectCodeReconnect) {
                    // If the command is a reconnect request, notify the callback
                    callback.onReconnectRequest(command)
                    return
                }
            }
        } catch (_: Exception) {
        }
        callback.onCommand(command)
    }

    override suspend fun onData(data: DataAmf) {
        callback.onData(data)
    }

    class Factory(private val callback: RtmpClientCallback) : RtmpConnectionCallback.Factory {
        override fun create(connection: RtmpConnection): RtmpConnectionCallback {
            return RtmpClientConnectionCallback(connection, callback)
        }
    }
}
