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
package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.common.FourCCs
import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.extensions.shl
import io.github.thibaultbee.krtmp.flv.extensions.shr
import io.github.thibaultbee.krtmp.flv.models.av.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import io.github.thibaultbee.krtmp.flv.models.sources.MultiRawSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.experimental.and

fun ExtendedVideoTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    mimeType: MimeType,
    frameType: FrameType,
    packetType: PacketType,
): ExtendedVideoTag {
    require(ExtendedVideoTag.isSupportedCodec(mimeType)) {
        "Unsupported codec $mimeType. Only ${
            ExtendedVideoTag.supportedCodecs.joinToString(
                ", "
            )
        } are supported"
    }

    return ExtendedVideoTag(
        timestampMs,
        source,
        sourceSize,
        ExtendedVideoTag.ExtendedTagHeader(frameType, packetType, mimeType)
    )
}

/**
 * @param packetType 0: AVC sequence header, 1: AVC NALU, 2: AVC end of sequence. Using `PacketType` instead of `AVCPacketType` to simplify as the first 3 values are the same.
 */
fun LegacyVideoTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    mimeType: MimeType,
    frameType: FrameType,
    packetType: AVCPacketType?,
): LegacyVideoTag {
    require(LegacyVideoTag.isSupportedCodec(mimeType)) { "Unsupported codec $mimeType" }

    return LegacyVideoTag(
        timestampMs,
        source,
        sourceSize,
        LegacyVideoTag.LegacyTagHeader(frameType, CodecID.mimetypeOf(mimeType), packetType)
    )
}

sealed class VideoTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    open val tagHeader: TagHeader,
) :
    AVFlvTag(timestampMs, Type.VIDEO, source, sourceSize) {
    companion object {
        fun read(source: Source, header: FlvTagPacket.Header): VideoTag {
            val tagHeader = TagHeader.read(source)
            val remainingSize = header.bodySize - tagHeader.size
            return when (tagHeader) {
                is LegacyVideoTag.LegacyTagHeader -> {
                    LegacyVideoTag(
                        header.timestampMs,
                        source,
                        remainingSize,
                        tagHeader
                    )
                }

                is ExtendedVideoTag.ExtendedTagHeader -> {
                    throw NotImplementedError("ExtendedTagHeader is not implemented")
                }
            }
        }
    }

    sealed class TagHeader(
        val isExHeader: Boolean,
        val frameType: FrameType
    ) {
        abstract val size: Int
        abstract fun writeToSink(output: Sink)

        companion object {
            fun read(source: Source): TagHeader {
                val firstByte = source.readByte()
                val isExHeader = (firstByte.toInt() and 0x80) != 0
                val frameType = FrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
                return if (isExHeader) {
                    val packetType = PacketType.entryOf(firstByte.toInt() and 0x0F)
                    val fourCC = FourCCs.codeOf(source.readInt())
                    ExtendedVideoTag.ExtendedTagHeader(frameType, packetType, fourCC.value.mimeType)
                } else {
                    val codecID = CodecID.entryOf(firstByte and 0x0F)
                    if (codecID == CodecID.AVC) {
                        val packetType = AVCPacketType.entryOf(source.readByte())
                        source.readInt24() // CompositionTime
                        LegacyVideoTag.LegacyTagHeader(frameType, codecID, packetType)
                    } else {
                        LegacyVideoTag.LegacyTagHeader(frameType, codecID, null)
                    }
                }
            }

        }
    }
}


class LegacyVideoTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    override val tagHeader: LegacyTagHeader
) : VideoTag(
    timestampMs,
    source,
    sourceSize,
    tagHeader
) {
    private val tagBodySize = computeBodySize()

    override val bodySize = tagHeader.size + tagBodySize

    init {
        if (tagHeader.codecID == CodecID.AVC) {
            requireNotNull(tagHeader.packetType) { "AVC packet type is required for H264" }
        }
    }

    override fun writeTagHeader(output: Sink) {
        tagHeader.writeToSink(output)
    }

    override fun writeTagBody(output: Sink) {
        when (tagHeader.packetType) {
            AVCPacketType.END_OF_SEQUENCE -> {
                // signals end of sequence
            }

            else -> output.transferFrom(source)
        }
    }

    private fun computeBodySize(): Int {
        return when (tagHeader.packetType) {
            AVCPacketType.END_OF_SEQUENCE -> {
                0
            }

            else -> {
                sourceSize
            }
        }
    }

    override fun readRawSource(isEncrypted: Boolean): RawSource {
        val sources = mutableListOf<RawSource>(Buffer().apply { writeTagHeader(this) })
        when (tagHeader.packetType) {
            AVCPacketType.END_OF_SEQUENCE -> {
                // signals end of sequence
            }

            else -> sources.add(source)
        }
        return MultiRawSource(sources)
    }

    companion object {
        private val supportedCodecs =
            listOf(MimeType.VIDEO_AVC)

        fun isSupportedCodec(mimeType: MimeType) =
            supportedCodecs.contains(mimeType)
    }


    class LegacyTagHeader(
        frameType: FrameType,
        val codecID: CodecID,
        val packetType: AVCPacketType?,
    ) : TagHeader(false, frameType) {
        override val size = if (codecID == CodecID.AVC) 5 else 1

        override fun writeToSink(output: Sink) {
            output.writeByte(
                ((frameType.value shl 4) or // Frame Type
                        (codecID.value.toInt())).toByte() // CodecID
            )
            if (codecID == CodecID.AVC) {
                output.writeByte(packetType!!.value) // AVC sequence header or NALU
                output.writeInt24(0) // TODO: CompositionTime
            }
        }
    }
}

class ExtendedVideoTag(
    timestampMs: Int,
    source: RawSource,
    sourceSize: Int,
    override val tagHeader: ExtendedTagHeader,
) : VideoTag(timestampMs, source, sourceSize, tagHeader) {
    private val tagBodySize = computeBodySize()

    override val bodySize = tagHeader.size + tagBodySize

    override fun writeTagHeader(output: Sink) {
        tagHeader.writeToSink(output)
    }

    override fun writeTagBody(output: Sink) {
        when (tagHeader.packetType) {
            PacketType.META_DATA -> {
                throw NotImplementedError("PacketType ${tagHeader.packetType} is not implemented for ${tagHeader.mimeType}")
            }

            PacketType.SEQUENCE_END -> {
                // signals end of sequence
            }

            else -> {
                if ((tagHeader.packetType == PacketType.CODED_FRAMES) && (tagHeader.mimeType == MimeType.VIDEO_HEVC)) {
                    output.writeInt24(0) // TODO: CompositionTime
                }
                output.transferFrom(source)
            }
        }
    }

    private fun computeBodySize(): Int {
        return when (tagHeader.packetType) {
            PacketType.META_DATA -> {
                throw NotImplementedError("PacketType ${tagHeader.packetType} is not implemented for ${tagHeader.mimeType}")
            }

            PacketType.SEQUENCE_END -> {
                0
            }

            else -> {
                val size =
                    if ((tagHeader.packetType == PacketType.CODED_FRAMES) && (tagHeader.mimeType == MimeType.VIDEO_HEVC)) {
                        3 // TODO: CompositionTime
                    } else {
                        0
                    }
                return sourceSize + size
            }
        }
    }

    override fun readRawSource(isEncrypted: Boolean): RawSource {
        val sources = mutableListOf<RawSource>(Buffer().apply { writeTagHeader(this) })
        when (tagHeader.packetType) {
            PacketType.META_DATA -> {
                throw NotImplementedError("PacketType ${tagHeader.packetType} is not implemented for ${tagHeader.mimeType}")
            }

            PacketType.SEQUENCE_END -> {
                // signals end of sequence
            }

            else -> {
                if ((tagHeader.packetType == PacketType.CODED_FRAMES) && (tagHeader.mimeType == MimeType.VIDEO_HEVC)) {
                    sources.add(Buffer().apply { writeInt24(0) }) // TODO: CompositionTime
                }
                sources.add(source)
            }
        }
        return MultiRawSource(sources)
    }

    companion object {
        val supportedCodecs =
            listOf(MimeType.VIDEO_AV1, MimeType.VIDEO_VP9, MimeType.VIDEO_HEVC)

        fun isSupportedCodec(mimeType: MimeType) = supportedCodecs.contains(mimeType)
    }

    class ExtendedTagHeader(
        frameType: FrameType,
        val packetType: PacketType,
        val mimeType: MimeType
    ) : TagHeader(true, frameType) {
        override val size = VIDEO_TAG_HEADER_SIZE

        override fun writeToSink(output: Sink) {
            output.writeByte(
                (0x80 or // IsExHeader
                        (frameType.value shl 4) or // Frame Type
                        packetType.value).toByte() // PacketType
            )
            output.writeInt(FourCCs.mimeTypeOf(mimeType).value.code) // Video FourCC
        }

        companion object {
            private const val VIDEO_TAG_HEADER_SIZE = 5
        }
    }
}

enum class FrameType(val value: Byte) {
    KEY(1),
    INTER(2),
    DISPOSABLE_INTER(3),
    GENERATED_KEY(4),
    INFO_COMMAND(5);

    companion object {
        fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}

enum class AVCPacketType(val value: Byte) {
    SEQUENCE_HEADER(0),
    NALU(1),
    END_OF_SEQUENCE(2);

    companion object {
        fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}

enum class PacketType(val value: Int, val avcPacketType: AVCPacketType? = null) {
    SEQUENCE_START(0, AVCPacketType.SEQUENCE_HEADER), // Sequence Start
    CODED_FRAMES(1, AVCPacketType.NALU),
    SEQUENCE_END(2, AVCPacketType.END_OF_SEQUENCE),
    CODED_FRAMES_X(3, AVCPacketType.NALU),
    META_DATA(4, null),
    MPEG2_TS_SEQUENCE_START(5, null);

    companion object {
        fun entryOf(value: Int) = entries.first { it.value == value }
    }
}


