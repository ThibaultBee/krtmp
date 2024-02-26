package io.github.thibaultbee.krtmp.flv.models.packets

import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals


class FlvHeaderTest {
    @Test
    fun `test audio video flv header`() {
        val expected = byteArrayOf(0x46, 0x4C, 0x56, 0x01, 0x05, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00) // FLV header + PreviousTagSize0
        val flvHeader = FlvHeader(hasAudio = true, hasVideo = true)
        assertContentEquals(expected, flvHeader.readRawSource().buffered().readByteArray())
    }
}