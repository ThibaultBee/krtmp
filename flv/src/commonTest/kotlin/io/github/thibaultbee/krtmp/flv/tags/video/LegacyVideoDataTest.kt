/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.readByteArray
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LegacyVideoDataTest {
    @Test
    fun `write avc key tag test`() {
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
    fun `write avc key data test`() {
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
    fun `read avc key tag test`() {
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