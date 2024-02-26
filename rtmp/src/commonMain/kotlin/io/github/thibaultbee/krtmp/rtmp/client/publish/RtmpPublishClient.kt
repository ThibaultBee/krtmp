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

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.common.logger.Logger
import io.github.thibaultbee.krtmp.flv.FlvMuxer
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import io.github.thibaultbee.krtmp.flv.models.packets.Packet
import io.github.thibaultbee.krtmp.flv.models.sources.ByteArrayRawSource
import io.github.thibaultbee.krtmp.flv.models.tags.FlvTag
import io.github.thibaultbee.krtmp.flv.models.tags.OnMetadata
import io.github.thibaultbee.krtmp.rtmp.RtmpConfiguration
import io.github.thibaultbee.krtmp.rtmp.client.RemoteServerException
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClientConnectInformation
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClientSettings
import io.github.thibaultbee.krtmp.rtmp.extensions.streamKey
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
import io.github.thibaultbee.krtmp.rtmp.utils.MessagesManager
import io.github.thibaultbee.krtmp.rtmp.utils.NetStreamCommand
import io.github.thibaultbee.krtmp.rtmp.utils.RtmpClock
import io.github.thibaultbee.krtmp.rtmp.utils.TransactionCommandCompletion
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.Connection
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.isClosed
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource

/**
 * A RTMP client to publish stream.
 *
 * To create a client, use [RtmpPublishClientConnectionFactory.connect].
 *
 * The usage is:
 *  - Connect to the server with [RtmpPublishClientConnectionFactory.connect]
 *  - Send RTMP create stream command with [createStream]
 *  - Send RTMP publish command with [publish]
 *
 * - Send metadata and audio and video frames with [writeSetDataFrame] and [writeAudio] or [writeVideo].
 *
 * - Close the connection with [close]
 */
class RtmpPublishClient internal constructor(
    private val urlBuilder: URLBuilder,
    private val connection: Connection,
    private val settings: Settings
) : CoroutineScope {
    private val messagesManager = MessagesManager()

    private var _transactionId = 1L
    private val transactionId: Long
        get() = _transactionId++

    private val input = connection.input
    private val output = connection.output
    private val socket = connection.socket

    private var writeChunkSize: Int = settings.writeChunkSize
    private var readChunkSize = RtmpConfiguration.DEFAULT_CHUNK_SIZE
    private var readWindowAcknowledgementSize = Int.MAX_VALUE
    private var lastReadWindowAcknowledgementSize = input.totalBytesRead

    private val commandChannels = TransactionCommandCompletion()

    private var messageStreamId = 0

    private var _flvMuxer: FlvMuxer? = null

    override val coroutineContext: CompletableJob = Job()

    init {
        // A hook to intercept connection closure and clean up resources
        socket.socketContext.invokeOnCompletion {
            messagesManager.clear()
            if (it != null) {
                coroutineContext.completeExceptionally(it)
            } else {
                coroutineContext.complete()
            }
        }
    }

    /**
     * Gets the cause of the connection closure.
     */
    @OptIn(InternalCoroutinesApi::class)
    val closedCause: Throwable?
        get() {
            return try {
                socket.socketContext.getCancellationException().cause
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Returns the FLV muxer that can be used to write FLV tags directly.
     *
     * The muxer is created on the first call and reused for subsequent calls.
     *
     * Before using the muxer for writing frames, you must call [FlvMuxer.addStream] and
     * [FlvMuxer.startStream].
     *
     * When using this muxer, you don't have to call [writeAudio] neither [writeVideo] (and
     * assimilated).
     *
     * @return the FLV muxer
     */
    val flvMuxer: FlvMuxer
        get() {
            _flvMuxer?.let { return it }

            val listener = object : FlvMuxer.Listener {
                override fun onOutputPacket(outputPacket: Packet) {
                    // Throw exception if connection is closed so the user can handle it
                    if (socket.isClosed) {
                        throw IOException("Connection closed", closedCause)
                    }
                    try {
                        socket.launch {
                            try {
                                writePacket(outputPacket)
                            } catch (e: TimeoutCancellationException) {
                                Logger.w(TAG, "Timeout while writing packet: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        throw IOException("Failed to write packet", e)
                    }
                }
            }
            val muxer = FlvMuxer().apply {
                addListener(listener)
            }
            _flvMuxer = muxer
            return muxer
        }

    /**
     * Returns true if the connection is closed.
     */
    val isClosed: Boolean
        get() = connection.socket.isClosed

    /**
     * Returns the write chunk size.
     *
     * The default value is [RtmpConfiguration.DEFAULT_CHUNK_SIZE].
     *
     * @return the write chunk size
     * @see [setWriteChunkSize]
     */
    fun getWriteChunkSize() = writeChunkSize

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

        // Launch coroutine to handle RTMP messages
        socket.launch {
            handleRtmpMessages()
        }

        settings.clock.reset()

        val connectTransactionId = transactionId
        val connectCommand = Command.Connect(
            connectTransactionId, settings.clock.nowInMs, connectObject
        )
        connectCommand.write()

        try {
            return commandChannels.waitForResponse(connectTransactionId) as Command.Result
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
     */
    suspend fun createStream(): Command.Result {
        val releaseStreamCommand = Command.ReleaseStream(
            transactionId, settings.clock.nowInMs, urlBuilder.streamKey
        )
        releaseStreamCommand.write()

        val fcPublishCommand = Command.FCPublish(
            transactionId, settings.clock.nowInMs, urlBuilder.streamKey
        )
        fcPublishCommand.write()

        val createStreamTransactionId = transactionId
        val createStreamCommand =
            Command.CreateStream(createStreamTransactionId, settings.clock.nowInMs)
        createStreamCommand.write()

        val result = try {
            commandChannels.waitForResponse(createStreamTransactionId)
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
            commandChannels.waitForResponse(NetStreamCommand.PUBLISH) as Command.OnStatus
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
     * Writes a raw frame.
     *
     * The frame must be in the FLV format.
     *
     * Internally, it will parse the frame to extract the header and the body.
     * It is not the most efficient way to write frames but it is convenient.
     * If you have a frame in the FLV format, prefer using [writeAudio] or [writeVideo].
     *
     * @param buffer the frame to write
     */
    suspend fun writeFrame(buffer: Buffer) {
        try {
            val header = FlvTagPacket.Header.read(buffer)
            val frame = Buffer().apply { write(buffer, header.bodySize.toLong()) }
            when (header.type) {
                FlvTag.Type.AUDIO -> writeAudio(header.timestampMs, frame)
                FlvTag.Type.VIDEO -> writeVideo(header.timestampMs, frame)
                FlvTag.Type.SCRIPT -> writeSetDataFrame(buffer)
                else -> throw IllegalArgumentException("Frame type ${header.type} not supported")
            }
        } catch (e: Exception) {
            throw IOException("Failed to write frame", e)
        }
    }

    /**
     * Writes an [Packet].
     *
     * @param packet the FLV packet to write
     */
    suspend fun writePacket(packet: Packet) {
        // Only FlvTagOutputPacket matters here
        if (packet is FlvTagPacket) {
            writeFlvTagOutputPacket(packet)
        }
    }

    /**
     * Writes a FLV tag wrapped in a [FlvTagPacket].
     *
     * @param tagPacket the FLV tag output packet to write
     */
    private suspend fun writeFlvTagOutputPacket(tagPacket: FlvTagPacket) {
        return when {
            tagPacket.tag.type == FlvTag.Type.AUDIO -> writeAudio(
                tagPacket.timestampMs,
                tagPacket.bodyOutputPacket.readRawSource()
            )

            tagPacket.tag.type == FlvTag.Type.VIDEO -> writeVideo(
                tagPacket.timestampMs,
                tagPacket.bodyOutputPacket.readRawSource()
            )

            tagPacket.tag is OnMetadata -> {
                val flvTag = tagPacket.tag as OnMetadata
                flvTag.amfVersion = settings.amfVersion
                writeSetDataFrame(flvTag.metadata)
            }

            else -> throw IllegalArgumentException("Packet type ${tagPacket.tag::class.simpleName} not supported")
        }
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
     * Closes the connection.
     */
    suspend fun close() {
        val closeCommand = Command.CloseStream(transactionId, settings.clock.nowInMs)
        closeCommand.write()

        connection.socket.close()
    }

    private suspend fun handleRtmpMessages() {
        try {
            while (true) {
                handleRtmpMessage()
            }
        } catch (e: CancellationException) {
            commandChannels.completeAllExceptionally(e)
            Logger.i(TAG, "Connection cancelled")
        } catch (e: ClosedReceiveChannelException) {
            commandChannels.completeAllExceptionally(e)
            Logger.i(TAG, "Received channel closed")
        } catch (e: ClosedSendChannelException) {
            commandChannels.completeAllExceptionally(e)
            Logger.i(TAG, "Send channel closed")
        } catch (e: Exception) {
            commandChannels.completeAllExceptionally(e)
            Logger.e(TAG, "Error while handling RTMP messages", e)
        } finally {
            connection.socket.close()
        }
    }

    private suspend fun handleRtmpMessage() {
        val message = messagesManager.read(input, readChunkSize).apply {
            // Send Acknowledgement message if needed
            val totalBytesRead = input.totalBytesRead
            val readBytes = totalBytesRead - lastReadWindowAcknowledgementSize
            if (readBytes >= readWindowAcknowledgementSize) {
                Acknowledgement(settings.clock.nowInMs, totalBytesRead.toInt()).write()
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

            is Audio -> {
                throw NotImplementedError("Audio not supported")
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

            is Video -> {
                throw NotImplementedError("Video not supported")
            }

            is WindowAcknowledgementSize -> {
                readWindowAcknowledgementSize = message.windowSize
            }

            else -> {
                throw IllegalArgumentException("Message $message not supported (type: ${message.messageType})")
            }
        }
    }

    suspend fun AmfMessage.write() {
        val message = createMessage(settings.amfVersion)
        message.write()
    }

    suspend fun Message.withTimeoutWriteIfNeeded() {
        if (settings.enableTooLateFrameDrop) {
            val timeoutInMs: Long =
                settings.tooLateFrameDropTimeoutInMs - (settings.clock.nowInMs - timestamp)
            withTimeout(timeoutInMs) {
                messagesManager.write(this@withTimeoutWriteIfNeeded, output, writeChunkSize)
            }
        } else {
            messagesManager.write(this, output, writeChunkSize)
        }
    }

    suspend fun Message.write() {
        messagesManager.write(this, output, writeChunkSize)
    }

    companion object {
        private const val TAG = "RtmpPublishClient"
    }

    open class ConnectInformation(
        flashVer: String = DEFAULT_FLASH_VER,
        audioCodecs: List<MimeType>? = DEFAULT_AUDIO_CODECS,
        videoCodecs: List<MimeType>? = DEFAULT_VIDEO_CODECS,
    ) : RtmpClientConnectInformation(flashVer, audioCodecs, videoCodecs) {
        /**
         * The default instance of [ConnectInformation]
         */
        companion object Default : ConnectInformation()
    }

    /**
     * RTMP settings for [RtmpPublishClient].
     *
     * @param enableTooLateFrameDrop enable dropping too late frames. Default is false. It will drop frames if they are are too late if set to true. If enable, make sure frame timestamps are on on the same clock as [clock].
     * @param tooLateFrameDropTimeoutInMs the timeout after which a frame will be dropped (from frame timestamps). Default is 3000ms.
     */
    open class Settings(
        socketOptions: SocketOptions.PeerSocketOptions.() -> Unit = {},
        writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
        writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
        amfVersion: AmfVersion = AmfVersion.AMF0,
        clock: RtmpClock = RtmpClock.Default(),
        val enableTooLateFrameDrop: Boolean = false,
        val tooLateFrameDropTimeoutInMs: Long = DEFAULT_TOO_LATE_FRAME_DROP_TIMEOUT_IN_MS
    ) : RtmpClientSettings(
        socketOptions,
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
