package io.github.thibaultbee.krtmp.rtmpserver.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfContainer
import io.github.thibaultbee.krtmp.common.logger.IKrtmpLogger
import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.flv.tags.script.Metadata
import io.github.thibaultbee.krtmp.rtmp.RtmpConnectionBuilder
import io.github.thibaultbee.krtmp.rtmp.bind
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject
import io.github.thibaultbee.krtmp.rtmp.messages.decode
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.Dispatchers

class RTMPServerCli : SuspendingCliktCommand() {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val builder = RtmpConnectionBuilder(selectorManager)

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

        val server = builder.bind(
            address,
            message = {
                connect {
                    echo("Client connected: $commandObject")

                    // Deserialize the connect object
                    commandObject?.let {
                        val connectObject =
                            Amf.decodeFromAmfElement(ConnectObject.serializer(), it)
                        echo("Connect object: $connectObject")
                    }
                }
                createStream {
                    echo("Stream created: $this")
                }
                releaseStream {
                    echo("Stream released: $this")
                }
                deleteStream {
                    echo("Stream deleted: $this")
                }
                publish {
                    echo("Stream published: $this")
                }
                play {
                    echo("Stream played: $this")
                }
                fcPublish {
                    echo("Stream FCPublished: $this")
                }
                fcUnpublish {
                    echo("Stream FCUnpublished: $this")
                }
                closeStream {
                    echo("Stream close: $this")
                }
                setDataFrame {
                    echo("Set data frame: $this")

                    // Deserialize the onMetadata object
                    val parameters = parameters
                    if ((parameters is AmfContainer) && (parameters.size >= 2)) {
                        val onMetadata = Metadata(parameters[1])
                        echo("onMetadata: $onMetadata")
                    }
                }
                audio {
                    echo("Audio data received: ${decode()}")
                }
                video {
                    echo("Video data received: ${decode()}")
                }
                unknownMessage {
                    echo("Unknown message received: $this")
                }
                unknownCommandMessage {
                    echo("Unknown command received: $this")
                }
                unknownDataMessage {
                    echo("Unknown data message received: $this")
                }
            })

        // Start the RTMP server
        server.listen()
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