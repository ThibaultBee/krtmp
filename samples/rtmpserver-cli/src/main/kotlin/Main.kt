package io.github.thibaultbee.krtmp.rtmpserver.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfContainer
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.common.logger.IKrtmpLogger
import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.ConnectObject
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.Video
import io.github.thibaultbee.krtmp.rtmp.messages.decode
import io.github.thibaultbee.krtmp.rtmp.server.RtmpServer
import io.github.thibaultbee.krtmp.rtmp.server.RtmpServerCallback
import io.github.thibaultbee.krtmp.rtmp.util.AmfUtil.amf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RTMPServerCli : SuspendingCliktCommand() {
    private val decoderScope = CoroutineScope(Dispatchers.Default)

    init {
        KrtmpLogger.logger = EchoLogger()
    }

    override fun help(context: Context): String {
        return "Launches a RTMP server on the specified address"
    }

    private val address: String by argument(
        "address",
        help = "The address to bind the RTMP server to"
    )

    override suspend fun run() {
        echo("RTMP server listening on $address")

        // Create the RTMP server
        val server = RtmpServer(address, object : RtmpServerCallback {
            override fun onConnect(connect: Command) {
                echo("Client connected: ${connect.commandObject}")

                // Deserialize the connect object
                connect.commandObject?.let {
                    val connectObject =
                        amf.decodeFromAmfElement(ConnectObject.serializer(), it)
                    echo("Connect object: $connectObject")
                }
            }

            override fun onCreateStream(createStream: Command) {
                echo("Stream created: $createStream")
            }

            override fun onReleaseStream(releaseStream: Command) {
                echo("Stream released: $releaseStream")
            }

            override fun onDeleteStream(deleteStream: Command) {
                echo("Stream deleted: $deleteStream")
            }

            override fun onPublish(publish: Command) {
                echo("Stream published: $publish")
            }

            override fun onPlay(play: Command) {
                echo("Stream played: $play")
            }

            override fun onFCPublish(fcPublish: Command) {
                echo("Stream FCPublished: $fcPublish")
            }

            override fun onFCUnpublish(fcUnpublish: Command) {
                echo("Stream FCUnpublished: $fcUnpublish")
            }

            override fun onCloseStream(closeStream: Command) {
                echo("Stream close: $closeStream")
            }

            override fun onSetDataFrame(setDataFrame: DataAmf) {
                echo("Set data frame: $setDataFrame")

                val parameters = setDataFrame.parameters
                // Deserialize the onMetadata object
                if ((parameters is AmfContainer) && (parameters.size >= 2)) {
                    val onMetadata = OnMetadata.Metadata.decode(parameters[1] as AmfEcmaArray)
                    echo("onMetadata: $onMetadata")
                }
            }

            override fun onAudio(audio: Audio) {
                // Dispatch the audio data to a coroutine for decoding to avoid blocking the server thread
                decoderScope.launch {
                    echo("Audio data received: ${audio.decode()}")
                }
            }

            override fun onVideo(video: Video) {
                // Dispatch the video data to a coroutine for decoding to avoid blocking the server thread
                decoderScope.launch {
                    echo("Video data received: ${video.decode()}")
                }
            }

            override fun onUnknownMessage(message: Message) {
                echo("Unknown message received: $message")
            }

            override fun onUnknownCommandMessage(command: Command) {
                echo("Unknown command received: $command")
            }

            override fun onUnknownDataMessage(data: DataAmf) {
                echo("Unknown data message received: $data")
            }
        })

        // Start the RTMP server
        server.start()
    }


    private inner class EchoLogger : IKrtmpLogger {
        override fun e(tag: String, message: String, tr: Throwable?) {
            echo("E[$tag] $message")
        }

        override fun w(tag: String, message: String, tr: Throwable?) {
            echo("W[$tag] $message")
        }

        override fun i(tag: String, message: String, tr: Throwable?) {
            echo("I[$tag] $message")
        }

        override fun v(tag: String, message: String, tr: Throwable?) {
            echo("V[$tag] $message")
        }

        override fun d(tag: String, message: String, tr: Throwable?) {
            echo("D[$tag] $message")
        }
    }
}

suspend fun main(args: Array<String>) = RTMPServerCli().main(args)