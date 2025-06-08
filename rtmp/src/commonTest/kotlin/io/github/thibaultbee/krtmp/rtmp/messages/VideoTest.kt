package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.rtmp.Resource
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

class VideoTest {
    @Test
    fun `write video after a sequence header with same timestamp`() = runTest {
        val expected = Resource("frames/video/avc/key/expected").toByteArray()

        val raw = Resource("frames/video/avc/key/raw").toByteArray()
        val video = Video(0, 10, Buffer().apply { write(raw) }, raw.size)

        val writeChannel = ByteChannel(false)

        video.write(writeChannel, 128, Video(0, 10, Buffer(), 0)) // Empty previous audio
        writeChannel.flush()

        val actual = ByteArray(writeChannel.availableForRead)
        writeChannel.readAvailable(actual, 0, actual.size)

        assertContentEquals(expected, actual)
    }
}