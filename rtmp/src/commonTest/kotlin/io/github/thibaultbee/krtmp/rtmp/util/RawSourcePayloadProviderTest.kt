package io.github.thibaultbee.krtmp.rtmp.util

import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ByteArrayChunkData
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.RawSourceChunkData
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawSourcePayloadProviderTest {
    @Test
    fun `test byte array with a single chunk`() {
        val array = byteArrayOf(0x01, 0x02, 0x03)
        val payload = ByteArrayPayloadProvider(array)
        val chunkData = payload.getChunkPayload(5)

        // Verify that the chunk data is of type RawSourceChunkData
        assertTrue(chunkData is ByteArrayChunkData)

        // Verify that the source
        assertTrue(chunkData.size == 3)

        assertEquals(0x01, chunkData.array[0])
        assertEquals(0x02, chunkData.array[1])
        assertEquals(0x03, chunkData.array[2])
    }

    @Test
    fun `test raw source with a single chunk`() {
        val buffer = Buffer().apply {
            writeByte(0x01)
            writeByte(0x02)
            writeByte(0x03)
        }
        val payload = RawSourcePayloadProvider(buffer, 3)
        val chunkData = payload.getChunkPayload(5)

        // Verify that the chunk data is of type RawSourceChunkData
        assertTrue(chunkData is RawSourceChunkData)

        // Verify that the source
        assertTrue(chunkData.size == 3)

        val source = chunkData.source.buffered()
        assertEquals(0x01, source.readByte())
        assertEquals(0x02, source.readByte())
        assertEquals(0x03, source.readByte())
    }

    @Test
    fun `test raw source with multiple chunk`() {
        val buffer = Buffer().apply {
            writeByte(0x01)
            writeByte(0x02)
            writeByte(0x03)
        }
        val payload = RawSourcePayloadProvider(buffer, 3)

        var chunkData = payload.getChunkPayload(2)

        // Verify that the chunk data is of type RawSourceChunkData
        assertTrue(chunkData is RawSourceChunkData)

        // Verify that the source
        assertTrue(chunkData.size == 2)

        var source = chunkData.source.buffered()
        assertEquals(0x01, source.readByte())
        assertEquals(0x02, source.readByte())

        chunkData = payload.getChunkPayload(2)

        // Verify that the chunk data is of type RawSourceChunkData
        assertTrue(chunkData is RawSourceChunkData)

        // Verify that the source
        assertTrue(chunkData.size == 1)

        source = chunkData.source.buffered()
        assertEquals(0x03, source.readByte())
    }
}