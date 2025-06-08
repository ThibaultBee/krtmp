package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.rtmp.Resource
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class AudioTest {
    @Test
    fun `write raw audio after a sequence header with same timestamp`() = runTest {
        val expected = Resource("frames/audio/aac/raw/expected").toByteArray()

        val raw = Resource("frames/audio/aac/raw/raw").toByteArray()
        val audio = Audio(0, 10, Buffer().apply { write(raw) }, raw.size)

        val writeChannel = ByteChannel(false)

        audio.write(writeChannel, 128, Audio(0, 10, Buffer(), 0)) // Empty previous audio
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }

    @Test
    fun `write sequence header audio`() = runTest {
        val expected = Resource("frames/audio/aac/sequence/expected").toByteArray()

        val raw = Resource("frames/audio/aac/sequence/sequence").toByteArray()
        val audio = Audio(78, 10, Buffer().apply { write(raw) }, raw.size)

        val writeChannel = ByteChannel(false)

        audio.write(writeChannel, 128, Audio(0, 10, Buffer(), 0))
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}