package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.rtmp.Resource
import io.ktor.utils.io.ByteChannelSequentialJVM
import io.ktor.utils.io.core.internal.ChunkBuffer
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class AudioTest {
    @Test
    fun `write raw audio after a sequence header with same timestamp`() = runTest {
        val expected = Resource("frames/audio/aac/raw/expected").toByteArray()

        val raw = Resource("frames/audio/aac/raw/raw").toByteArray()
        val audio = Audio(0, 10, Buffer().apply { write(raw) })

        val writeChannel = ByteChannelSequentialJVM(ChunkBuffer.Empty, false)

        audio.write(writeChannel, 128, Audio(0, 10, Buffer())) // Empty previous audio
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }

    @Test
    fun `write sequence header audio`() = runTest {
        val expected = Resource("frames/audio/aac/sequence/expected").toByteArray()

        val raw = Resource("frames/audio/aac/sequence/sequence").toByteArray()
        val audio = Audio(78, 10, Buffer().apply { write(raw) })

        val writeChannel = ByteChannelSequentialJVM(ChunkBuffer.Empty, false)

        audio.write(writeChannel, 128, Audio(0, 10, Buffer()))
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}