package io.github.thibaultbee.krtmp.common.sources

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ByteBufferBackedRawSourceTest {
    @Test
    fun `read from empty source`() {
        val byteBuffer = ByteBuffer.allocate(0)
        val byteBufferRawSource = ByteBufferBackedRawSource(byteBuffer)
        val buffer = Buffer()

        val size = byteBufferRawSource.readAtMostTo(buffer, 4)
        assertContentEquals(byteBuffer.array(), buffer.readByteArray())
        assertEquals(-1, size)
    }

    @Test
    fun `read from source`() {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.putInt(0x01020304)
        byteBuffer.flip() // Prepare the buffer for reading
        val byteBufferRawSource = ByteBufferBackedRawSource(byteBuffer)

        var buffer = Buffer()
        var size = byteBufferRawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x01, 0x02), buffer.readByteArray())
        assertEquals(2, size)

        buffer = Buffer()
        size = byteBufferRawSource.readAtMostTo(buffer, 2)
        assertContentEquals(byteArrayOf(0x03, 0x04), buffer.readByteArray())
        assertEquals(2, size)
    }

    @Test
    fun `read from exhausted source`() {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.putInt(0x01020304)
        byteBuffer.flip() // Prepare the buffer for reading
        val byteBufferRawSource = ByteBufferBackedRawSource(byteBuffer)
        val buffer = Buffer()

        byteBufferRawSource.readAtMostTo(buffer, 4)
        assertEquals(-1, byteBufferRawSource.readAtMostTo(buffer, 4))
    }

    @Test
    fun `read above source size`() {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.putInt(0x01020304)
        byteBuffer.flip() // Prepare the buffer for reading
        val byteBufferRawSource = ByteBufferBackedRawSource(byteBuffer)
        val buffer = Buffer()

        val size = byteBufferRawSource.readAtMostTo(buffer, 5)
        assertContentEquals(byteBuffer.array(), buffer.readByteArray(4))
        assertEquals(4, size)
    }

    @Test
    fun `byte count out of range test`() {
        val byteBuffer = ByteBuffer.allocate(4)
        byteBuffer.putInt(0x01020304)
        byteBuffer.flip() // Prepare the buffer for reading
        val byteBufferRawSource = ByteBufferBackedRawSource(byteBuffer)
        val buffer = Buffer()

        try {
            byteBufferRawSource.readAtMostTo(buffer, -1)
            fail("Should throw IllegalArgumentException")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }
    }
}