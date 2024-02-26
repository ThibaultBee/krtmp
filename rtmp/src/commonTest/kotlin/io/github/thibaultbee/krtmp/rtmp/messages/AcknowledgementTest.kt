package io.github.thibaultbee.krtmp.rtmp.messages

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class AcknowledgementTest {
    @Test
    fun `write acknowledgement`() = runTest {
        val expected = byteArrayOf(2, 0, 0, 0, 0, 0, 4, 3, 0, 0, 0, 0, 0, 0, 0, 10)
        val acknowledgement = Acknowledgement(0, 10)

        val writeChannel = ByteChannel(false)
        acknowledgement.write(writeChannel)
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}