package io.github.thibaultbee.krtmp.flv.sources

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals

class RawSourceWithSizeTest {
    @Test
    fun `test readAtMostTo with size is equal to buffer size`() {
        val expected = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val buffer = Buffer().apply {
            write(expected)
        }
        val sizedRawSource = RawSourceWithSize(buffer, 4)
        val actual = sizedRawSource.buffered().readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `test readAtMostTo with size is less buffer size`() {
        val expected = byteArrayOf(0x01, 0x02)
        val buffer = Buffer().apply {
            write(expected + byteArrayOf(0x03, 0x04))
        }
        val sizedRawSource = RawSourceWithSize(buffer, 2)
        val actual = sizedRawSource.buffered().readByteArray()
        assertContentEquals(expected, actual)
    }

}