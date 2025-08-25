package io.github.thibaultbee.krtmp.flvparser.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.thibaultbee.krtmp.flv.FLVDemuxer
import io.github.thibaultbee.krtmp.flv.decodeAllTagOnly
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.audio.ExtendedAudioData
import io.github.thibaultbee.krtmp.flv.tags.audio.LegacyAudioData
import io.github.thibaultbee.krtmp.flv.tags.script.ScriptDataObject
import io.github.thibaultbee.krtmp.flv.tags.video.ExtendedVideoData
import io.github.thibaultbee.krtmp.flv.tags.video.LegacyVideoData
import kotlinx.io.files.Path

class FLVParserCli : SuspendingCliktCommand() {
    override fun help(context: Context): String {
        return "Parse a FLV file"
    }

    private val filePath: String by option("-i", "--input", help = "The FLV file to parse")
        .required()

    private fun prettyTag(index: Int, tag: FLVTag): String {
        return """
        |FLV Tag: [$index]
        |  Type: ${tag::class.java.simpleName}
        |  Timestamp: ${tag.timestampMs} ms
        |  Data: [${prettyData(tag.data)}]
    """.trimMargin()
    }

    private fun prettyData(data: FLVData): String {
        return when (data) {
            is LegacyAudioData -> {
                """
            |Legacy Audio Data: Sound Format: ${data.soundFormat} Sound Rate: ${data.soundRate} Sound Size: ${data.soundSize} Sound Type: ${data.soundType} Body: ${data.body}
            """.trimMargin()
            }

            is ExtendedAudioData -> {
                """
            |Extended Audio Data: Packet Type: ${data.packetType} Packet Descriptor: ${data.packetDescriptor} ModExs: ${data.modExs} Body: ${data.body}
            """.trimMargin()
            }

            is LegacyVideoData -> {
                """
            |Legacy Video Data: Codec ID: ${data.codecID} Frame Type: ${data.frameType} AVCPacketType: ${data.packetType} Composition Time: ${data.compositionTime} Body: ${data.body}
            """.trimMargin()
            }

            is ExtendedVideoData -> {
                """
            |Extended Video Data: Packet Type: ${data.packetType} Frame Type: ${data.frameType} Packet Descriptor: ${data.packetDescriptor} ModExs: ${data.modExs} Body: ${data.body}
            """.trimMargin()
            }

            is ScriptDataObject -> {
                """
            |Script Data Object: Name: ${data.name} Value: ${data.value}
            """.trimMargin()
            }

            else -> data.toString()
        }
    }

    override suspend fun run() {
        require(filePath.endsWith(".flv")) { "The file must be a .flv file" }

        echo("Parsing FLV file: $filePath")
        val path = Path(filePath)
        val parser = FLVDemuxer(path = path)
        val header = parser.decodeFlvHeader()
        echo("Parsed FLV header: $header")
        var i = 0

        parser.decodeAllTagOnly { tag ->
            try {
                val decodedTag = tag.decodeTag()
                echo(prettyTag(i++, decodedTag))
            } catch (t: Throwable) {
                echo("${i++}: failed to decode: ${t.message}")
            }
        }
    }
}

suspend fun main(args: Array<String>) = FLVParserCli().main(args)