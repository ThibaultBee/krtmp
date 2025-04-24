/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.krtmp.flv

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.models.tags.FLVData
import io.github.thibaultbee.krtmp.flv.models.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.models.tags.aacAudioData
import io.github.thibaultbee.krtmp.flv.models.tags.avcHeaderVideoData
import io.github.thibaultbee.krtmp.flv.models.tags.avcVideoData
import io.github.thibaultbee.krtmp.flv.models.util.FlvHeader
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Creates a [FLVMuxer] dedicated to write to a file.
 *
 * @param path the path to the file
 * @param amfVersion the AMF version to use
 * @return a [FLVMuxer]
 */
fun FlvMuxer(
    path: Path,
    amfVersion: AmfVersion = AmfVersion.AMF0
): FLVMuxer {
    val sink = SystemFileSystem.sink(path, false)
    return FLVMuxer(sink.buffered(), amfVersion)
}

/**
 * Muxer for FLV format.
 *
 * Usage:
 * ```
 * val muxer = FlvMuxer(sink, AmfVersion.AMF0) // or FlvMuxer(path, AmfVersion.AMF0) to write to a file
 * // Encode FLV header if needed
 * muxer.encodeFlvHeader(hasAudio = true, hasVideo = true)
 * // Encode onMetadata
 * muxer.encode(0, OnMetadata(...))
 * // Encode video and audio frames
 * muxer.encode(0 /* timestamp */, avcHeaderVideoData(...))
 * muxer.encode(0 /* timestamp */, avcVideoData(...))
 * ...
 * // Don't forget to close the sink
 * sink.close()
 * ```
 *
 * @param output the output stream to write to
 * @param amfVersion the AMF version to use
 */
class FLVMuxer(private val output: Sink, private val amfVersion: AmfVersion = AmfVersion.AMF0) {
    private var hasEncoded = false

    /**
     * Encodes a [FLVTag] to the muxer.
     *
     * @param tag the [FLVTag] to encode
     */
    fun encode(tag: FLVTag) {
        if (!hasEncoded) {
            hasEncoded = true
            output.writeInt(0) // PreviousTagSize0
        }
        tag.encode(output, amfVersion)
        output.writeInt(tag.data.getSize(amfVersion)) // PreviousTagSize
    }

    /**
     * Encodes the FLV header.
     *
     * The FLV header is a 9-byte structure that contains the following fields:
     * - Signature: "FLV" (3 bytes)
     * - Version: 1 (1 byte)
     * - Flags: (1 byte)
     * - Data Offset: (4 bytes)
     *
     * @param hasAudio true if the FLV file contains audio data, false otherwise
     * @param hasVideo true if the FLV file contains video data, false otherwise
     */
    fun encodeFlvHeader(hasAudio: Boolean, hasVideo: Boolean) {
        FlvHeader(hasAudio, hasVideo).encode(output)
    }
}

/**
 * Encodes a [FLVData] to the muxer.
 *
 * This method is a convenience method that wraps the [FLVTag] encoding.
 * The project comes with [FLVData] factories such as [avcHeaderVideoData], [avcVideoData], [aacAudioData], etc.
 *
 * @param timestampMs the timestamp in milliseconds
 * @param data the [FLVData] to encode
 * @throws IllegalArgumentException if the stream is not found
 */
fun FLVMuxer.encode(
    timestampMs: Int,
    data: FLVData
) {
    encode(FLVTag(timestampMs, data))
}
