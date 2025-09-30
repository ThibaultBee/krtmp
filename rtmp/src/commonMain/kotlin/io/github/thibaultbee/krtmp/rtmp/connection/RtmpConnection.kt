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
package io.github.thibaultbee.krtmp.rtmp.connection

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.FLVTagRawBody
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.flv.tags.script.Metadata
import io.github.thibaultbee.krtmp.flv.tags.script.ScriptDataObject
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.flv.util.FLVHeader
import io.github.thibaultbee.krtmp.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.RtmpConstants
import io.github.thibaultbee.krtmp.rtmp.extensions.rtmpAppOrNull
import io.github.thibaultbee.krtmp.rtmp.extensions.rtmpStreamKey
import io.github.thibaultbee.krtmp.rtmp.extensions.rtmpTcUrl
import io.github.thibaultbee.krtmp.rtmp.extensions.write
import io.github.thibaultbee.krtmp.rtmp.messages.Acknowledgement
import io.github.thibaultbee.krtmp.rtmp.messages.AmfMessage
import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.CommandCloseStream
import io.github.thibaultbee.krtmp.rtmp.messages.CommandConnect
import io.github.thibaultbee.krtmp.rtmp.messages.CommandCreateStream
import io.github.thibaultbee.krtmp.rtmp.messages.CommandDeleteStream
import io.github.thibaultbee.krtmp.rtmp.messages.CommandFCPublish
import io.github.thibaultbee.krtmp.rtmp.messages.CommandFCUnpublish
import io.github.thibaultbee.krtmp.rtmp.messages.CommandMessage
import io.github.thibaultbee.krtmp.rtmp.messages.CommandNetConnectionResult
import io.github.thibaultbee.krtmp.rtmp.messages.CommandPlay
import io.github.thibaultbee.krtmp.rtmp.messages.CommandPublish
import io.github.thibaultbee.krtmp.rtmp.messages.CommandReleaseStream
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmfMessage
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.MessageStreamId
import io.github.thibaultbee.krtmp.rtmp.messages.PeerBandwidthLimitType
import io.github.thibaultbee.krtmp.rtmp.messages.SetChunkSize
import io.github.thibaultbee.krtmp.rtmp.messages.SetDataFrame
import io.github.thibaultbee.krtmp.rtmp.messages.SetPeerBandwidth
import io.github.thibaultbee.krtmp.rtmp.messages.UserControl
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.messages.WindowAcknowledgementSize
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.Chunk
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObjectBuilder
import io.github.thibaultbee.krtmp.rtmp.messages.command.NetConnectionConnectResultObject
import io.github.thibaultbee.krtmp.rtmp.messages.command.NetConnectionReconnectRequestInformation
import io.github.thibaultbee.krtmp.rtmp.messages.command.ObjectEncoding
import io.github.thibaultbee.krtmp.rtmp.messages.command.StreamPublishType
import io.github.thibaultbee.krtmp.rtmp.messages.createChunks
import io.github.thibaultbee.krtmp.rtmp.util.MessagesManager
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCodePublish
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelError
import io.github.thibaultbee.krtmp.rtmp.util.TransactionCommandCompletion
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import io.ktor.http.Url
import io.ktor.network.sockets.ASocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.readString

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
internal class RtmpConnection internal constructor(
    private val connection: ISocket,
    val settings: RtmpSettings,
    callbackFactory: RtmpConnectionCallback.Factory,
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CoroutineScope by connection, ASocket by connection {
    private val callback by lazy { callbackFactory.create(this) }
    private val messagesManager = MessagesManager()

    private var _transactionId = 1L
    private val transactionId: Long
        get() = _transactionId++

    /**
     * The write chunk size.
     *
     * The default value is [RtmpSettings.DEFAULT_CHUNK_SIZE].
     *
     * @return the write chunk size
     * @see [setWriteChunkSize]
     */
    var writeChunkSize: Int = RtmpSettings.DEFAULT_CHUNK_SIZE
        private set
    var readChunkSize = RtmpSettings.DEFAULT_CHUNK_SIZE
        private set
    var readWindowAcknowledgementSize = Int.MAX_VALUE
        private set
    private var lastReadWindowAcknowledgementSize = Int.MAX_VALUE

    private val commandChannels = TransactionCommandCompletion()

    private var messageStreamId: Int? = null

    override val coroutineContext = connection.coroutineContext

    init {
        // Launch coroutine to handle RTMP messages
        launch {
            handleRtmpMessages()
        }
    }

    /**
     * Whether the connection is closed.
     */
    val isClosed: Boolean
        get() = connection.isClosed

    /**
     * Sets the write chunk size.
     *
     * The default value is [RtmpSettings.DEFAULT_CHUNK_SIZE].
     *
     * @param chunkSize the write chunk size
     * @param timestampMs the timestamp of the SetChunkSize message in milliseconds
     */
    suspend fun setWriteChunkSize(chunkSize: Int, timestampMs: Int = 0) {
        require(chunkSize in RtmpConstants.chunkSizeRange) {
            "Chunk size must be in range ${RtmpConstants.chunkSizeRange}, but was $chunkSize"
        }
        if (writeChunkSize != chunkSize) {
            val setChunkSize = SetChunkSize(timestampMs, chunkSize)
            writeMessage(setChunkSize)
            writeChunkSize = chunkSize
        }
    }

    /**
     * Writes a window acknowledgement size message with the given size.
     *
     * @param size the size of the window acknowledgement
     * @param timestampMs the timestamp of the WindowAcknowledgementSize message in milliseconds
     */
    suspend fun writeWindowAcknowledgementSize(size: Int, timestampMs: Int = 0) {
        val setWindowAcknowledgementSize = WindowAcknowledgementSize(
            timestampMs, size
        )
        writeMessage(setWindowAcknowledgementSize)
    }

    /**
     * Writes a SetPeerBandwidth message with the given size and type.
     *
     * @param size the size of the peer bandwidth
     * @param type the type of the peer bandwidth limit
     * @param timestampMs the timestamp of the SetPeerBandwidth message in milliseconds
     */
    suspend fun writeSetPeerBandwidth(size: Int, type: PeerBandwidthLimitType, timestampMs: Int) {
        val setPeerBandwidth = SetPeerBandwidth(
            timestampMs, size, type
        )
        writeMessage(setPeerBandwidth)
    }

    /**
     * Writes a user control message with the given event type.
     *
     * @param eventType the type of the user control event
     * @param timestampMs the timestamp of the user control event in milliseconds
     */
    suspend fun writeUserControl(
        eventType: UserControl.EventType,
        timestampMs: Int = settings.clock.nowInMs,
    ) {
        val userControl = UserControl(
            timestampMs, eventType
        )
        writeMessage(userControl)
    }


    /**
     * Writes a user control message with the given event type and data.
     *
     * @param eventType the type of the user control event
     * @param data the data to send with the user control event
     * @param timestampMs the timestamp of the user control event in milliseconds
     */
    suspend fun writeUserControl(
        eventType: UserControl.EventType,
        data: Buffer,
        timestampMs: Int = settings.clock.nowInMs
    ) {
        val userControl = UserControl(
            timestampMs, eventType, data
        )
        writeMessage(userControl)
    }

    /**
     * Replies to the connect request with the necessary parameters.
     *
     * @param windowAcknowledgementSize the size of the window acknowledgement
     * @param peerBandwidth the peer bandwidth to set
     * @param peerBandwidthType the type of the peer bandwidth limit
     * @param timestampMs the timestamp of the last message in milliseconds
     */
    suspend fun replyConnect(
        windowAcknowledgementSize: Int,
        peerBandwidth: Int,
        peerBandwidthType: PeerBandwidthLimitType,
        timestampMs: Int = 0
    ) {
        val setWindowAcknowledgementSize = WindowAcknowledgementSize(
            timestampMs, windowAcknowledgementSize
        )

        val setPeerBandwidth = SetPeerBandwidth(
            timestampMs, peerBandwidth, peerBandwidthType
        )

        val userControlStreamBegin = UserControl(
            timestampMs, UserControl.EventType.STREAM_BEGIN
        )

        val result = CommandNetConnectionResult(
            timestampMs,
            NetConnectionConnectResultObject.default,
            if (settings.amfVersion == AmfVersion.AMF0) {
                ObjectEncoding.AMF0
            } else {
                ObjectEncoding.AMF3
            }
        )

        writeMessages(
            listOf(
                setWindowAcknowledgementSize,
                setPeerBandwidth,
                userControlStreamBegin,
                result.createMessage(amfVersion = settings.amfVersion)
            )
        )
    }

    /**
     * Connects to the server.
     *
     * @param block a block to configure the [ConnectObjectBuilder]
     * @return the [Command.Result] send by the server
     */
    internal suspend fun connect(block: ConnectObjectBuilder.() -> Unit = {}): Command.Result {
        // Prepare connect object
        val objectEncoding = if (settings.amfVersion == AmfVersion.AMF0) {
            ObjectEncoding.AMF0
        } else {
            ObjectEncoding.AMF3
        }
        val connectObjectBuilder = ConnectObjectBuilder(
            app = connection.urlBuilder.rtmpAppOrNull ?: "",
            tcUrl = connection.urlBuilder.rtmpTcUrl,
            objectEncoding = objectEncoding
        )
        connectObjectBuilder.block()

        settings.clock.reset()

        val connectTransactionId = transactionId
        val connectCommand = CommandConnect(
            connectTransactionId, settings.clock.nowInMs, connectObjectBuilder.build()
        )

        try {
            return writeAmfMessageWithResponse(
                connectCommand, connectTransactionId
            ) as Command.Result
        } catch (t: Throwable) {
            throw IOException("Connect command failed", t)
        }
    }

    /**
     * Creates a stream.
     *
     * @param timestampMs the timestamp of the create stream command in milliseconds
     * @return the [Command.Result] send by the server
     *
     * @see [deleteStream]
     */
    suspend fun createStream(timestampMs: Int = 0): Command.Result {
        val releaseStreamCommand = CommandReleaseStream(
            transactionId, timestampMs, connection.urlBuilder.rtmpStreamKey
        )

        val fcPublishCommand = CommandFCPublish(
            transactionId, timestampMs, connection.urlBuilder.rtmpStreamKey
        )

        val createStreamTransactionId = transactionId
        val createStreamCommand =
            CommandCreateStream(createStreamTransactionId, timestampMs)

        val result = try {
            writeAmfMessagesWithResponse(
                listOf(
                    releaseStreamCommand, fcPublishCommand, createStreamCommand
                ), createStreamTransactionId
            )
        } catch (t: Throwable) {
            throw IOException("Create stream command failed", t)
        }
        messageStreamId = (result.arguments[0] as AmfNumber).value.toInt()
        return result as Command.Result
    }

    /**
     * Publishes the stream.
     *
     * @param type the publish type
     * @param timestampMs the timestamp of the publish command in milliseconds
     * @return the [Command.OnStatus] send by the server
     */
    suspend fun publish(
        type: StreamPublishType = StreamPublishType.LIVE,
        timestampMs: Int = 0
    ): Command.OnStatus {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val publishTransactionId = transactionId
        val publishCommand = CommandPublish(
            messageStreamId,
            publishTransactionId,
            timestampMs,
            connection.urlBuilder.rtmpStreamKey,
            type
        )

        return try {
            writeAmfMessageWithResponse(
                publishCommand, NetStreamOnStatusCodePublish
            ) as Command.OnStatus
        } catch (t: Throwable) {
            throw IOException("Publish command failed", t)
        }
    }

    /**
     * Plays the stream.
     *
     * @param streamName the name of the stream to play
     * @param timestampMs the timestamp of the play command in milliseconds
     */
    suspend fun play(streamName: String, timestampMs: Int = settings.clock.nowInMs) {
        val playCommand = CommandPlay(
            timestampMs, streamName
        )

        return try {
            writeAmfMessage(playCommand)
        } catch (t: Throwable) {
            throw IOException("Play command failed", t)
        }
    }

    /**
     * Deletes the stream.
     *
     * @param timestampMs the timestamp of the delete stream command in milliseconds
     *
     * @see [createStream]
     */
    suspend fun deleteStream(timestampMs: Int = settings.clock.nowInMs) {
        val messages = mutableListOf(
            CommandFCUnpublish(
                transactionId, timestampMs, connection.urlBuilder.rtmpStreamKey
            )
        )
        messageStreamId?.let {
            messages += CommandDeleteStream(
                transactionId, timestampMs, it
            )
        }

        writeAmfMessages(messages)
    }


    /**
     * Sends a reconnect request to the client.
     *
     * It is a server command.
     *
     * @param tcUrl the URL to reconnect to
     * @param description a description of the reconnect request
     * @param timestampMs the timestamp of the reconnect request in milliseconds
     */
    suspend fun reconnectRequest(
        tcUrl: Url,
        description: String = "The streaming server is undergoing updates.",
        timestampMs: Int = settings.clock.nowInMs
    ) = reconnectRequest(
        tcUrlString = tcUrl.toString(),
        description = description,
        timestampMs
    )

    /**
     * Sends a reconnect request to the client.
     *
     * It is a server command.
     *
     * @param tcUrlString the URL as a String to reconnect to
     * @param description a description of the reconnect request
     */
    suspend fun reconnectRequest(
        tcUrlString: String,
        description: String = "The streaming server is undergoing updates.",
        timestampMs: Int = settings.clock.nowInMs
    ) {
        val onStatus = Command.OnStatus(
            MessageStreamId.PROTOCOL_CONTROL.value,
            transactionId, timestampMs,
            Amf.encodeToAmfElement(
                NetConnectionReconnectRequestInformation.serializer(),
                NetConnectionReconnectRequestInformation(
                    description,
                    tcUrlString
                )
            )
        )
        writeAmfMessage(onStatus)
    }

    private fun close(timestampMs: Int) {
        try {
            val closeCommand = CommandCloseStream(transactionId, timestampMs)
            runBlocking {
                writeAmfMessage(closeCommand)
            }
        } catch (t: Throwable) {
            KrtmpLogger.i(TAG, "Error sending close command: ${t.message}")
        }

        try {
            connection.close()
        } finally {
            commandChannels.completeAllExceptionally(CancellationException(""))
            messagesManager.clear()
        }
    }

    /**
     * Closes the connection and cleans up resources.
     */
    override fun close() {
        close(settings.clock.nowInMs)
    }

    /**
     * Writes the SetDataFrame from [Metadata].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param metadata the on metadata to send
     * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
     */
    suspend fun writeSetDataFrame(metadata: Metadata, timestampMs: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            messageStreamId, timestampMs, metadata
        )
        writeAmfMessage(dataFrameDataAmf)
    }

    /**
     * Writes the SetDataFrame from a [ByteArray].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     * @param timestampMs the timestamp of the metadata in milliseconds  (usually 0)
     */
    suspend fun writeSetDataFrame(onMetadata: ByteArray, timestampMs: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            settings.amfVersion,
            messageStreamId,
            timestampMs,
            ByteArrayBackedRawSource(onMetadata),
            onMetadata.size
        )
        return writeMessage(dataFrameDataAmf)
    }

    /**
     * Writes the SetDataFrame from a [Buffer].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param onMetadata the on metadata to send
     * @param onMetadataSize the size of the metadata
     * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
     * @param amfVersion the AMF version to use
     */
    internal suspend fun writeSetDataFrame(
        onMetadata: RawSource,
        onMetadataSize: Int,
        timestampMs: Int,
        amfVersion: AmfVersion = settings.amfVersion
    ) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            amfVersion, messageStreamId, timestampMs, onMetadata, onMetadataSize
        )
        return writeMessage(dataFrameDataAmf)
    }

    /**
     * Writes an audio frame from a [RawSource] and its size.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param source the audio frame to write
     * @param sourceSize the size of the audio frame
     * @param timestampMs the timestamp of the frame in milliseconds
     */
    suspend fun writeAudio(source: RawSource, sourceSize: Int, timestampMs: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val audio = Audio(timestampMs, messageStreamId, source, sourceSize)
        return withTimeoutWriteIfNeeded(audio)
    }

    /**
     * Writes a video frame from a [RawSource] and its size.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param source the video frame to write
     * @param sourceSize the size of the video frame
     * @param timestampMs the timestamp of the frame in milliseconds
     */
    suspend fun writeVideo(source: RawSource, sourceSize: Int, timestampMs: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val video = Video(timestampMs, messageStreamId, source, sourceSize)
        return withTimeoutWriteIfNeeded(video)
    }

    private suspend fun writeAmfMessagesWithResponse(
        amfMessages: List<AmfMessage>, id: Any = transactionId
    ): Command {
        writeAmfMessages(amfMessages)
        return commandChannels.waitForResponse(id)
    }

    private suspend fun writeAmfMessages(amfMessages: List<AmfMessage>) {
        val messages = amfMessages.map { it.createMessage(settings.amfVersion) }
        writeMessages(messages)
    }

    private suspend fun writeAmfMessageWithResponse(
        amfMessage: AmfMessage, id: Any = transactionId
    ): Command {
        val message = amfMessage.createMessage(settings.amfVersion)
        return writeMessageWithResponse(message, id)
    }

    suspend fun writeAmfMessage(amfMessage: AmfMessage) {
        val message = amfMessage.createMessage(settings.amfVersion)
        writeMessage(message)
    }

    private suspend fun withTimeoutWriteIfNeeded(message: Message) {
        if (settings.enableTooLateFrameDrop) {
            val timeoutInMs: Long =
                settings.tooLateFrameDropTimeoutInMs - (settings.clock.nowInMs - message.timestamp)
            withTimeout(timeoutInMs) {
                writeMessage(message)
            }
        } else {
            writeMessage(message)
        }
    }

    private suspend fun writeMessages(messages: List<Message>) {
        val chunks = mutableListOf<Chunk>()
        messages.forEach { message ->
            messagesManager.getPreviousWrittenMessage(message) { previousMessage ->
                chunks += message.createChunks(writeChunkSize, previousMessage)
            }
        }
        val length = chunks.sumOf { it.size.toLong() }
        connection.write(length) {
            chunks.write(this)
        }
    }

    /**
     * Writes a message to the connection and waits for a response.
     *
     * @param message the message to write
     * @param id the transaction id to wait for the response
     * @return the response command
     */
    suspend fun writeMessageWithResponse(message: Message, id: Any = transactionId): Command {
        writeMessage(message)
        return commandChannels.waitForResponse(id)
    }

    /**
     * Writes the message to the connection.
     *
     * @param message the message to write
     */
    suspend fun writeMessage(message: Message) {
        messagesManager.getPreviousWrittenMessage(message) { previousMessage ->
            val chunks = message.createChunks(writeChunkSize, previousMessage)
            val length = chunks.sumOf { it.size.toLong() }
            connection.write(length) {
                chunks.write(this)
            }
        }
    }

    private suspend fun handleRtmpMessages() {
        try {
            while (isActive) {
                handleMessages()
            }
        } catch (t: Throwable) {
            commandChannels.completeAllExceptionally(t)
            KrtmpLogger.i(TAG, "Failed to handle RTMP message: ${t.message}", t)
        } finally {
            try {
                connection.close()
            } finally {
                messagesManager.clear()
            }
        }
    }

    private suspend fun handleMessages() {
        val message = readMessage().apply {
            // Send Acknowledgement message if needed
            val totalBytesRead = connection.totalBytesRead.toInt()
            val readBytes = totalBytesRead - lastReadWindowAcknowledgementSize
            if (readBytes >= readWindowAcknowledgementSize) {
                writeMessage(Acknowledgement(settings.clock.nowInMs, totalBytesRead))
                lastReadWindowAcknowledgementSize = totalBytesRead
            }
        }

        processMessage(message)
    }

    private suspend fun processMessage(message: Message) {
        when (message) {
            is Acknowledgement -> {
                /**
                 * The server sends Acknowledgement messages to the client every
                 * `writeWindowAcknowledgementSize` bytes send.
                 * We don't do anything with this message.
                 */
            }

            is CommandMessage -> {
                launch {
                    handleCommandMessage(message)
                }
            }

            is SetChunkSize -> {
                if (message.chunkSize in RtmpConstants.chunkSizeRange) {
                    readChunkSize = message.chunkSize
                } else {
                    KrtmpLogger.e(TAG, "Invalid chunk size: ${message.chunkSize}")
                }
            }

            is SetPeerBandwidth -> {
                writeWindowAcknowledgementSize(settings.writeWindowAcknowledgementSize)
            }

            is UserControl -> {
                when (message.eventType) {
                    UserControl.EventType.PING_REQUEST -> {
                        val pingResponse = UserControl(
                            settings.clock.nowInMs,
                            UserControl.EventType.PING_RESPONSE,
                            message.data
                        )
                        writeMessage(pingResponse)
                    }

                    else -> Unit // Nothing to do
                }
            }

            is WindowAcknowledgementSize -> {
                readWindowAcknowledgementSize = message.windowSize
            }

            is DataAmfMessage -> {
                handleDataMessage(message)
            }

            else -> {
                launch(callbackDispatcher) {
                    callback.onMessage(message)
                }
            }
        }
    }

    private fun handleCommandMessage(message: CommandMessage) = launch(callbackDispatcher) {
        when (val command = Command.read(message)) {
            is Command.Result -> commandChannels.complete(command.transactionId, command)
            is Command.Error -> commandChannels.completeExceptionally(
                command.transactionId, command
            )

            is Command.OnStatus -> {
                val amfObject = command.arguments[0] as AmfObject

                val code = (amfObject["code"]!! as AmfString).value
                val commandType = code.substringBeforeLast('.')

                val level = (amfObject["level"]!! as AmfString).value

                if (level == NetStreamOnStatusLevelError) {
                    commandChannels.completeExceptionally(
                        commandType, command
                    )
                } else {
                    commandChannels.complete(commandType, command)
                }
            }

            else -> callback.onCommand(command)
        }
    }

    private fun handleDataMessage(message: DataAmfMessage) {
        launch(callbackDispatcher) {
            callback.onData(DataAmf.read(message))
        }
    }

    private suspend fun readMessage(): Message {
        return connection.read {
            messagesManager.getPreviousReadMessage { previousMessages ->
                Message.read(this, readChunkSize) { chunkStreamId ->
                    previousMessages[chunkStreamId]
                }
            }
        }
    }

    companion object {
        internal const val TAG = "RtmpConnection"
    }

    override fun dispose() {
        try {
            close()
        } catch (_: Throwable) {
        }
    }
}


/**
 * Callback interface for the [RtmpConnection].
 */
internal interface RtmpConnectionCallback {
    /**
     * Called when a command message is received.
     *
     * @param command the command message
     */
    suspend fun onCommand(command: Command) = Unit

    /**
     * Called when a data message is received.
     *
     * @param data the data message
     */
    suspend fun onData(data: DataAmf) = Unit

    /**
     * Called when a message is received.
     *
     * @param message the message
     */
    suspend fun onMessage(message: Message) = Unit

    /**
     * Factory interface for creating [RtmpConnectionCallback] instances.
     */
    interface Factory {
        fun create(connection: RtmpConnection): RtmpConnectionCallback
    }
}

/**
 * Writes a raw audio, video or script frame from a [ByteArray].
 *
 * The frame must be in the FLV format.
 *
 * Internally, it will parse the frame to extract the header and the body.
 * It is not the most efficient way to write frames but it is convenient.
 *
 * @param array the frame to write
 */
internal suspend fun RtmpConnection.write(array: ByteArray) {
    write(Buffer().apply { write(array) })
}

/**
 * Writes a raw audio, video or script frame from a [Source].
 *
 * The frame must be in the FLV format.
 *
 * @param source the frame to write
 */
internal suspend fun RtmpConnection.write(source: Source) {
    /**
     * Dropping FLV header that is not needed. It starts with 'F', 'L' and 'V'.
     * Just check the first byte to simplify.
     */
    val peek = source.peek()
    val isHeader = try {
        peek.readString(3) == "FLV"
    } catch (_: Throwable) {
        false
    }
    if (isHeader) {
        // Skip header
        FLVHeader.decode(source)
    }

    source.readInt() // skip previous tag size

    val tag = FLVTagRawBody.decode(source)
    when (tag.type) {
        FLVTag.Type.AUDIO -> writeAudio(tag.body, tag.bodySize, tag.timestampMs)
        FLVTag.Type.VIDEO -> writeVideo(tag.body, tag.bodySize, tag.timestampMs)
        FLVTag.Type.SCRIPT_AMF0 -> {
            writeSetDataFrame(tag.body, tag.bodySize, tag.timestampMs, AmfVersion.AMF0)
        }

        FLVTag.Type.SCRIPT_AMF3 -> {
            writeSetDataFrame(tag.body, tag.bodySize, tag.timestampMs, AmfVersion.AMF3)
        }
    }
}

/**
 * Writes a [FLVData].
 *
 * @param data the frame to write
 * @param timestampMs the timestamp of the frame in milliseconds
 */
internal suspend fun RtmpConnection.write(data: FLVData, timestampMs: Int) {
    val rawSource = data.asRawSource(settings.amfVersion, false)
    val rawSourceSize = data.getSize(settings.amfVersion)

    when (data) {
        is AudioData -> writeAudio(
            rawSource, rawSourceSize, timestampMs,
        )

        is VideoData -> writeVideo(
            rawSource, rawSourceSize, timestampMs,
        )

        is ScriptDataObject -> {
            writeSetDataFrame(
                rawSource,
                rawSourceSize,
                timestampMs,
                if (settings.amfVersion == AmfVersion.AMF0) AmfVersion.AMF0 else AmfVersion.AMF3
            )
        }

        else -> throw IllegalArgumentException("Packet type ${data::class.simpleName} not supported")
    }
}

/**
 * Writes a [FLVTag].
 *
 * @param tag the FLV tag to write
 */
internal suspend fun RtmpConnection.write(tag: FLVTag) = write(tag.data, tag.timestampMs)

/**
 * Writes a [FLVTagRawBody].
 *
 * @param tag the FLV tag to write
 */
internal suspend fun RtmpConnection.write(tag: FLVTagRawBody) {
    when (tag.type) {
        FLVTag.Type.AUDIO -> {
            writeAudio(tag.body, tag.bodySize, tag.timestampMs)
        }

        FLVTag.Type.VIDEO -> {
            writeVideo(tag.body, tag.bodySize, tag.timestampMs)
        }

        FLVTag.Type.SCRIPT_AMF0 -> {
            writeSetDataFrame(tag.body, tag.timestampMs, tag.bodySize, AmfVersion.AMF0)
        }

        FLVTag.Type.SCRIPT_AMF3 -> {
            writeSetDataFrame(tag.body, tag.bodySize, tag.timestampMs, AmfVersion.AMF3)
        }
    }
}

