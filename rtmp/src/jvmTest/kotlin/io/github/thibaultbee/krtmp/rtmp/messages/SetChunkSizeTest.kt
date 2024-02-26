package io.github.thibaultbee.krtmp.rtmp.messages

import io.ktor.utils.io.ByteChannelSequentialJVM
import io.ktor.utils.io.core.internal.ChunkBuffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class SetChunkSizeTest {
    @Test
    fun `write set chunk size`() = runTest {
        val expected = byteArrayOf(2, 0, 0, 0, 0, 0, 4, 1, 0, 0, 0, 0, 0, 0, 1, 0)
        val setChunkSize = SetChunkSize(0, 256)

        val writeChannel = ByteChannelSequentialJVM(ChunkBuffer.Empty, false)
        setChunkSize.write(writeChannel)
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)
        
        assertContentEquals(expected, actual)
    }
}