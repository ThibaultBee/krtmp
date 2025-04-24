package io.github.thibaultbee.krtmp.flv.bitreaders

import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class BitReaderTest {
    @Test
    fun `request test`() {
        val buffer = Buffer().apply {
            writeByte(0x47)
        }

        val reader = BitReader(buffer)
        assertEquals(true, reader.request(8))
        assertEquals(false, reader.request(9))

        reader.readBoolean()
        assertEquals(true, reader.request(7))
        assertEquals(false, reader.request(8))

        reader.readByte(7)
        assertEquals(false, reader.request(1))
    }

    @Test
    fun `read from bit reader`() {
        val buffer = Buffer().apply {
            writeByte(0x47)
        }

        val reader = BitReader(buffer)
        assertEquals(0x2, reader.readByte(3))
        assertEquals(0x0, reader.readByte(2))
        assertEquals(0x1, reader.readByte(1))
        assertEquals(0x3, reader.readByte(2))
        try {
            reader.readByte(1)
            fail("Should throw exception")
        } catch (e: Exception) {
            // Expected
        }
    }
}