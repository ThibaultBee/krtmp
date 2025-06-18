package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.flv.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import java.nio.ByteBuffer

/**
 * Creates a video message with a [ByteBuffer] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The byte buffer containing the video data.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the video channel.
 */
fun Video(
    timestamp: Int,
    messageStreamId: Int,
    payload: ByteBuffer,
    chunkStreamId: Int = ChunkStreamId.VIDEO_CHANNEL.value
) = Video(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = ByteBufferBackedRawSource(payload),
    payloadSize = payload.remaining(),
    chunkStreamId = chunkStreamId
)