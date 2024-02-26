package io.github.thibaultbee.krtmp.flv.util

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals


class FLVHeaderTest {
    @Test
    fun `test audio video flv header`() {
        val expected = byteArrayOf(
            0x46,
            0x4C,
            0x56,
            0x01,
            0x05,
            0x00,
            0x00,
            0x00,
            0x09
        ) // FLV header
        val flvHeader = FLVHeader(hasAudio = true, hasVideo = true)
        val buffer = Buffer()
        flvHeader.encode(buffer)
        assertContentEquals(expected, buffer.readByteArray())
    }
}