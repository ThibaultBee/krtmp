/*
 * Copyright (C) 2023 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered

/**
 * Creates a video message with a [ByteArray] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The byte array containing the video data.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the video channel.
 */
fun Video(
    timestamp: Int,
    messageStreamId: Int,
    payload: ByteArray,
    chunkStreamId: Int = ChunkStreamId.VIDEO_CHANNEL.value
) = Video(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = ByteArrayBackedRawSource(payload),
    payloadSize = payload.size,
    chunkStreamId = chunkStreamId
)

/**
 * Creates a video message with a [Buffer] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The buffer containing the video data.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the video channel.
 */
fun Video(
    timestamp: Int,
    messageStreamId: Int,
    payload: Buffer,
    chunkStreamId: Int = ChunkStreamId.VIDEO_CHANNEL.value
) = Video(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = payload,
    payloadSize = payload.size.toInt(),
    chunkStreamId = chunkStreamId
)

/**
 * Creates a video message with a [RawSource] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The raw source containing the video data.
 * @param payloadSize The size of the payload in bytes.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the video channel.
 */
class Video internal constructor(
    timestamp: Int,
    messageStreamId: Int,
    payload: RawSource,
    payloadSize: Int,
    chunkStreamId: Int = ChunkStreamId.VIDEO_CHANNEL.value
) :
    Message(
        chunkStreamId = chunkStreamId,
        messageStreamId = messageStreamId,
        timestamp = timestamp,
        messageType = MessageType.VIDEO,
        payload = payload,
        payloadSize = payloadSize
    ) {
    override fun toString(): String {
        return "Video(timestamp=$timestamp, messageStreamId=$messageStreamId, payload=$payload)"
    }
}

fun Video.decode() =
    VideoData.decode(payload.buffered(), payloadSize, false)
