package io.github.thibaultbee.krtmp.common.sources

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

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        val size = rawSource.readAtMostTo(buffer, 4)
        assertContentEquals(byteArray, buffer.readByteArray())
        assertEquals(-1, size)
        assertTrue(rawSource.isExhausted)
    }

    @Test
    fun `read from source`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)

        var buffer = Buffer()
        var size = rawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x01, 0x02), buffer.readByteArray())
        assertEquals(2, size)
        assertFalse(rawSource.isExhausted)

        buffer = Buffer()
        size = rawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x03, 0x04), buffer.readByteArray())
        assertEquals(2, size)
        assertTrue(rawSource.isExhausted)
    }

    @Test
    fun `read from exhausted source`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        rawSource.readAtMostTo(buffer, 4)
        assertTrue(rawSource.isExhausted)
        assertEquals(-1, rawSource.readAtMostTo(buffer, 4))
    }

    @Test
    fun `read above source size`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        val size = rawSource.readAtMostTo(buffer, 5)
        assertContentEquals(byteArray, buffer.readByteArray(4))
        assertEquals(4, size)
        assertTrue(rawSource.isExhausted)
    }

    @Test
    fun `read 0 bytes`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        val size = rawSource.readAtMostTo(buffer, 0)
        assertEquals(buffer.size, 0)
        assertEquals(0, size)
    }

    @Test
    fun `byte count out of range test`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        try {
            rawSource.readAtMostTo(buffer, -1)
            fail("Should throw IllegalArgumentException")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun `constructor validation test`() {
        // Negative start index
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        try {
            ByteArrayBackedRawSource(byteArray, -1)
            fail("Should throw IllegalArgumentException")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }

        // Start index greater than size
        try {
            ByteArrayBackedRawSource(byteArray, 5)
            fail("Should throw IllegalArgumentException")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }

    @Test
    fun `read after close`() {
        val byteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)

        val rawSource = ByteArrayBackedRawSource(byteArray)
        val buffer = Buffer()

        rawSource.close()
        try {
            rawSource.readAtMostTo(buffer, -1)
            fail("Should throw IllegalArgumentException")
        } catch (_: Throwable) {
        }
    }
}