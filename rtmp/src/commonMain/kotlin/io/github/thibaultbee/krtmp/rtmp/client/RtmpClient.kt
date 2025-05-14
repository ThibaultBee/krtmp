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
package io.github.thibaultbee.krtmp.rtmp.client

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.common.logger.Logger
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.krtmp.flv.sources.ByteArrayRawSource
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.RawFLVTag
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.flv.tags.readBuffer
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.rtmp.chunk.Chunk
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClient.Companion.TAG
import io.github.thibaultbee.krtmp.rtmp.extensions.streamKey
import io.github.thibaultbee.krtmp.rtmp.extensions.write
import io.github.thibaultbee.krtmp.rtmp.messages.Acknowledgement
import io.github.thibaultbee.krtmp.rtmp.messages.AmfMessage
import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_AUDIO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_FLASH_VER
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_VIDEO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.CommandMessage
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmfMessage
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.SetChunkSize
import io.github.thibaultbee.krtmp.rtmp.messages.SetPeerBandwidth
import io.github.thibaultbee.krtmp.rtmp.messages.UserControl
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.messages.WindowAcknowledgementSize
import io.github.thibaultbee.krtmp.rtmp.util.MessagesManager
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamCommand
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock
import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLBuilder
import io.github.thibaultbee.krtmp.rtmp.util.TransactionCommandCompletion
import io.github.thibaultbee.krtmp.rtmp.util.connections.ConnectionFactory
import io.github.thibaultbee.krtmp.rtmp.util.connections.IConnection
import io.ktor.http.URLBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.startsWith

/**
 * Creates a new [RtmpClient] instance.
 *
 * It will connect to the RTMP server and perform the handshake.
 * You will need to call [connect] to send a connect message to the server.
 *
 * @param url the RTMP URL to connect to
 * @param settings the settings to use for the client
 */
suspend fun RtmpClient(
    url: String,
    settings: RtmpClient.Settings = RtmpClient.Settings
) = RtmpClient(RtmpURLBuilder(url), settings)

/**
 * Creates a new [RtmpClient] instance.
 *
 * It will connect to the RTMP server and perform the handshake.
 * You will need to call [connect] to send a connect message to the server.
 *
 * @param urlBuilder the RTMP URL builder to connect to
 * @param settings the settings to use for the client
 */
suspend fun RtmpClient(
    urlBuilder: URLBuilder,
    settings: RtmpClient.Settings = RtmpClient.Settings,
): RtmpClient {
    val connection = ConnectionFactory(settings.clock).create(urlBuilder)
    return RtmpClient(urlBuilder, connection, settings)
}

/**
 * A RTMP client to publish stream.
 *
 * The usage is:
 *  - Send RTMP connect command with [connect]
 *  - Send RTMP create stream command with [createStream]
 *  - Send RTMP publish command with [publish]
 *
 * - Send metadata and audio and video frames with [writeSetDataFrame] and [writeAudio] or [writeVideo].
 *
 * - Close the connection with [close]
 */
class RtmpClient(
    private val urlBuilder: URLBuilder,
    private val connection: IConnection,
    private val settings: Settings
) : CoroutineScope by connection {
    private val messagesManager = MessagesManager()

    private var _transactionId = 1L
    private val transactionId: Long
        get() = _transactionId++

    /**
     * The write chunk size.
     *
     * The default value is [RtmpConfiguration.DEFAULT_CHUNK_SIZE].
     *
     * @return the write chunk size
     * @see [setWriteChunkSize]
     */
    var writeChunkSize: Int = settings.writeChunkSize
        private set
    var readChunkSize = RtmpClientSettings.DEFAULT_CHUNK_SIZE
        private set
    var readWindowAcknowledgementSize = Int.MAX_VALUE
        private set
    private var lastReadWindowAcknowledgementSize = Int.MAX_VALUE

    private val commandChannels = TransactionCommandCompletion()

    private var messageStreamId = 0

    override val coroutineContext = connection.coroutineContext

    private val _messageFlow = MutableSharedFlow<Message>()
    val messageFlow = _messageFlow.asSharedFlow()

    init {
        // Launch coroutine to handle RTMP messages
        connection.launch {
            handleRtmpMessages()
        }
    }

    /**
     * Returns true if the connection is closed.
     */
    val isClosed: Boolean
        get() = connection.isClosed

    /**
     * Sets the write chunk size.
     *
     * The default value is [RtmpConfiguration.DEFAULT_CHUNK_SIZE].
     *
     * @param chunkSize the write chunk size
     * @see [getWriteChunkSize]
     */
    suspend fun setWriteChunkSize(chunkSize: Int) {
        if (writeChunkSize != chunkSize) {
            val setChunkSize = SetChunkSize(settings.clock.nowInMs, chunkSize)
            setChunkSize.write()
        }
    }

    /**
     * Connects to the server.
     *
     * @return the [Command.Result] send by the server
     */
    suspend fun connect(connectInformation: ConnectInformation = ConnectInformation): Command.Result {
        // Prepare connect object
        val objectEncoding = if (settings.amfVersion == AmfVersion.AMF0) {
            Command.Connect.ObjectEncoding.AMF0
        } else {
            Command.Connect.ObjectEncoding.AMF3
        }
        val connectObject = Command.Connect.ConnectObject(
            app = urlBuilder.pathSegments[1],
            flashVer = connectInformation.flashVer,
            swfUrl = null,
            tcUrl = urlBuilder.buildString().removeSuffix(urlBuilder.streamKey),
            audioCodecs = connectInformation.audioCodecs,
            videoCodecs = connectInformation.videoCodecs,
            pageUrl = null,
            objectEncoding = objectEncoding
        )

        settings.clock.reset()

        val connectTransactionId = transactionId
        val connectCommand = Command.Connect(
            connectTransactionId, settings.clock.nowInMs, connectObject
        )

        try {
            return connectCommand.writeWithResponse(connectTransactionId) as Command.Result
        } catch (e: RemoteServerException) {
            throw RemoteServerException("Connect command failed: ${e.message}", e.command)
        } catch (e: Exception) {
            throw IOException("Connect command failed", e)
        }
    }

    /**
     * Creates a stream.
     *
     * @return the [Command.Result] send by the server
     *
     * @see [deleteStream]
     */
    suspend fun createStream(): Command.Result {
        val releaseStreamCommand = Command.ReleaseStream(
            transactionId, settings.clock.nowInMs, urlBuilder.streamKey
        )

        val fcPublishCommand = Command.FCPublish(
            transactionId, settings.clock.nowInMs, urlBuilder.streamKey
        )

        val createStreamTransactionId = transactionId
        val createStreamCommand =
            Command.CreateStream(createStreamTransactionId, settings.clock.nowInMs)

        val result = try {
            listOf(
                releaseStreamCommand,
                fcPublishCommand,
                createStreamCommand
            ).writeAmfMessagesWithResponse(createStreamTransactionId)
        } catch (e: RemoteServerException) {
            throw RemoteServerException("Create stream command failed: ${e.message}", e.command)
        } catch (e: Exception) {
            throw IOException("Create stream command failed", e)
        }
        messageStreamId = (result.arguments[0] as AmfNumber).value.toInt()
        return result as Command.Result
    }

    /**
     * Publishes the stream.
     *
     * @param type the publish type
     * @return the [Command.OnStatus] send by the server
     */
    suspend fun publish(type: Command.Publish.Type = Command.Publish.Type.LIVE): Command.OnStatus {
        val publishTransactionId = transactionId
        val publishCommand = Command.Publish(
            messageStreamId,
            publishTransactionId,
            settings.clock.nowInMs,
            urlBuilder.streamKey,
            type
        )
        publishCommand.write()

        return try {
            publishCommand.writeWithResponse(NetStreamCommand.PUBLISH) as Command.OnStatus
        } catch (e: RemoteServerException) {
            throw RemoteServerException("Publish command failed: ${e.message}", e.command)
        } catch (e: Exception) {
            throw IOException("Publish command failed", e)
        }
    }

    /**
     * Write SetDataFrame from [OnMetadata.Metadata].
     * It must be called after [publish] and before [writeFrame], [writeAudio] or [writeVideo].
     *
     * Expected AMF format is the one set in [RtmpClientSettings.amfVersion].
     *
     * @param metadata the on metadata to send
     */
    suspend fun writeSetDataFrame(metadata: OnMetadata.Metadata) {
        val dataFrameDataAmf = DataAmf.SetDataFrame(
            settings.clock.nowInMs,
            messageStreamId,
            metadata
        )
        dataFrameDataAmf.write()
    }

    /**
     * Write SetDataFrame from a [ByteArray].
     * It must be called after [publish] and before [writeFrame], [writeAudio] or [writeVideo].
     *
     * Expected AMF format is the one set in [RtmpClientSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     */
    suspend fun writeSetDataFrame(onMetadata: ByteArray) {
        val dataFrameDataAmf = DataAmfMessage.SetDataFrame(
            settings.amfVersion,
            messageStreamId,
            settings.clock.nowInMs,
            ByteArrayRawSource(onMetadata)
        )
        return dataFrameDataAmf.write()
    }

    /**
     * Write SetDataFrame from a [Buffer].
     * It must be called after [publish] and before [writeFrame], [writeAudio] or [writeVideo].
     *
     * Expected AMF format is the one set in [RtmpClientSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     */
    suspend fun writeSetDataFrame(onMetadata: RawSource) {
        val dataFrameDataAmf = DataAmfMessage.SetDataFrame(
            settings.amfVersion,
            messageStreamId,
            settings.clock.nowInMs,
            onMetadata
        )
        return dataFrameDataAmf.write()
    }

    /**
     * Writes an audio frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param array the audio frame to write
     */
    suspend fun writeAudio(timestamp: Int, array: ByteArray) =
        writeAudio(timestamp, ByteArrayRawSource(array))

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param array the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, array: ByteArray) =
        writeVideo(timestamp, ByteArrayRawSource(array))

    /**
     * Writes an audio frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the audio frame to write
     */
    suspend fun writeAudio(timestamp: Int, source: RawSource) {
        val audio = Audio(timestamp, messageStreamId, source)
        return audio.withTimeoutWriteIfNeeded()
    }

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, source: RawSource) {
        val video = Video(timestamp, messageStreamId, source)
        return video.withTimeoutWriteIfNeeded()
    }

    /**
     * Deletes the stream.
     *
     * @see [createStream]
     */
    suspend fun deleteStream() {
        val deleteStreamCommand = Command.DeleteStream(
            transactionId, settings.clock.nowInMs, urlBuilder.streamKey
        )
        deleteStreamCommand.write()
    }

    /**
     * Closes the connection and cleans up resources.
     */
    suspend fun close() {
        val closeCommand = Command.CloseStream(transactionId, settings.clock.nowInMs)
        closeCommand.write()

        connection.close()
        messagesManager.clear()
    }

    private suspend fun handleRtmpMessages() {
        try {
            while (true) {
                handleRtmpMessage()
            }
        } catch (t: Throwable) {
            commandChannels.completeAllExceptionally(t)
            Logger.i(TAG, "Connection cancelled")
        } finally {
            close()
        }
    }

    private suspend fun handleRtmpMessage() {
        val message = readMessage().apply {
            // Send Acknowledgement message if needed
            val totalBytesRead = connection.totalBytesRead.toInt()
            val readBytes = totalBytesRead - lastReadWindowAcknowledgementSize
            if (readBytes >= readWindowAcknowledgementSize) {
                Acknowledgement(settings.clock.nowInMs, totalBytesRead).write()
                lastReadWindowAcknowledgementSize = totalBytesRead
            }
        }

        when (message) {
            is Acknowledgement -> {
                /**
                 * The server sends Acknowledgement messages to the client every
                 * `writeWindowAcknowledgementSize` bytes send.
                 * We don't do anything with this message.
                 */
            }

            is CommandMessage -> {
                when (val command = Command.read(message)) {
                    is Command.Result -> commandChannels.complete(command.transactionId, command)
                    is Command.Error -> commandChannels.completeExceptionally(
                        command.transactionId, command
                    )

                    is Command.OnStatus -> {
                        val amfObject = command.arguments[0] as AmfObject
                        amfObject["code"]?.let {
                            val code = (it as AmfString).value
                            if (code.startsWith(NetStreamCommand.PUBLISH)) {
                                if (code == NetStreamCommand.PUBLISH_START) {
                                    commandChannels.complete(NetStreamCommand.PUBLISH, command)
                                } else {
                                    commandChannels.completeExceptionally(
                                        NetStreamCommand.PUBLISH, command
                                    )
                                }
                            }
                        }
                    }

                    else -> Unit // Nothing to do
                }
            }

            is SetChunkSize -> {
                readChunkSize = message.chunkSize
            }

            is SetPeerBandwidth -> {
                val windowAcknowledgementSize = WindowAcknowledgementSize(
                    settings.clock.nowInMs, settings.writeWindowAcknowledgementSize
                )
                windowAcknowledgementSize.write()
            }

            is UserControl -> {
                when (message.eventType) {
                    UserControl.EventType.PING_REQUEST -> {
                        val pingResponse = UserControl(
                            settings.clock.nowInMs,
                            UserControl.EventType.PING_RESPONSE,
                            message.data
                        )
                        pingResponse.write()
                    }

                    else -> Unit // Nothing to do
                }
            }

            is WindowAcknowledgementSize -> {
                readWindowAcknowledgementSize = message.windowSize
            }

            else -> {
                _messageFlow.emit(message)
                Logger.e(TAG, "Unknown message type: ${message::class.simpleName}")
            }
        }
    }

    private suspend fun readMessage(): Message {
        return connection.read { readChannel ->
            messagesManager.getPreviousReadMessage { previousMessages ->
                Message.read(readChannel, readChunkSize) { chunkStreamId ->
                    previousMessages[chunkStreamId]
                }
            }
        }
    }

    private suspend fun List<AmfMessage>.writeAmfMessagesWithResponse(id: Any = transactionId): Command {
        writeAmfMessages()
        return commandChannels.waitForResponse(id)
    }

    private suspend fun List<AmfMessage>.writeAmfMessages() {
        val messages = map { it.createMessage(settings.amfVersion) }
        messages.writeMessages()
    }

    private suspend fun AmfMessage.writeWithResponse(id: Any = transactionId): Command {
        val message = createMessage(settings.amfVersion)
        return message.writeWithResponse(id)
    }

    private suspend fun AmfMessage.write() {
        val message = createMessage(settings.amfVersion)
        message.write()
    }

    private suspend fun Message.withTimeoutWriteIfNeeded() {
        if (settings.enableTooLateFrameDrop) {
            val timeoutInMs: Long =
                settings.tooLateFrameDropTimeoutInMs - (settings.clock.nowInMs - timestamp)
            withTimeout(timeoutInMs) {
                this@withTimeoutWriteIfNeeded.write()
            }
        } else {
            write()
        }
    }

    private suspend fun List<Message>.writeMessages() {
        val chunks = mutableListOf<Chunk>()
        forEach { message ->
            messagesManager.getPreviousWrittenMessage(message) { previousMessage ->
                chunks += message.createChunks(writeChunkSize, previousMessage)
            }
        }
        val length = chunks.sumOf { it.size }
        connection.write(length) { writeChannel ->
            chunks.write(writeChannel)
        }
    }

    suspend fun Message.writeWithResponse(id: Any = transactionId): Command {
        write()
        return commandChannels.waitForResponse(id)
    }

    suspend fun Message.write() {
        messagesManager.getPreviousWrittenMessage(this) { previousMessage ->
            val chunks = this.createChunks(writeChunkSize, previousMessage)
            val length = chunks.sumOf { it.size }
            connection.write(length) { writeChannel ->
                chunks.write(writeChannel)
            }
        }
    }

    companion object {
        internal const val TAG = "RtmpClient"
    }

    open class ConnectInformation(
        flashVer: String = DEFAULT_FLASH_VER,
        audioCodecs: List<AudioMediaType>? = DEFAULT_AUDIO_CODECS,
        videoCodecs: List<VideoMediaType>? = DEFAULT_VIDEO_CODECS,
    ) : RtmpClientConnectInformation(flashVer, audioCodecs, videoCodecs) {
        /**
         * The default instance of [ConnectInformation]
         */
        companion object Default : ConnectInformation()
    }

    /**
     * RTMP settings for [RtmpClient].
     *
     * @param enableTooLateFrameDrop enable dropping too late frames. Default is false. It will drop frames if they are are too late if set to true. If enable, make sure frame timestamps are on on the same clock as [clock].
     * @param tooLateFrameDropTimeoutInMs the timeout after which a frame will be dropped (from frame timestamps). Default is 3000ms.
     */
    open class Settings(
        writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
        writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
        amfVersion: AmfVersion = AmfVersion.AMF0,
        clock: RtmpClock = RtmpClock.Default(),
        val enableTooLateFrameDrop: Boolean = false,
        val tooLateFrameDropTimeoutInMs: Long = DEFAULT_TOO_LATE_FRAME_DROP_TIMEOUT_IN_MS,

        ) : RtmpClientSettings(
        writeChunkSize,
        writeWindowAcknowledgementSize,
        amfVersion,
        clock
    ) {
        /**
         * The default instance of [Settings]
         */
        companion object Default : Settings() {
            const val DEFAULT_TOO_LATE_FRAME_DROP_TIMEOUT_IN_MS = 2000L // ms
        }
    }
}


/**
 * Writes a raw frame.
 *
 * The frame must be in the FLV format.
 *
 * Internally, it will parse the frame to extract the header and the body.
 * It is not the most efficient way to write frames but it is convenient.
 * If you have a frame in the FLV format, prefer using [writeAudio] or [writeVideo].
 *
 * @param array the frame to write
 */
suspend fun RtmpClient.write(array: ByteArray) {
    write(Buffer().apply { write(array) })
}

/**
 * Writes a raw frame.
 *
 * The frame must be in the FLV format.
 *
 * @param buffer the frame to write
 */
suspend fun RtmpClient.write(buffer: Buffer) {
    /**
     * Dropping FLV header that is not needed. It starts with 'F', 'L' and 'V'.
     * Just check the first byte to simplify.
     */
    if (buffer.startsWith('F'.code.toByte())) {
        Logger.i(TAG, "Dropping FLV header")
        return
    }
    val tag = RawFLVTag.decode(buffer)
    when (tag.type) {
        FLVTag.Type.AUDIO -> writeAudio(tag.timestampMs, tag.body)
        FLVTag.Type.VIDEO -> writeVideo(tag.timestampMs, tag.body)
        FLVTag.Type.SCRIPT -> writeSetDataFrame(tag.body)
        else -> throw IllegalArgumentException("Frame type ${tag.type} not supported")
    }
}

/**
 * Writes a [FLVData].
 *
 * @param timestampMs the timestamp of the frame in milliseconds
 * @param data the frame to write
 */
suspend fun RtmpClient.write(timestampMs: Int, data: FLVData) {
    return when (data) {
        is AudioData -> writeAudio(
            timestampMs,
            data.readBuffer()
        )

        is VideoData -> writeVideo(
            timestampMs,
            data.readBuffer()
        )

        is OnMetadata -> {
            writeSetDataFrame(data.readBuffer())
        }

        else -> throw IllegalArgumentException("Packet type ${data::class.simpleName} not supported")
    }
}

/**
 * Writes a [FLVTag].
 *
 * @param tag the FLV tag to write
 */
suspend fun RtmpClient.write(tag: FLVTag) = write(tag.timestampMs, tag.data)

