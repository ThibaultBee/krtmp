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
package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlin.test.Test
import kotlin.test.assertEquals

class LegacyAudioDatasTest {
    @Test
    fun `create aac raw test`() {
        val factory = AACAudioDataFactory(
            soundRate = SoundRate.F_44100HZ,
            soundSize = SoundSize.S_16BITS,
            soundType = SoundType.STEREO
        )
        val codedFrame = factory.codedFrame(Buffer().apply {
            writeInt(0x01020304)
        }, 4)

        assertEquals(SoundRate.F_44100HZ, codedFrame.soundRate)
        assertEquals(SoundSize.S_16BITS, codedFrame.soundSize)
        assertEquals(SoundType.STEREO, codedFrame.soundType)
        assertEquals(AACPacketType.RAW, codedFrame.aacPacketType)
        assertEquals(4, codedFrame.body.size)
        assertEquals(0x01020304, codedFrame.body.asRawSource().buffered().readInt())
    }

    @Test
    fun `create aac sequence header test`() {
        val factory = AACAudioDataFactory(
            soundRate = SoundRate.F_44100HZ,
            soundSize = SoundSize.S_16BITS,
            soundType = SoundType.STEREO
        )
        val sequenceStart = factory.sequenceStart(Buffer().apply {
            writeInt(0x01020304)
        }, 4)

        assertEquals(SoundRate.F_44100HZ, sequenceStart.soundRate)
        assertEquals(SoundSize.S_16BITS, sequenceStart.soundSize)
        assertEquals(SoundType.STEREO, sequenceStart.soundType)
        assertEquals(AACPacketType.SEQUENCE_HEADER, sequenceStart.aacPacketType)
        assertEquals(4, sequenceStart.body.size)
        assertEquals(0x01020304, sequenceStart.body.asRawSource().buffered().readInt())
    }
}