/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.client

import io.github.thibaultbee.krtmp.common.sources.ByteBufferBackedRawSource
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpSettings
import java.nio.ByteBuffer


/**
 * Writes an audio frame from a [ByteBuffer].
 *
 * The frame must be wrapped in a FLV body.
 *
 * @param buffer the audio frame to write
 * @param timestampMs the timestamp of the frame in milliseconds
 */
suspend fun RtmpClient.writeAudio(buffer: ByteBuffer, timestampMs: Int) =
    writeAudio(ByteBufferBackedRawSource(buffer), buffer.remaining(), timestampMs)

/**
 * Writes a video frame from a [ByteBuffer],
 *
 * The frame must be wrapped in a FLV body.
 *
 * @param buffer the video frame to write
 * @param timestampMs the timestamp of the frame in milliseconds
 */
suspend fun RtmpClient.writeVideo(buffer: ByteBuffer, timestampMs: Int) =
    writeVideo(ByteBufferBackedRawSource(buffer), buffer.remaining(), timestampMs)

/**
 * Writes the SetDataFrame from a [ByteBuffer].
 * It must be called after [RtmpClient.publish] and before sending audio or video frames.
 *
 * Expected AMF format is the one set in [RtmpSettings.amfVersion].
 *
 * @param onMetadata the on metadata to send
 * @param timestampMs the timestamp of the metadata in milliseconds (usually 0)
 */
suspend fun RtmpClient.writeSetDataFrame(onMetadata: ByteBuffer, timestampMs: Int = 0) =
    writeSetDataFrame(
        onMetadata = ByteBufferBackedRawSource(onMetadata),
        onMetadataSize = onMetadata.remaining(),
        timestampMs = timestampMs
    )
