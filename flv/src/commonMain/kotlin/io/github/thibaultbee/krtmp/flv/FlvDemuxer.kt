/*
 * Copyright (C) 2024 Thibault B.
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
import io.github.thibaultbee.krtmp.flv.models.AACFrame
import io.github.thibaultbee.krtmp.flv.models.AV1Frame
import io.github.thibaultbee.krtmp.flv.models.AVCFrame
import io.github.thibaultbee.krtmp.flv.models.Frame
import io.github.thibaultbee.krtmp.flv.models.HEVCFrame
import io.github.thibaultbee.krtmp.flv.models.NaluRawSource
import io.github.thibaultbee.krtmp.flv.models.SizedRawSource
import io.github.thibaultbee.krtmp.flv.models.VP9Frame
import io.github.thibaultbee.krtmp.flv.models.av.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.models.packets.FlvHeader
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import io.github.thibaultbee.krtmp.flv.models.tags.AACPacketType
import io.github.thibaultbee.krtmp.flv.models.tags.AVCPacketType
import io.github.thibaultbee.krtmp.flv.models.tags.AVFlvTag
import io.github.thibaultbee.krtmp.flv.models.tags.AudioTag
import io.github.thibaultbee.krtmp.flv.models.tags.ExtendedVideoTag
import io.github.thibaultbee.krtmp.flv.models.tags.FrameType
import io.github.thibaultbee.krtmp.flv.models.tags.LegacyVideoTag
import io.github.thibaultbee.krtmp.flv.models.tags.OnMetadata
import io.github.thibaultbee.krtmp.flv.models.tags.PacketType
import io.github.thibaultbee.krtmp.flv.models.tags.ScriptTag
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

/**
 * Creates a [FlvDemuxer] dedicated to read from a file.
 *
 * @param path the path to the file
 * @return a [FlvDemuxer]
 */
fun FlvDemuxer(
    path: Path,
    listener: FlvDemuxer.Listener
): FlvDemuxer {
    val source = SystemFileSystem.source(path)
    return FlvDemuxer().apply {
        addListener(listener)
        readAll(source.buffered())
    }
}

/**
 * Demuxer for FLV format.
 */
class FlvDemuxer {
    private val listeners = mutableListOf<Listener>()
    private var previousAudioSequenceHeaderTag: AudioTag? = null
    private var previousVideoSequenceHeaderTag: AVFlvTag? = null

    private var _streams: List<FlvConfig> = emptyList()

    /**
     * List of streams.
     * Only available once [OnMetadata] has been read.
     */
    val streams: List<FlvConfig>
        get() = _streams

    fun readAll(source: Source) {
        while (!source.exhausted()) {
            read(source)
        }
    }

    /**
     * Read a single FLV frames.
     *
     * @param source the source to read from. It can contains one or multiple frames.
     */
    fun read(source: Source) {
        val peek = source.peek()
        val isHeader = try {
            peek.readString(3) == "FLV"
        } catch (e: Exception) {
            false
        }
        if (isHeader) {
            FlvHeader.read(source)
            // Skip header
        } else {
            val tag = FlvTagPacket.read(source)
            if (tag is AudioTag && tag.tagHeader.aacPacketType == AACPacketType.SEQUENCE_HEADER) {
                previousAudioSequenceHeaderTag = tag
                return
            } else if (tag is LegacyVideoTag && tag.tagHeader.packetType == AVCPacketType.SEQUENCE_HEADER) {
                previousVideoSequenceHeaderTag = tag
                return
            } else if (tag is ExtendedVideoTag && tag.tagHeader.packetType == PacketType.SEQUENCE_START) {
                previousVideoSequenceHeaderTag = tag
                return
            }

            val frame: Frame? = when (tag) {
                is AudioTag -> {
                    when (tag.tagHeader.soundFormat) {
                        SoundFormat.AAC -> {
                            val frame = AACFrame(
                                SizedRawSource(tag.source, tag.sourceSize),
                                tag.timestampMs,
                                previousAudioSequenceHeaderTag?.let {
                                    SizedRawSource(
                                        it.source,
                                        it.sourceSize
                                    )
                                })
                            previousAudioSequenceHeaderTag = null
                            frame
                        }

                        else -> throw NotImplementedError("SoundFormat ${tag.tagHeader.soundFormat} is not implemented")
                    }
                }

                is LegacyVideoTag -> {
                    when (tag.tagHeader.codecID) {
                        CodecID.AVC -> {
                            val frame = AVCFrame(
                                NaluRawSource(tag.source, tag.sourceSize),
                                tag.timestampMs,
                                tag.tagHeader.frameType == FrameType.KEY,
                                previousVideoSequenceHeaderTag?.let {
                                    SizedRawSource(
                                        it.source,
                                        it.sourceSize
                                    )
                                })
                            previousVideoSequenceHeaderTag = null
                            frame
                        }

                        else -> throw NotImplementedError("CodecID ${tag.tagHeader.codecID} is not implemented")
                    }
                }

                is ExtendedVideoTag -> {
                    when (tag.tagHeader.mimeType) {
                        MimeType.VIDEO_HEVC -> {
                            val frame = HEVCFrame(
                                NaluRawSource(tag.source, tag.sourceSize),
                                tag.timestampMs,
                                tag.tagHeader.frameType == FrameType.KEY,
                                previousVideoSequenceHeaderTag?.let {
                                    SizedRawSource(
                                        it.source,
                                        it.sourceSize
                                    )
                                })
                            previousVideoSequenceHeaderTag = null
                            frame
                        }

                        MimeType.VIDEO_VP9 -> {
                            val frame = VP9Frame(
                                NaluRawSource(tag.source, tag.sourceSize),
                                tag.timestampMs,
                                tag.tagHeader.frameType == FrameType.KEY,
                                previousVideoSequenceHeaderTag?.let {
                                    SizedRawSource(
                                        it.source,
                                        it.sourceSize
                                    )
                                })
                            previousVideoSequenceHeaderTag = null
                            frame
                        }

                        MimeType.VIDEO_AV1 -> {
                            val frame = AV1Frame(
                                NaluRawSource(tag.source, tag.sourceSize),
                                tag.timestampMs,
                                tag.tagHeader.frameType == FrameType.KEY,
                                previousVideoSequenceHeaderTag?.let {
                                    SizedRawSource(
                                        it.source,
                                        it.sourceSize
                                    )
                                })
                            previousVideoSequenceHeaderTag = null
                            frame
                        }

                        else -> throw NotImplementedError("MimeType ${tag.tagHeader.mimeType} is not implemented")
                    }
                }

                is ScriptTag -> {
                    when (tag) {
                        is OnMetadata -> {
                            listeners.forEach { listener ->
                                listener.onMetadata(tag)
                            }
                            null
                        }

                        else -> throw NotImplementedError("ScriptTag ${tag.name} is not implemented")
                    }
                }
            }

            frame?.let {
                listeners.forEach { listener ->
                    listener.onOutputFrame(it)
                }
            }
        }
    }

    /**
     * Adds a listener to the demuxer.
     *
     * @param listener the listener
     */
    fun addListener(listener: Listener) =
        listeners.add(listener)

    /**
     * Removes a listener from the demuxer.
     *
     * @param listener the listener
     */
    fun removeListener(listener: Listener) =
        listeners.remove(listener)

    /**
     * Listener for output frames
     */
    interface Listener {
        /**
         * Called when metadata is read
         */
        fun onMetadata(metadata: OnMetadata) {}

        /**
         * Called when a frame is ready to be read
         */
        fun onOutputFrame(frame: Frame)
    }
}
