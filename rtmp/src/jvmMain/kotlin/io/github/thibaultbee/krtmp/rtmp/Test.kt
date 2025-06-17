package io.github.thibaultbee.krtmp.rtmp

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByteBuffer
import java.nio.ByteBuffer

class Test {
    suspend fun test(byteWriteChannel: ByteWriteChannel) {
        byteWriteChannel.writeByteBuffer(ByteBuffer.allocate(3))
    }
}