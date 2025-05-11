package io.github.thibaultbee.krtmp.rtmpclient.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.thibaultbee.krtmp.common.logger.IKrtmpLogger
import io.github.thibaultbee.krtmp.common.logger.KrtmpLogger
import io.github.thibaultbee.krtmp.flv.FLVDemuxer
import io.github.thibaultbee.krtmp.flv.decodeAll
import io.github.thibaultbee.krtmp.flv.decodeAllRaw
import io.github.thibaultbee.krtmp.rtmp.client.RtmpClient
import kotlinx.io.files.Path

class RTMPClientCli : SuspendingCliktCommand() {
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
        // Create the RTMP client
        val client = RtmpClient(rtmpUrlPath)
        try {
            val result = client.connect {
                // Configure the connect object
                // videoCodecs = listOf(VideoMediaType.AVC)
            }
            echo("Connected to RTMP server: $result")

            client.createStream()
            client.publish()
        } catch (t: Throwable) {
            echo("Error connecting to connect server: ${t.message}")
            client.close()
            throw t
        }

        // Read the FLV file and send it to the RTMP server
        try {
            echo("Sending FLV file: $filePath")

            val header = parser.decodeFlvHeader()
            echo("Parsed FLV header: $header")

            /*
            parser.decodeAllRaw { tag ->
                echo("Sending: $tag")
                client.write(tag)
            }*/
            parser.decodeAll { tag ->
                echo("Sending: $tag")
                client.write(tag)
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
            client.close()
        } catch (t: Throwable) {
            echo("Error connecting to close connection to the server: ${t.message}")
            client.close()
            throw t
        }
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