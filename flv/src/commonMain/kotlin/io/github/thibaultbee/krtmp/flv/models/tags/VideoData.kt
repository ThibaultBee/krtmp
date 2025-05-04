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

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.models.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.config.FourCCs
import io.github.thibaultbee.krtmp.flv.models.util.extensions.shl
import io.github.thibaultbee.krtmp.flv.models.util.extensions.shr
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.experimental.and

class LegacyVideoData internal constructor(
    frameType: VideoFrameType,
    val codecID: CodecID,
    body: IVideoTagBody,
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
}

class ExtendedVideoData internal constructor(
    frameType: VideoFrameType,
    val packetType: VideoPacketType,
    val fourCC: FourCCs,
    body: IVideoTagBody
) : VideoData(true, frameType, packetType.value, body) {
    init {
        if (packetType == VideoPacketType.CODED_FRAMES_X) {
            require(fourCC == FourCCs.AVC || fourCC == FourCCs.HEVC) {
                "Invalid fourCC for coded frames: $fourCC. Only AVC and HEVC are supported."
            }
        }
        if ((packetType == VideoPacketType.CODED_FRAMES) &&
            ((fourCC == FourCCs.AVC) || (fourCC != FourCCs.HEVC))
        ) {
            require(body is AVCHEVCCodedFrameVideoTagBody) {
                "Invalid body for coded frames: $body. Only AVCHEVCCodedFrameVideoTagBody is supported."
            }
        }
    }

    override val size = super.size + 4

    override fun encodeHeaderImpl(output: Sink) {
        output.writeInt24(fourCC.value.code)
    }
}

sealed class VideoData(
    val isExtended: Boolean,
    val frameType: VideoFrameType,
    val next4bitsValue: Int,
    val body: IVideoTagBody
) : FLVData {
    open val size = body.size + 1
    override fun getSize(amfVersion: AmfVersion) = size

    abstract fun encodeHeaderImpl(
        output: Sink
    )

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((isExtended shl 7) or // IsExHeader
                    (frameType.value shl 4) or // Frame Type
                    next4bitsValue).toByte() // PacketType
        )
        encodeHeaderImpl(output)
        body.encode(output)
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): VideoData {
            val firstByte = source.readByte()
            val isExHeader = (firstByte.toInt() and 0x80) != 0
            val frameType = VideoFrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
            return if (isExHeader) {
                val packetType = VideoPacketType.entryOf(firstByte.toInt() and 0x0F)
                val fourCC = FourCCs.codeOf(source.readInt())
                val remainingSize = sourceSize - 5
                val body = if (packetType == VideoPacketType.CODED_FRAMES) {
                    AVCHEVCCodedFrameVideoTagBody.decode(source, remainingSize)
                } else {
                    DefaultVideoTagBody.decode(source, remainingSize)
                }
                ExtendedVideoData(frameType, packetType, fourCC, body)
            } else {
                val codecID = CodecID.entryOf(firstByte and 0x0F)
                return if (codecID == CodecID.AVC) {
                    val packetType = AVCPacketType.entryOf(source.readByte())
                    val compositionTime = source.readInt24()
                    val remainingSize = sourceSize - 5
                    require(!isEncrypted) { "Encrypted video is not supported." }
                    val body = DefaultVideoTagBody.decode(source, remainingSize)
                    LegacyVideoData(frameType, codecID, body, packetType, compositionTime)
                } else {
                    val remainingSize = sourceSize - 1
                    require(!isEncrypted) { "Encrypted video is not supported." }
                    val body = DefaultVideoTagBody.decode(source, remainingSize)
                    LegacyVideoData(
                        frameType, codecID, body
                    )
                }
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
    KEY(1),
    INTER(2),
    DISPOSABLE_INTER(3),
    GENERATED_KEY(4),
    COMMAND(5);

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

/**
 * The FLV Packet type.
 *
 * @param value the value
 * @param avcPacketType the corresponding AVC packet type
 * @see AVCPacketType
 */
enum class VideoPacketType(
    val value: Int, val avcPacketType: AVCPacketType
) {
    SEQUENCE_START(0, AVCPacketType.SEQUENCE_HEADER), // Sequence Start
    CODED_FRAMES(1, AVCPacketType.NALU), SEQUENCE_END(
        2,
        AVCPacketType.END_OF_SEQUENCE
    ),
    CODED_FRAMES_X(3, AVCPacketType.NALU), META_DATA(
        4,
        throw NotImplementedError("MetaData is not implemented")
    ),
    MPEG2_TS_SEQUENCE_START(
        5, throw NotImplementedError("MPEG2_TS_SEQUENCE_START is not implemented")
    ), ;

    companion object {
        fun entryOf(value: Int) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid PacketType value: $value"
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