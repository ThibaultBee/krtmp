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
package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.config.CodecID
import io.github.thibaultbee.krtmp.flv.config.VideoFourCC
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.ModEx
import io.github.thibaultbee.krtmp.flv.tags.ModExFactory
import io.github.thibaultbee.krtmp.flv.tags.MultitrackType
import io.github.thibaultbee.krtmp.flv.tags.SinkEncoder
import io.github.thibaultbee.krtmp.flv.util.WithValue
import io.github.thibaultbee.krtmp.flv.util.extensions.shl
import io.github.thibaultbee.krtmp.flv.util.extensions.shr
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.experimental.and

/**
 * Representation of video data in legacy FLV format.
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param codecID the codec ID
 * @param body the coded frame [RawSource]
 * @param packetType the packet type
 * @param compositionTime the composition time. Required for AVC codec, not used for other codecs.
 */
class LegacyVideoData internal constructor(
    frameType: VideoFrameType,
    val codecID: CodecID,
    body: VideoTagBody,
    val packetType: AVCPacketType? = null,
    val compositionTime: Int = 0,
) : VideoData(false, frameType, codecID.value.toInt(), body) {
    override val size = super.size + if (codecID == CodecID.AVC) 4 else 0

    override fun encodeHeaderImpl(output: Sink) {
        if (codecID == CodecID.AVC) {
            output.writeByte(packetType!!.value) // AVC Packet Type
            output.writeInt24(compositionTime) // Composition Time
        }
    }

    override fun toString(): String {
        return "LegacyVideoData(frameType=$frameType, codecID=$codecID, packetType=$packetType, compositionTime=$compositionTime, body=$body)"
    }

    companion object {
        /**
         * Decodes a legacy video data from the given [source] and [sourceSize].
         *
         * @param source the source to read from
         * @param sourceSize the size of the source
         */
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): LegacyVideoData {
            val firstByte = source.readByte()
            val isExHeader = (firstByte.toInt() and 0x80) != 0
            require(!isExHeader) {
                "Extended header is not supported for legacy video data."
            }
            val frameType = VideoFrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
            val codecID = CodecID.entryOf(firstByte and 0x0F)
            return decode(frameType, codecID, source, sourceSize - 1, isEncrypted)
        }

        internal fun decode(
            frameType: VideoFrameType,
            codecID: CodecID,
            source: Source,
            sourceSize: Int,
            isEncrypted: Boolean
        ): LegacyVideoData {
            return if (codecID == CodecID.AVC) {
                val packetType = AVCPacketType.entryOf(source.readByte())
                val compositionTime = source.readInt24()
                val remainingSize = sourceSize - 4
                require(!isEncrypted) { "Encrypted video is not supported." }
                val body = if (frameType == VideoFrameType.COMMAND) {
                    CommandLegacyVideoTagBody.decode(source, remainingSize)
                } else {
                    RawVideoTagBody.decode(source, remainingSize)
                }
                LegacyVideoData(
                    frameType, codecID, body, packetType, compositionTime
                )
            } else {
                require(!isEncrypted) { "Encrypted video is not supported." }
                val body = if (frameType == VideoFrameType.COMMAND) {
                    CommandLegacyVideoTagBody.decode(source, sourceSize)
                } else {
                    RawVideoTagBody.decode(source, sourceSize)
                }
                LegacyVideoData(
                    frameType, codecID, body
                )
            }
        }
    }
}

/**
 * Creates a [ExtendedVideoData] from a [body] for a single track.
 *
 * When using [VideoPacketType.CODED_FRAMES] and [VideoFourCC.AVC] or [VideoFourCC.HEVC], the body must be a [CompositionTimeExtendedVideoTagBody]
 *
 * @param frameType the frame type (key frame or intra frame)
 * @param packetType the packet type (excluding [VideoPacketType.MULTITRACK] nad [VideoPacketType.MOD_EX])
 * @param fourCC the FourCCs
 * @param body the coded frame [RawSource]
 * @return the [ExtendedVideoData] with the coded frame
 */
fun ExtendedVideoData(
    frameType: VideoFrameType,
    packetType: VideoPacketType,
    fourCC: VideoFourCC,
    body: SingleVideoTagBody
) = ExtendedVideoData(
    packetDescriptor = ExtendedVideoData.SingleVideoPacketDescriptor(
        frameType,
        packetType,
        fourCC,
        body
    )
)

/**
 * Representation of extended video data in enhanced FLV format (v1 and v2).
 *
 * @param packetDescriptor the packet descriptor
 * @param videoModExs the set of video mod ex
 */
class ExtendedVideoData internal constructor(
    val packetDescriptor: VideoPacketDescriptor,
    val videoModExs: Set<ModEx<VideoPacketModExType>> = emptySet()
) : VideoData(
    true, packetDescriptor.frameType, if (videoModExs.isEmpty()) {
        packetDescriptor.packetType.value
    } else {
        VideoPacketType.MOD_EX.value
    }.toInt(), packetDescriptor.body
) {
    override val size = super.size + packetDescriptor.size
    val packetType = packetDescriptor.packetType

    override fun encodeHeaderImpl(output: Sink) {
        videoModExs.forEachIndexed { index, modEx ->
            val nextPacketType = if (index == videoModExs.size - 1) {
                packetDescriptor.packetType
            } else {
                VideoPacketType.MOD_EX
            }
            modEx.encode(output, nextPacketType)
        }
        if ((packetType != VideoPacketType.META_DATA) && (frameType == VideoFrameType.COMMAND)) {
            require(packetDescriptor is CommandVideoPacketDescriptor) {
                "Invalid frame type for command: $frameType. Only CommandHeaderExtension is supported."
            }
        } else if (packetType == VideoPacketType.MULTITRACK) {
            require(packetDescriptor is MultitrackVideoPacketDescriptor) {
                "Invalid frame type for multitrack: $frameType. Only MultitrackHeaderExtension is supported."
            }
        }
        packetDescriptor.encode(output)
    }

    override fun toString(): String {
        return "ExtendedVideoData(frameType=$frameType, packetType=${packetType}, packetDescriptor=$packetDescriptor, videoModExs=$videoModExs)"
    }

    companion object {
        /**
         * Decodes a extended video data from the given [source] and [sourceSize].
         *
         * @param source the source to read from
         * @param sourceSize the size of the source
         */
        fun decode(source: Source, sourceSize: Int): ExtendedVideoData {
            val firstByte = source.readByte()
            val isExHeader = (firstByte.toInt() and 0x80) != 0
            require(isExHeader) {
                "Legacy header is not supported for extended video data."
            }
            val frameType = VideoFrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
            val packetType = VideoPacketType.entryOf(firstByte and 0x0F)
            return decode(frameType, packetType, source, sourceSize - 1)
        }

        internal fun decode(
            frameType: VideoFrameType,
            packetType: VideoPacketType,
            source: Source,
            sourceSize: Int
        ): ExtendedVideoData {
            var remainingSize = sourceSize
            val videoModExs = mutableSetOf<ModEx<VideoPacketModExType>>()
            while (packetType == VideoPacketType.MOD_EX) {
                val videoModEx = ModEx.decode<VideoPacketModExType>(source)
                videoModExs.add(videoModEx)
                remainingSize -= videoModEx.size
            }
            val packetDescriptor =
                if ((packetType != VideoPacketType.META_DATA) && (frameType == VideoFrameType.COMMAND)) {
                    CommandVideoPacketDescriptor.decode(source)
                } else if (packetType == VideoPacketType.MULTITRACK) {
                    MultitrackVideoPacketDescriptor.decode(
                        frameType,
                        source,
                        remainingSize
                    )
                } else {
                    SingleVideoPacketDescriptor.decode(frameType, packetType, source, remainingSize)
                }
            return ExtendedVideoData(packetDescriptor, videoModExs)
        }
    }

    interface VideoPacketDescriptor : SinkEncoder {
        val frameType: VideoFrameType
        val packetType: VideoPacketType
        val body: VideoTagBody
    }

    class SingleVideoPacketDescriptor internal constructor(
        override val frameType: VideoFrameType,
        override val packetType: VideoPacketType,
        val fourCC: VideoFourCC,
        override val body: SingleVideoTagBody
    ) : VideoPacketDescriptor {
        override val size = 4

        init {
            require(packetType != VideoPacketType.MULTITRACK) {
                "Invalid packet type for single video: $packetType. Use MultitrackVideoPacketDescriptor instead."
            }
            require(frameType != VideoFrameType.COMMAND) {
                "Invalid frame type for single video: $frameType. Use CommandVideoPacketDescriptor instead."
            }
            require(packetType != VideoPacketType.MOD_EX) {
                "Invalid packet type for single video: $packetType. MOD_EX is not a valid packet type."
            }
        }

        override fun encode(output: Sink) {
            output.writeInt(fourCC.value.code)
        }

        override fun toString(): String {
            return "SingleVideoPacketDescriptor(frameType=$frameType, fourCC=$fourCC, body=$body)"
        }

        companion object {
            fun decode(
                frameType: VideoFrameType,
                packetType: VideoPacketType,
                source: Source,
                sourceSize: Int
            ): SingleVideoPacketDescriptor {
                val fourCC = VideoFourCC.codeOf(source.readInt())
                val remainingSize = sourceSize - 4
                val body = decodeBody(packetType, fourCC, source, remainingSize)
                return SingleVideoPacketDescriptor(frameType, packetType, fourCC, body)
            }

            internal fun decodeBody(
                packetType: VideoPacketType,
                fourCC: VideoFourCC,
                source: Source,
                sourceSize: Int
            ): SingleVideoTagBody {
                return if (sourceSize == 0) {
                    EmptyVideoTagBody()
                } else if ((packetType == VideoPacketType.CODED_FRAMES) && ((fourCC == VideoFourCC.HEVC) ||
                            (fourCC == VideoFourCC.AVC))
                ) {
                    CompositionTimeExtendedVideoTagBody.decode(source, sourceSize)
                } else {
                    RawVideoTagBody.decode(source, sourceSize)
                }
            }
        }
    }

    class CommandVideoPacketDescriptor internal constructor(
        override val packetType: VideoPacketType,
        val command: VideoCommand,
    ) : VideoPacketDescriptor {
        override val frameType = VideoFrameType.COMMAND
        override val body = EmptyVideoTagBody() as VideoTagBody

        override val size = 1

        override fun encode(output: Sink) {
            output.writeByte(command.value)
        }

        override fun toString(): String {
            return "CommandVideoPacketDescriptor(command=$command)"
        }

        companion object {
            fun decode(source: Source): CommandVideoPacketDescriptor {
                val command = VideoCommand.entryOf(source.readByte())
                return CommandVideoPacketDescriptor(VideoPacketType.MOD_EX, command)
            }
        }
    }

    /**
     * The multitrack extended video packet descriptor.
     */
    sealed class MultitrackVideoPacketDescriptor(
        override val frameType: VideoFrameType,
        val multitrackType: MultitrackType,
        val framePacketType: VideoPacketType
    ) :
        VideoPacketDescriptor {
        override val packetType = VideoPacketType.MULTITRACK
        override val size = 1

        init {
            require(framePacketType != VideoPacketType.MULTITRACK) {
                "Invalid packet type for multitrack: $framePacketType."
            }
        }

        abstract fun encodeImpl(output: Sink)

        override fun encode(output: Sink) {
            output.writeByte((multitrackType.value shl 4) or framePacketType.value.toInt())
            encodeImpl(output)
        }

        private interface OneCodec : SinkEncoder {
            val fourCC: VideoFourCC
        }

        companion object {
            fun decode(
                frameType: VideoFrameType,
                source: Source,
                sourceSize: Int
            ): MultitrackVideoPacketDescriptor {
                val byte = source.readByte()
                val multitrackType =
                    MultitrackType.entryOf(((byte and 0xF0.toByte()) shr 4).toByte())
                val framePacketType = VideoPacketType.entryOf(byte and 0x0F.toByte())
                val remainingSize = sourceSize - 1
                return when (multitrackType) {
                    MultitrackType.ONE_TRACK -> OneTrackVideoPacketDescriptor.decode(
                        frameType,
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK -> ManyTrackVideoPacketDescriptor.decode(
                        frameType,
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK_MANY_CODEC -> ManyTrackManyCodecVideoPacketDescriptor.decode(
                        frameType,
                        framePacketType,
                        source,
                        remainingSize
                    )
                }
            }
        }

        class OneTrackVideoPacketDescriptor internal constructor(
            override val frameType: VideoFrameType,
            framePacketType: VideoPacketType,
            override val fourCC: VideoFourCC,
            override val body: OneTrackVideoTagBody
        ) : MultitrackVideoPacketDescriptor(frameType, MultitrackType.ONE_TRACK, framePacketType),
            OneCodec {
            override val size = super.size + 4

            override fun encodeImpl(output: Sink) {
                output.writeInt(fourCC.value.code)
            }

            override fun toString(): String {
                return "OneTrackVideoPacketDescriptor(frameType=$frameType, packetType=$framePacketType fourCC=$fourCC, body=$body)"
            }

            companion object {
                fun decode(
                    frameType: VideoFrameType,
                    packetType: VideoPacketType,
                    source: Source,
                    sourceSize: Int
                ): OneTrackVideoPacketDescriptor {
                    val fourCC = VideoFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        OneTrackVideoTagBody.decode(packetType, fourCC, source, remainingSize)
                    return OneTrackVideoPacketDescriptor(frameType, packetType, fourCC, body)
                }
            }
        }

        class ManyTrackVideoPacketDescriptor internal constructor(
            override val frameType: VideoFrameType,
            framePacketType: VideoPacketType,
            override val fourCC: VideoFourCC,
            override val body: ManyTrackOneCodecVideoTagBody
        ) : MultitrackVideoPacketDescriptor(frameType, MultitrackType.MANY_TRACK, framePacketType),
            OneCodec {
            override val size = super.size + 4

            override fun encodeImpl(output: Sink) {
                output.writeInt(fourCC.value.code)
            }

            override fun toString(): String {
                return "ManyTrackVideoPacketDescriptor(frameType=$frameType, packetType=$framePacketType, fourCC=$fourCC, body=$body)"
            }

            companion object {
                fun decode(
                    frameType: VideoFrameType,
                    packetType: VideoPacketType,
                    source: Source,
                    sourceSize: Int
                ): ManyTrackVideoPacketDescriptor {
                    val fourCC = VideoFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        ManyTrackOneCodecVideoTagBody.decode(
                            packetType,
                            fourCC,
                            source,
                            remainingSize
                        )
                    return ManyTrackVideoPacketDescriptor(frameType, packetType, fourCC, body)
                }
            }
        }

        class ManyTrackManyCodecVideoPacketDescriptor internal constructor(
            override val frameType: VideoFrameType,
            framePacketType: VideoPacketType,
            override val body: ManyTrackManyCodecVideoTagBody
        ) : MultitrackVideoPacketDescriptor(
            frameType,
            MultitrackType.MANY_TRACK_MANY_CODEC,
            framePacketType
        ) {
            override val size = super.size

            override fun encodeImpl(output: Sink) = Unit

            override fun toString(): String {
                return "ManyTrackManyCodecVideoPacketDescriptor(frameType=$frameType, packetType=$framePacketType, body=$body)"
            }

            companion object {
                fun decode(
                    frameType: VideoFrameType,
                    packetType: VideoPacketType,
                    source: Source,
                    sourceSize: Int
                ): ManyTrackManyCodecVideoPacketDescriptor {
                    val body = ManyTrackManyCodecVideoTagBody.decode(packetType, source, sourceSize)
                    return ManyTrackManyCodecVideoPacketDescriptor(
                        frameType,
                        packetType,
                        body
                    )
                }
            }
        }
    }
}

sealed class VideoData(
    val isExtended: Boolean,
    val frameType: VideoFrameType,
    val next4bitsValue: Int,
    val body: VideoTagBody
) : FLVData {
    open val size = body.size + 1
    override fun getSize(amfVersion: AmfVersion) = size

    abstract fun encodeHeaderImpl(
        output: Sink
    )

    private fun encodeBody(output: Sink, amfVersion: AmfVersion) {
        body.encode(output, amfVersion)
    }

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((isExtended shl 7) or // IsExHeader
                    (frameType.value shl 4) or // Frame Type
                    next4bitsValue).toByte()
        )
        encodeHeaderImpl(output)
        encodeBody(output, amfVersion)
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): VideoData {
            val firstByte = source.readByte()
            val isExHeader = (firstByte.toInt() and 0x80) != 0
            val frameType = VideoFrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
            return if (isExHeader) {
                val packetType = VideoPacketType.entryOf(firstByte and 0x0F)
                ExtendedVideoData.decode(
                    frameType,
                    packetType,
                    source,
                    sourceSize - 1
                )
            } else {
                val codecID = CodecID.entryOf(firstByte and 0x0F)
                LegacyVideoData.decode(
                    frameType,
                    codecID,
                    source,
                    sourceSize - 1,
                    isEncrypted
                )
            }
        }
    }
}

/**
 * The FLV Frame type.
 *
 * @param value the value
 */
enum class VideoFrameType(val value: Byte) {
    KEY(1), INTER(2), DISPOSABLE_INTER(3), GENERATED_KEY(4), COMMAND(5);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid FrameType value: $value"
            )
    }
}

/**
 * The AVC Packet type.
 *
 * @param value the value
 */
enum class AVCPacketType(val value: Byte) {
    SEQUENCE_HEADER(0), NALU(1), END_OF_SEQUENCE(2);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AVCPacketType value: $value"
            )
    }
}

enum class VideoCommand(val value: Byte) {
    START_SEEK(0), STOP_SEEK(1);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid VideoCommand value: $value"
            )
    }
}

/**
 * The FLV Packet type.
 *
 * @param value the value
 * @param avcPacketType the corresponding AVC packet type
 * @see AVCPacketType
 */
enum class VideoPacketType(
    val value: Byte, val avcPacketType: AVCPacketType? = null
) {
    SEQUENCE_START(0, AVCPacketType.SEQUENCE_HEADER), // Sequence Start
    CODED_FRAMES(1, AVCPacketType.NALU),
    SEQUENCE_END(
        2, AVCPacketType.END_OF_SEQUENCE
    ),

    /**
     * Composition time is implicitly set to 0.
     */
    CODED_FRAMES_X(3, null),
    META_DATA(
        4, null
    ),

    /**
     * Carriage of bitstream in MPEG-2 TS format
     */
    MPEG2_TS_SEQUENCE_START(
        5, null
    ),

    /**
     * Turns on multitrack mode
     */
    MULTITRACK(
        6, null
    ),

    /**
     * A special signal within that serves to both modify and extend the behavior of the current packet
     */
    MOD_EX(
        7, null
    );

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid VideoPacketType value: $value"
            )
    }
}

enum class VideoPacketModExType(override val value: Byte) : WithValue<Byte> {
    TIMESTAMP_OFFSET_NANO(0);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid VideoPacketModExType value: $value"
            )
    }
}

abstract class VideoModExFactory<T>(type: VideoPacketModExType) :
    ModExFactory<VideoPacketModExType, T>(type)

object VideoModExFactories {
    val TIMESTAMP_OFFSET_NANO =
        object : VideoModExFactory<Int>(VideoPacketModExType.TIMESTAMP_OFFSET_NANO) {
            override fun create(value: Int): ModEx<VideoPacketModExType> {
                return ModEx(type, 3, { sink ->
                    sink.writeInt24(value)
                })
            }
        }
}
