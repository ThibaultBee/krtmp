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

import io.github.thibaultbee.krtmp.amf.AmfVersion
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyVideoDatasTest {
    @Test
    fun `create avc nalu test`() {
        val factory = AVCVideoDataFactory()
        val codedFrame = factory.codedFrame(
            VideoFrameType.KEY,
            Buffer().apply { writeInt(0x01020304) },
            4,
            1234
        )

        assertEquals(VideoFrameType.KEY, codedFrame.frameType)
        assertEquals(1234, codedFrame.compositionTime)
        assertEquals(AVCPacketType.NALU, codedFrame.packetType)
        assertEquals(4, codedFrame.body.getSize(AmfVersion.AMF0))
        assertEquals(
            0x01020304,
            codedFrame.body.asRawSource(AmfVersion.AMF0).buffered().readInt()
        )
    }

    @Test
    fun `create avc sequence start test`() {
        val factory = AVCVideoDataFactory()
        val codedFrame = factory.sequenceStart(
            Buffer().apply { writeInt(0x01020304) },
            4
        )

        assertEquals(AVCPacketType.SEQUENCE_HEADER, codedFrame.packetType)
        assertEquals(4, codedFrame.body.getSize(AmfVersion.AMF0))
        assertEquals(
            0x01020304,
            codedFrame.body.asRawSource(AmfVersion.AMF0).buffered().readInt()
        )
    }

    @Test
    fun `create avc sequence end test`() {
        val factory = AVCVideoDataFactory()
        val codedFrame = factory.sequenceEnd()

        assertEquals(AVCPacketType.END_OF_SEQUENCE, codedFrame.packetType)
        assertTrue(codedFrame.body is EmptyVideoTagBody)
    }
}