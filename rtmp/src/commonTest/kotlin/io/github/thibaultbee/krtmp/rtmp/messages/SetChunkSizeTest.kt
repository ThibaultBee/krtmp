package io.github.thibaultbee.krtmp.rtmp.messages

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class SetChunkSizeTest {
    @Test
    fun `write set chunk size`() = runTest {
        val expected = byteArrayOf(2, 0, 0, 0, 0, 0, 4, 1, 0, 0, 0, 0, 0, 0, 1, 0)
        val setChunkSize = SetChunkSize(0, 256)

        val writeChannel = ByteChannel(false)
        setChunkSize.write(writeChannel)
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}