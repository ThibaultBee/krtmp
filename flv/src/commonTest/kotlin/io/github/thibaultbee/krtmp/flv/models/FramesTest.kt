package io.github.thibaultbee.krtmp.flv.models

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FramesTest {
    @Test
    fun `build nalu raw source from array in avcc format`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)
        val sizedRawSource = NaluRawSource(array)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from array in annex b`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x00, 0x00)
        val sizedRawSource = NaluRawSource(array)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from array without prefix`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00)
        val sizedRawSource = NaluRawSource(array)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer in avcc format`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeInt(2)
            writeShort(0)
        }
        val sizedRawSource = NaluRawSource(buffer)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer in annex b`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeInt(1)
            writeShort(0)
        }
        val sizedRawSource = NaluRawSource(buffer)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer without prefix`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeShort(0)
        }
        val sizedRawSource = NaluRawSource(buffer)
        assertEquals(6, sizedRawSource.byteCount)

        val actual = Buffer().apply { sizedRawSource.source.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

}