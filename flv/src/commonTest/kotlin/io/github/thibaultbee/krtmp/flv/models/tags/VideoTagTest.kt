package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.models.av.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class VideoTagTest {
    @Test
    fun `test write avc key`() {
        val expected = Resource("tags/video/avc/key/muxed").toByteArray()

        val raw = Resource("tags/video/avc/key/key").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val videoTag =
            LegacyVideoTag(
                36321,
                rawBuffer,
                rawBuffer.size.toInt(),
                MimeType.VIDEO_AVC,
                FrameType.KEY,
                AVCPacketType.NALU
            )

        assertContentEquals(expected, FlvTagPacket(videoTag).readByteArray())
    }

    @Test
    fun `test write avc body`() {
        val expected = Resource("tags/video/avc/key/muxedbody").toByteArray()

        val raw = Resource("tags/video/avc/key/key").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val videoTag =
            LegacyVideoTag(
                36321,
                rawBuffer,
                rawBuffer.size.toInt(),
                MimeType.VIDEO_AVC,
                FrameType.KEY,
                AVCPacketType.NALU
            )

        assertContentEquals(expected, FlvTagPacket(videoTag).bodyOutputPacket.readByteArray())
    }

    @Test
    fun `test read avc key`() {
        val expected = Resource("tags/video/avc/key/key").toByteArray()

        val muxed = Resource("tags/video/avc/key/muxed").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val videoTag = FlvTagPacket.read(muxBuffer) as LegacyVideoTag

        assertEquals(36321, videoTag.timestampMs)
        assertEquals(CodecID.AVC, videoTag.tagHeader.codecID)
        assertEquals(FrameType.KEY, videoTag.tagHeader.frameType)
        assertEquals(AVCPacketType.NALU, videoTag.tagHeader.packetType)

        val actualBuffer = Buffer().apply {
            write(videoTag.source, videoTag.sourceSize.toLong())
        }
        assertContentEquals(expected, actualBuffer.readByteArray())
    }
}