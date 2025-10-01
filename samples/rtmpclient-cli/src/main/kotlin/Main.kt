package io.github.thibaultbee.krtmp.rtmpclient.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.thibaultbee.krtmp.flv.FLVDemuxer
import io.github.thibaultbee.krtmp.flv.decodeAllTagOnly
import io.github.thibaultbee.krtmp.logger.IKrtmpLogger
import io.github.thibaultbee.krtmp.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.rtmp.RtmpConnectionBuilder
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClientSettings
import io.github.thibaultbee.krtmp.rtmp.connect
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.awaitClosed
import kotlinx.coroutines.Dispatchers
import kotlinx.io.files.Path

class RTMPClientCli : SuspendingCliktCommand() {
    private val selectorManager = SelectorManager(Dispatchers.IO)
    private val builder = RtmpConnectionBuilder(selectorManager)

    init {
        KrtmpLogger.logger = EchoLogger()
    }

    override fun help(context: Context): String {
        return "Send a FLV file to a RTMP server on the specified address"
    }

    private val filePath: String by option("-i", "--input", help = "The FLV file to send")
        .required()
    private val rtmpUrlPath: String by argument(
        "RTMP URL",
        help = "The RTMP URL to send the FLV file to (expected format: rtmp://<host>:<port>/<app>/<stream>)"
    )

    override suspend fun run() {
        echo("Trying to open file $filePath")
        val path = Path(filePath)
        val parser = FLVDemuxer(path = path)

        echo("Trying to connect to $rtmpUrlPath")
        val client =
            try {
                // Connect the RTMP client
                builder.connect(
                    rtmpUrlPath, RtmpClientSettings(
                        connectInfo = {
                            // Configure the connect object here if needed
                            // videoCodecs = listOf(VideoMediaType.AVC)
                        }
                    ))
            } catch (t: Throwable) {
                echo("Error connecting to the server: ${t.message}")
                throw t
            }

        echo("Connected to RTMP server")

        try {
            client.createStream()
            client.publish()
        } catch (t: Throwable) {
            echo("Error sending publish to the server: ${t.message}")
            client.close()
            throw t
        }

        // Read the FLV file and send it to the RTMP server
        try {
            echo("Sending FLV file: $filePath")

            val header = parser.decodeFlvHeader()
            echo("Parsed FLV header: $header")

            parser.decodeAllTagOnly {
                echo("Sending: $this")
                client.write(this)
            }
        } catch (t: Throwable) {
            echo("Error reading FLV file: ${t.message}")
            client.close()
            throw t
        }

        // Close the connection gracefully
        try {
            echo("Closing connection to the server")
            client.deleteStream()
        } catch (t: Throwable) {
            echo("Error sending delete stream to the server: ${t.message}")
            throw t
        } finally {
            client.close()
        }
        client.awaitClosed()
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

suspend fun main(args: Array<String>) = RTMPClientCli().main(args)