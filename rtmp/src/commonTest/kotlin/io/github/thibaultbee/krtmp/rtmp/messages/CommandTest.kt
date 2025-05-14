package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CommandTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `encode amf0 connect object`() = runTest {
        val expected =
            "030003617070020007746573744170700008666c61736856657202000c74657374466c6173685665720005746355726c02000974657374546355726c000673776655726c02000a7465737453776655726c0004667061640100000c6361706162696c697469657300406de00000000000000b617564696f436f64656373004096000000000000000b766964656f436f64656373004060800000000000000a666f757243634c6973740a0000000102000468766331000d766964656f46756e6374696f6e00000000000000000000077061676555726c02000b746573745061676555726c000e6f626a656374456e636f64696e67000000000000000000000009"
        val connectObject = Command.Connect.ConnectObject(
            app = "testApp",
            flashVer = "testFlashVer",
            tcUrl = "testTcUrl",
            swfUrl = "testSwfUrl",
            fpad = false,
            capabilities = 239,
            audioCodecs = listOf(
                AudioMediaType.G711_ALAW,
                AudioMediaType.G711_MLAW,
                AudioMediaType.AAC
            ),
            videoCodecs = listOf(
                VideoMediaType.AVC,
                VideoMediaType.SORENSON_H263,
                VideoMediaType.HEVC
            ),
            videoFunction = emptyList(),
            pageUrl = "testPageUrl",
            objectEncoding = Command.Connect.ObjectEncoding.AMF0
        )
        val actual =
            Amf.encodeToByteArray(Command.Connect.ConnectObject.serializer(), connectObject)
        assertEquals(expected, actual.toHexString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `write amf0 connect command`() = runTest {
        val expected =
            "020000000001121400000000020007636f6e6e656374003ff0000000000000030003617070020007746573744170700008666c61736856657202000c74657374466c6173685665720005746355726c02000974657374546355726c000673776655726c02000a7465737453776655726c0004667061640100000c6361706162696c697469657300406de00000c2000000000b617564696f436f64656373004096000000000000000b766964656f436f64656373004060800000000000000a666f757243634c6973740a0000000102000468766331000d766964656f46756e6374696f6e00000000000000000000077061676555726c02000b746573745061676555726c000e6f626a656374456ec2636f64696e67000000000000000000000009"
        val connectObject = Command.Connect.ConnectObject(
            app = "testApp",
            flashVer = "testFlashVer",
            tcUrl = "testTcUrl",
            swfUrl = "testSwfUrl",
            fpad = false,
            capabilities = 239,
            audioCodecs = listOf(
                AudioMediaType.G711_ALAW,
                AudioMediaType.G711_MLAW,
                AudioMediaType.AAC
            ),
            videoCodecs = listOf(
                VideoMediaType.AVC,
                VideoMediaType.SORENSON_H263,
                VideoMediaType.HEVC
            ),
            videoFunction = emptyList(),
            pageUrl = "testPageUrl",
            objectEncoding = Command.Connect.ObjectEncoding.AMF0
        )

        val connectCommand = Command.Connect(
            transactionId = 1,
            timestamp = 0,
            connectObject = connectObject
        )
        val writeChannel = ByteChannel(false)
        connectCommand.write(writeChannel, AmfVersion.AMF0)

        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertEquals(expected, actual.toHexString())
    }

    @Test
    fun `encode amf0 create stream with a previous message`() = runTest {
        val expected = byteArrayOf(
            0x42,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x19,
            0x14,
            0x02,
            0x00,
            0x0c,
            0x63,
            0x72,
            0x65,
            0x61,
            0x74,
            0x65,
            0x53,
            0x74,
            0x72,
            0x65,
            0x61,
            0x6d,
            0x00,
            0x40,
            0x10,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x05
        )

        val previousMessage = Command.FCPublish(
            transactionId = 3,
            timestamp = 0,
            streamKey = "streamKey"
        ).createMessage(AmfVersion.AMF0)
        val createStreamCommand = Command.CreateStream(
            timestamp = 0,
            transactionId = 4,
        )
        val writeChannel = ByteChannel(false)
        createStreamCommand.write(
            writeChannel,
            AmfVersion.AMF0,
            previousMessage = previousMessage
        )

        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}