package io.github.thibaultbee.krtmp.flv.sources

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class ByteArrayBackedRawSourceTest {
    @Test
    fun `read from empty source`() {
        val byteArray = byteArrayOf()
        val byteArrayRawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        val size = byteArrayRawSource.readAtMostTo(buffer, 4)
        assertContentEquals(byteArray, buffer.readByteArray())
        assertEquals(-1, size)
        assertTrue(byteArrayRawSource.isExhausted)
    }

    @Test
    fun `read from source`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteArrayRawSource = ByteArrayBackedRawSource(byteArray)

        var buffer = Buffer()
        var size = byteArrayRawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x01, 0x02), buffer.readByteArray())
        assertEquals(2, size)
        assertFalse(byteArrayRawSource.isExhausted)

        buffer = Buffer()
        size = byteArrayRawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x03, 0x04), buffer.readByteArray())
        assertEquals(2, size)
        assertTrue(byteArrayRawSource.isExhausted)
    }

    @Test
    fun `read from exhausted source`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteArrayRawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        byteArrayRawSource.readAtMostTo(buffer, 4)
        assertTrue(byteArrayRawSource.isExhausted)
        assertEquals(-1, byteArrayRawSource.readAtMostTo(buffer, 4))
    }

    @Test
    fun `read above source size`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteArrayRawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        val size = byteArrayRawSource.readAtMostTo(buffer, 5)
        assertContentEquals(byteArray, buffer.readByteArray(4))
        assertEquals(4, size)
        assertTrue(byteArrayRawSource.isExhausted)
    }

    @Test
    fun `byte count out of range test`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val byteArrayRawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()
        try {
            byteArrayRawSource.readAtMostTo(buffer, -1)
            fail("Should throw IllegalArgumentException")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }
}