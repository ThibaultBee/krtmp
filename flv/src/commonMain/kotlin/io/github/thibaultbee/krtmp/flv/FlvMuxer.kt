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

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.models.Frame
import io.github.thibaultbee.krtmp.flv.models.VideoFrame
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvConfig
import io.github.thibaultbee.krtmp.flv.models.packets.FlvHeader
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import io.github.thibaultbee.krtmp.flv.models.packets.Packet
import io.github.thibaultbee.krtmp.flv.models.tags.OnMetadata
import io.github.thibaultbee.krtmp.flv.models.tags.factories.AVTagsFactory
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Creates a [FlvMuxer] dedicated to write to a file.
 *
 * @param path the path to the file
 * @return a [FlvMuxer]
 */
fun FlvMuxer(
    path: Path
): FlvMuxer {
    val sink = SystemFileSystem.sink(path, false)
    val listener = object : FlvMuxer.Listener {
        override fun onOutputPacket(outputPacket: Packet) {
            outputPacket.writeToSink(sink.buffered())
        }
    }
    return FlvMuxer().apply { addListener(listener) }
}

/**
 * Muxer for FLV format.
 *
 * Usage:
 * ```
 * val muxer = FlvMuxer() // or FlvMuxer(path) to write to a file
 * // Register streams
 * val videoStreamId = muxer.addStream(videoConfig)
 * val audioStreamId = muxer.addStream(audioConfig)
 * // Start stream
 * muxer.startStream()
 * // Write frames
 * muxer.write(videoFrame, videoStreamId)
 * muxer.write(audioFrame, audioStreamId)
 * ...
 * // Stop stream
 * muxer.stopStream()
 * ```
 */
class FlvMuxer {
    private val streams = mutableListOf<FlvConfig>()
    private val hasAudio: Boolean
        get() = streams.any { it.mimeType.type == MimeType.Type.AUDIO }
    private val hasVideo: Boolean
        get() = streams.any { it.mimeType.type == MimeType.Type.VIDEO }

    private val videoStreamId: Int
        get() = streams.indexOfFirst { it.mimeType.type == MimeType.Type.VIDEO }
    private val audioStreamId: Int
        get() = streams.indexOfFirst { it.mimeType.type == MimeType.Type.AUDIO }

    private val listeners = mutableListOf<Listener>()

    /**
     * Writes a frame to the muxer.
     * The stream id is inferred from the frame type.
     *
     * You have to register the stream with [addStream] or [addStreams] before calling this method.
     *
     * @param frame the frame to write
     * @throws IllegalArgumentException if the stream is not found
     */
    fun write(frame: Frame) {
        val streamId = if (frame is VideoFrame) {
            videoStreamId
        } else {
            audioStreamId
        }
        if (streamId == -1) {
            throw IllegalArgumentException("Stream not found for frame type ${frame.mimeType}")
        }
        write(frame, streamId)
    }

    /**
     * Writes a frame to the muxer.
     *
     * You have to register the stream with [addStream] or [addStreams] before calling this method.
     *
     * @param frame the frame to write
     * @param streamId the stream id (returned by [addStream] or [addStreams])
     */
    fun write(frame: Frame, streamId: Int) {
        val flvTags = AVTagsFactory(frame, streams[streamId]).build()
        flvTags.forEach { tag ->
            listeners.forEach { listener ->
                listener.onOutputPacket(FlvTagPacket(tag))
            }
        }
    }

    /**
     * Adds a stream to the muxer.
     * You can't add two streams of the same type.
     * To be called before [startStream].
     *
     * @param config the stream configuration
     * @return the stream id
     */
    fun addStream(config: FlvConfig): Int {
        require(streams.none { it.mimeType.type == config.mimeType.type }) {
            "FLV only supports one stream of a kind"
        }
        streams.add(config)
        return streams.indexOf(config)
    }

    /**
     * Adds streams to the muxer.
     * You can't add two streams of the same type.
     * To be called before [startStream].
     *
     * @param configs the stream configurations
     * @return the stream id
     */
    fun addStreams(configs: List<FlvConfig>) = configs.associateWith { config ->
        addStream(config)
    }

    /**
     * Removes a stream from the muxer.
     * To be called before [startStream].
     *
     * @param streamId the stream id
     */
    fun removeStream(streamId: Int) = streams.removeAt(streamId)

    /**
     * Removes a stream from the muxer.
     * To be called before [startStream].
     *
     * @param config the stream configuration
     */
    fun removeStream(config: FlvConfig) = streams.remove(config)

    /**
     * Starts the stream.
     * It writes the header (for file) and the metadata.
     */
    fun startStream() {
        // Header
        listeners.forEach {
            it.onOutputPacket(
                FlvHeader(hasAudio, hasVideo)
            )
        }

        // Metadata
        listeners.forEach {
            it.onOutputPacket(
                FlvTagPacket(OnMetadata(0, streams))
            )
        }
    }

    /**
     * Stops the stream.
     * It clears the registered streams.
     * To restart the stream, call [addStreams] (or [addStream]) and [startStream] again.
     */
    fun stopStream() {
        streams.clear()
    }

    /**
     * Adds a listener to the muxer.
     *
     * @param listener the listener
     */
    fun addListener(listener: Listener) =
        listeners.add(listener)

    /**
     * Removes a listener from the muxer.
     *
     * @param listener the listener
     */
    fun removeListener(listener: Listener) =
        listeners.remove(listener)

    /**
     * Listener for output packets
     */
    interface Listener {
        /**
         * Called when a packet is ready to be used
         */
        fun onOutputPacket(outputPacket: Packet)
    }

}
