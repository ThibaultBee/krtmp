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

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.flv.sources.ByteArrayRawSource
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.RawFLVTag
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.flv.tags.readBuffer
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.flv.util.FLVHeader
import io.github.thibaultbee.krtmp.rtmp.chunk.Chunk
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
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmfMessage
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.NetConnectionResultObject
import io.github.thibaultbee.krtmp.rtmp.messages.ObjectEncoding
import io.github.thibaultbee.krtmp.rtmp.messages.SetChunkSize
import io.github.thibaultbee.krtmp.rtmp.messages.SetDataFrame
import io.github.thibaultbee.krtmp.rtmp.messages.SetPeerBandwidth
import io.github.thibaultbee.krtmp.rtmp.messages.StreamPublishType
import io.github.thibaultbee.krtmp.rtmp.messages.UserControl
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.messages.WindowAcknowledgementSize
import io.github.thibaultbee.krtmp.rtmp.util.MessagesManager
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusCodePublish
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelError
import io.github.thibaultbee.krtmp.rtmp.util.TransactionCommandCompletion
import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
) : CoroutineScope by connection {
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
    var writeChunkSize: Int = settings.writeChunkSize
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
        connection.launch {
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
     * The default value is [RtmpConfiguration.DEFAULT_CHUNK_SIZE].
     *
     * @param chunkSize the write chunk size
     */
    suspend fun setWriteChunkSize(chunkSize: Int) {
        if (writeChunkSize != chunkSize) {
            val setChunkSize = SetChunkSize(settings.clock.nowInMs, chunkSize)
            writeMessage(setChunkSize)
        }
    }

    /**
     * Writes a window acknowledgement size message with the given size.
     *
     * @param size the size of the window acknowledgement
     */
    suspend fun writeWindowAcknowledgementSize(size: Int) {
        val setWindowAcknowledgementSize = WindowAcknowledgementSize(
            settings.clock.nowInMs, size
        )
        writeMessage(setWindowAcknowledgementSize)
    }

    /**
     * Writes a SetPeerBandwidth message with the given size and type.
     *
     * @param size the size of the peer bandwidth
     * @param type the type of the peer bandwidth limit
     */
    suspend fun writeSetPeerBandwidth(size: Int, type: SetPeerBandwidth.LimitType) {
        val setPeerBandwidth = SetPeerBandwidth(
            settings.clock.nowInMs, size, type
        )
        writeMessage(setPeerBandwidth)
    }

    /**
     * Writes a user control message with the given event type.
     *
     * @param eventType the type of the user control event
     */
    suspend fun writeUserControl(
        eventType: UserControl.EventType
    ) {
        val userControl = UserControl(
            settings.clock.nowInMs,
            eventType
        )
        writeMessage(userControl)
    }


    /**
     * Writes a user control message with the given event type and data.
     *
     * @param eventType the type of the user control event
     * @param data the data to send with the user control event
     */
    suspend fun writeUserControl(
        eventType: UserControl.EventType,
        data: Buffer
    ) {
        val userControl = UserControl(
            settings.clock.nowInMs,
            eventType,
            data
        )
        writeMessage(userControl)
    }

    /**
     * Replies to the connect request with the necessary parameters.
     *
     * @param windowAcknowledgementSize the size of the window acknowledgement
     * @param peerBandwidth the peer bandwidth to set
     * @param peerBandwidthType the type of the peer bandwidth limit
     */
    suspend fun replyConnect(
        windowAcknowledgementSize: Int,
        peerBandwidth: Int,
        peerBandwidthType: SetPeerBandwidth.LimitType
    ) {
        val setWindowAcknowledgementSize = WindowAcknowledgementSize(
            settings.clock.nowInMs, windowAcknowledgementSize
        )

        val setPeerBandwidth = SetPeerBandwidth(
            settings.clock.nowInMs, peerBandwidth, peerBandwidthType
        )

        val userControlStreamBegin = UserControl(
            settings.clock.nowInMs,
            UserControl.EventType.STREAM_BEGIN
        )

        val result = CommandNetConnectionResult(
            settings.clock.nowInMs,
            NetConnectionResultObject.default,
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
     * @param connectInformation the information to send in the connect command
     * @return the [Command.Result] send by the server
     */
    suspend fun connect(connectInformation: ConnectInformation = ConnectInformation): Command.Result {
        // Prepare connect object
        val objectEncoding = if (settings.amfVersion == AmfVersion.AMF0) {
            ObjectEncoding.AMF0
        } else {
            ObjectEncoding.AMF3
        }
        val connectObject = ConnectObject(
            app = connection.urlBuilder.rtmpAppOrNull ?: "",
            flashVer = connectInformation.flashVer,
            swfUrl = null,
            tcUrl = connection.urlBuilder.rtmpTcUrl,
            audioCodecs = connectInformation.audioCodecs,
            videoCodecs = connectInformation.videoCodecs,
            pageUrl = null,
            objectEncoding = objectEncoding
        )

        settings.clock.reset()

        val connectTransactionId = transactionId
        val connectCommand = CommandConnect(
            connectTransactionId, settings.clock.nowInMs, connectObject
        )

        try {
            return writeAmfMessageWithResponse(
                connectCommand,
                connectTransactionId
            ) as Command.Result
        } catch (e: RemoteCommandException) {
            throw RemoteCommandException("Connect command failed: ${e.message}", e.command)
        } catch (t: Throwable) {
            throw IOException("Connect command failed", t)
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
        val releaseStreamCommand = CommandReleaseStream(
            transactionId, settings.clock.nowInMs, connection.urlBuilder.rtmpStreamKey
        )

        val fcPublishCommand = CommandFCPublish(
            transactionId, settings.clock.nowInMs, connection.urlBuilder.rtmpStreamKey
        )

        val createStreamTransactionId = transactionId
        val createStreamCommand =
            CommandCreateStream(createStreamTransactionId, settings.clock.nowInMs)

        val result = try {
            writeAmfMessagesWithResponse(
                listOf(
                    releaseStreamCommand,
                    fcPublishCommand,
                    createStreamCommand
                ), createStreamTransactionId
            )
        } catch (e: RemoteCommandException) {
            throw RemoteCommandException("Create stream command failed: ${e.message}", e.command)
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
     * @return the [Command.OnStatus] send by the server
     */
    suspend fun publish(type: StreamPublishType = StreamPublishType.LIVE): Command.OnStatus {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val publishTransactionId = transactionId
        val publishCommand = CommandPublish(
            messageStreamId,
            publishTransactionId,
            settings.clock.nowInMs,
            connection.urlBuilder.rtmpStreamKey,
            type
        )

        return try {
            writeAmfMessageWithResponse(
                publishCommand,
                NetStreamOnStatusCodePublish
            ) as Command.OnStatus
        } catch (e: RemoteCommandException) {
            throw RemoteCommandException("Publish command failed: ${e.message}", e.command)
        } catch (t: Throwable) {
            throw IOException("Publish command failed", t)
        }
    }

    /**
     * Plays the stream.
     *
     * @param streamName the name of the stream to play
     */
    suspend fun play(streamName: String) {
        val playCommand = CommandPlay(
            settings.clock.nowInMs,
            streamName
        )

        return try {
            writeAmfMessage(playCommand)
        } catch (e: RemoteCommandException) {
            throw RemoteCommandException("Play command failed: ${e.message}", e.command)
        } catch (t: Throwable) {
            throw IOException("Play command failed", t)
        }
    }

    /**
     * Deletes the stream.
     *
     * @see [createStream]
     */
    suspend fun deleteStream() {
        val messages = mutableListOf(
            CommandFCUnpublish(
                transactionId, settings.clock.nowInMs, connection.urlBuilder.rtmpStreamKey
            )
        )
        messageStreamId?.let {
            messages += CommandDeleteStream(
                transactionId, settings.clock.nowInMs, it
            )
        }

        writeAmfMessages(messages)
    }

    /**
     * Closes the connection and cleans up resources.
     */
    suspend fun close() {
        try {
            val closeCommand = CommandCloseStream(transactionId, settings.clock.nowInMs)
            writeAmfMessage(closeCommand)
        } catch (t: Throwable) {
            KrtmpLogger.i(TAG, "Error sending close command: ${t.message}")
        }

        connection.close()
        messagesManager.clear()
    }

    /**
     * Writes the SetDataFrame from [OnMetadata.Metadata].
     * It must be called after [publish] and before sending audio or video frames.
     *
     * Expected AMF format is the one set in [RtmpSettings.amfVersion].
     *
     * @param metadata the on metadata to send
     */
    suspend fun writeSetDataFrame(metadata: OnMetadata.Metadata) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            settings.clock.nowInMs,
            messageStreamId,
            metadata
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
     */
    suspend fun writeSetDataFrame(onMetadata: ByteArray) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            settings.amfVersion,
            messageStreamId,
            settings.clock.nowInMs,
            ByteArrayRawSource(onMetadata),
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
     */
    suspend fun writeSetDataFrame(onMetadata: RawSource, size: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val dataFrameDataAmf = SetDataFrame(
            settings.amfVersion,
            messageStreamId,
            settings.clock.nowInMs,
            onMetadata,
            size
        )
        return writeMessage(dataFrameDataAmf)
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
        writeAudio(timestamp, ByteArrayRawSource(array), array.size)

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param array the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, array: ByteArray) =
        writeVideo(timestamp, ByteArrayRawSource(array), array.size)

    /**
     * Writes an audio frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the audio frame to write
     */
    suspend fun writeAudio(timestamp: Int, source: RawSource, sourceSize: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val audio = Audio(timestamp, messageStreamId, source, sourceSize)
        return withTimeoutWriteIfNeeded(audio)
    }

    /**
     * Writes a video frame.
     *
     * The frame must be wrapped in a FLV body.
     *
     * @param timestamp the timestamp of the frame
     * @param source the video frame to write
     */
    suspend fun writeVideo(timestamp: Int, source: RawSource, sourceSize: Int) {
        val messageStreamId = requireNotNull(messageStreamId) {
            "You must call createStream() before publish()"
        }

        val video = Video(timestamp, messageStreamId, source, sourceSize)
        return withTimeoutWriteIfNeeded(video)
    }

    private suspend fun writeAmfMessagesWithResponse(
        amfMessages: List<AmfMessage>,
        id: Any = transactionId
    ): Command {
        writeAmfMessages(amfMessages)
        return commandChannels.waitForResponse(id)
    }

    private suspend fun writeAmfMessages(amfMessages: List<AmfMessage>) {
        val messages = amfMessages.map { it.createMessage(settings.amfVersion) }
        writeMessages(messages)
    }

    private suspend fun writeAmfMessageWithResponse(
        amfMessage: AmfMessage,
        id: Any = transactionId
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
        val length = chunks.sumOf { it.size }
        connection.write(length) { writeChannel ->
            chunks.write(writeChannel)
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
            val length = chunks.sumOf { it.size }
            connection.write(length) { writeChannel ->
                chunks.write(writeChannel)
            }
        }
    }

    private suspend fun handleRtmpMessages() {
        try {
            while (true) {
                handleMessages()
            }
        } catch (t: Throwable) {
            commandChannels.completeAllExceptionally(t)
            KrtmpLogger.i(TAG, "Connection cancelled: ${t.message}")
        } finally {
            close()
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

        when (message) {
            is Acknowledgement -> {
                /**
                 * The server sends Acknowledgement messages to the client every
                 * `writeWindowAcknowledgementSize` bytes send.
                 * We don't do anything with this message.
                 */
            }

            is CommandMessage -> {
                handleCommandMessage(message)
            }

            is SetChunkSize -> {
                readChunkSize = message.chunkSize
            }

            is SetPeerBandwidth -> {
                val windowAcknowledgementSize = WindowAcknowledgementSize(
                    settings.clock.nowInMs, settings.writeWindowAcknowledgementSize
                )
                writeMessage(windowAcknowledgementSize)
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
                withContext(callbackDispatcher) {
                    callback.onMessage(message)
                }
            }
        }
    }

    private suspend fun handleCommandMessage(message: CommandMessage) {
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

            else ->
                withContext(callbackDispatcher) { callback.onCommand(command) }
        }
    }

    private suspend fun handleDataMessage(message: DataAmfMessage) {
        withContext(callbackDispatcher) {
            callback.onData(DataAmf.read(message))
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

    companion object {
        internal const val TAG = "MessageStreamer"
    }
}


/**
 * Callback interface for the [RtmpConnection].
 */
internal interface RtmpConnectionCallback {
    suspend fun onCommand(command: Command) = Unit
    suspend fun onData(data: DataAmf) = Unit
    suspend fun onMessage(message: Message) = Unit

    interface Factory {
        fun create(streamer: RtmpConnection): RtmpConnectionCallback
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
    } catch (t: Throwable) {
        false
    }
    if (isHeader) {
        // Skip header
        FLVHeader.decode(source)
    }

    source.readInt() // skip previous tag size

    val tag = RawFLVTag.decode(source)
    when (tag.type) {
        FLVTag.Type.AUDIO -> writeAudio(tag.timestampMs, tag.body, tag.bodySize)
        FLVTag.Type.VIDEO -> writeVideo(tag.timestampMs, tag.body, tag.bodySize)
        FLVTag.Type.SCRIPT -> writeSetDataFrame(tag.body, tag.bodySize)
    }
}

/**
 * Writes a [FLVData].
 *
 * @param timestampMs the timestamp of the frame in milliseconds
 * @param data the frame to write
 */
internal suspend fun RtmpConnection.write(timestampMs: Int, data: FLVData) {
    when (data) {
        is AudioData -> writeAudio(
            timestampMs,
            data.readBuffer(),
            data.getSize(settings.amfVersion)
        )

        is VideoData -> writeVideo(
            timestampMs,
            data.readBuffer(),
            data.getSize(settings.amfVersion)
        )

        is OnMetadata -> {
            writeSetDataFrame(data.readBuffer(), data.getSize(settings.amfVersion))
        }

        else -> throw IllegalArgumentException("Packet type ${data::class.simpleName} not supported")
    }
}

/**
 * Writes a [FLVTag].
 *
 * @param tag the FLV tag to write
 */
internal suspend fun RtmpConnection.write(tag: FLVTag) {
    write(tag.timestampMs, tag.data)
}

/**
 * Writes a [RawFLVTag].
 *
 * @param tag the FLV tag to write
 */
internal suspend fun RtmpConnection.write(tag: RawFLVTag) {
    when (tag.type) {
        FLVTag.Type.AUDIO -> {
            writeAudio(tag.timestampMs, tag.body, tag.bodySize)
        }

        FLVTag.Type.VIDEO -> {
            writeVideo(tag.timestampMs, tag.body, tag.bodySize)
        }

        FLVTag.Type.SCRIPT -> {
            writeSetDataFrame(tag.body, tag.bodySize)
        }
    }
}

