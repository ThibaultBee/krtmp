package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.common.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import java.nio.ByteBuffer

/**
 * Creates a audio message with a [ByteBuffer] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The byte buffer containing the audio data.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the video channel.
 */
fun Audio(
    timestamp: Int,
    messageStreamId: Int,
    payload: ByteBuffer,
    chunkStreamId: Int = ChunkStreamId.AUDIO_CHANNEL.value
) = Audio(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = ByteBufferBackedRawSource(payload),
    payloadSize = payload.remaining(),
    chunkStreamId = chunkStreamId
)