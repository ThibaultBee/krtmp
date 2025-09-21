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
 * @param timestamp the timestamp of the frame
 * @param buffer the audio frame to write
 */
suspend fun RtmpClient.writeAudio(timestamp: Int, buffer: ByteBuffer) =
    writeAudio(timestamp, ByteBufferBackedRawSource(buffer), buffer.remaining())

/**
 * Writes a video frame from a [ByteBuffer],
 *
 * The frame must be wrapped in a FLV body.
 *
 * @param timestamp the timestamp of the frame
 * @param buffer the video frame to write
 */
suspend fun RtmpClient.writeVideo(timestamp: Int, buffer: ByteBuffer) =
    writeVideo(timestamp, ByteBufferBackedRawSource(buffer), buffer.remaining())

/**
 * Writes the SetDataFrame from a [ByteBuffer].
 * It must be called after [publish] and before sending audio or video frames.
 *
 * Expected AMF format is the one set in [RtmpSettings.amfVersion].
 *
 * @param onMetadata the on metadata to send
 */
suspend fun RtmpClient.writeSetDataFrame(onMetadata: ByteBuffer) = writeSetDataFrame(
    onMetadata = ByteBufferBackedRawSource(onMetadata), onMetadataSize = onMetadata.remaining()
)
