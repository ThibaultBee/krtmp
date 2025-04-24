package io.github.thibaultbee.krtmp.flv.extensions

import io.github.thibaultbee.krtmp.flv.util.PacketWriter
import io.github.thibaultbee.krtmp.flv.util.readBuffer
import kotlinx.io.readAtMostTo
import java.nio.ByteBuffer

/**
 * Writes the packet to a [ByteBuffer].
 *
 * @return the [ByteBuffer] containing the packet
 */
fun PacketWriter.readByteBuffer(): ByteBuffer {
    val buffer = readBuffer()
    val byteBuffer = ByteBuffer.allocate(buffer.size.toInt())
    buffer.readAtMostTo(byteBuffer)
    return byteBuffer
}
