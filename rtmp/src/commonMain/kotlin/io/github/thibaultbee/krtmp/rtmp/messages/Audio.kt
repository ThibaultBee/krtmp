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

import io.github.thibaultbee.krtmp.flv.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.rtmp.messages.chunk.ChunkStreamId
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered

/**
 * Creates an audio message with a [ByteArray] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The byte array containing the audio data.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the audio channel.
 */
fun Audio(
    timestamp: Int,
    messageStreamId: Int,
    payload: ByteArray,
    chunkStreamId: Int = ChunkStreamId.AUDIO_CHANNEL.value
) = Audio(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = ByteArrayBackedRawSource(payload),
    payloadSize = payload.size,
    chunkStreamId = chunkStreamId
)

/**
 * Creates an audio message with a [Buffer] payload.
 *
 * @param timestamp The timestamp of the message.
 * @param messageStreamId The stream ID of the message.
 * @param payload The buffer containing the audio data.
 * @param payloadSize The size of the payload in bytes.
 * @param chunkStreamId The chunk stream ID for this message, defaulting to the audio channel.
 */
fun Audio(
    timestamp: Int,
    messageStreamId: Int,
    payload: Buffer,
    chunkStreamId: Int = ChunkStreamId.AUDIO_CHANNEL.value
) = Audio(
    timestamp = timestamp,
    messageStreamId = messageStreamId,
    payload = payload,
    payloadSize = payload.size.toInt(),
    chunkStreamId = chunkStreamId
)

/**
 * An audio message in RTMP.
 *
 * @property timestamp The timestamp of the message.
 * @property messageStreamId The stream ID of the message.
 * @property payload The payload provider containing the audio data.
 * @property chunkStreamId The chunk stream ID for this message, defaulting to the audio channel.
 */
class Audio internal constructor(
    timestamp: Int,
    messageStreamId: Int,
    payload: RawSource,
    payloadSize: Int,
    chunkStreamId: Int = ChunkStreamId.AUDIO_CHANNEL.value
) :
    Message(
        chunkStreamId = chunkStreamId,
        messageStreamId = messageStreamId,
        timestamp = timestamp,
        messageType = MessageType.AUDIO,
        payload = payload,
        payloadSize = payloadSize,
    ) {
    override fun toString(): String {
        return "Audio(timestamp=$timestamp, messageStreamId=$messageStreamId, payload=$payload)"
    }
}


fun Audio.decode() =
    AudioData.decode(payload.buffered(), payloadSize, false)
