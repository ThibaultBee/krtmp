package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.config.CodecID
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class VideoDataTest {
    @Test
    fun `test write avc key tag`() {
        val expected = Resource("tags/video/avc/key/tag").toByteArray()

        val raw = Resource("tags/video/avc/key/key").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val videoTagBody = RawVideoTagBody(rawBuffer, rawBuffer.size.toInt())
        val videoData =
            LegacyVideoData(
                frameType = VideoFrameType.KEY,
                codecID = CodecID.AVC,
                packetType = AVCPacketType.NALU,
                compositionTime = 0,
                body = videoTagBody
            )

        assertContentEquals(expected, FLVTag(36321, videoData).readByteArray())
    }

    @Test
    fun `test write avc key data`() {
        val expected = Resource("tags/video/avc/key/data").toByteArray()

        val raw = Resource("tags/video/avc/key/key").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val videoTagBody = RawVideoTagBody(rawBuffer, rawBuffer.size.toInt())
        val videoTag =
            LegacyVideoData(
                frameType = VideoFrameType.KEY,
                codecID = CodecID.AVC,
                packetType = AVCPacketType.NALU,
                compositionTime = 0,
                body = videoTagBody
            )

        assertContentEquals(expected, videoTag.readByteArray())
    }

    @Test
    fun `test read avc key tag`() {
        val expected = Resource("tags/video/avc/key/key").toByteArray()

        val muxed = Resource("tags/video/avc/key/tag").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val videoTag = FLVTag.decode(muxBuffer)
        assertEquals(36321, videoTag.timestampMs)

        val videoData = videoTag.data as LegacyVideoData
        assertEquals(CodecID.AVC, videoData.codecID)
        assertEquals(VideoFrameType.KEY, videoData.frameType)
        assertEquals(AVCPacketType.NALU, videoData.packetType)

        val body = videoData.body as RawVideoTagBody
        val actual = Buffer().apply {
            write(body.data, body.dataSize.toLong())
        }

        assertContentEquals(expected, actual.readByteArray())
    }
}