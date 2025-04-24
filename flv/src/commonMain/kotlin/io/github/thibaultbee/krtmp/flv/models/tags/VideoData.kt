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

class LegacyVideoData(
    frameType: FrameType,
    val codecID: CodecID,
    body: IVideoTagBody,
    val packetType: AVCPacketType? = null,
    private val compositionTime: Int = 0,
) : VideoData(frameType, body) {
    private val size = body.size + if (codecID == CodecID.AVC) 5 else 1

    override fun getSize(amfVersion: AmfVersion) = size

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((frameType.value shl 4) or // Frame Type
                    (codecID.value.toInt())).toByte() // CodecID
        )
        if (codecID == CodecID.AVC) {
            output.writeByte(packetType!!.value) // AVC Packet Type
            output.writeInt24(compositionTime) // Composition Time
        }
        body.encode(output)
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): VideoData {
            val firstByte = source.readByte()
            val frameType = FrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
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
                    frameType,
                    codecID,
                    body
                )
            }
        }
    }
}

class ExtendedVideoData(
    frameType: FrameType,
    private val packetType: PacketType,
    private val fourCC: FourCCs,
    body: IVideoTagBody
) : VideoData(frameType, body) {
    private val size = body.size + 5
    
    override fun getSize(amfVersion: AmfVersion) = size

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((frameType.value shl 4) or // Frame Type
                    (packetType.value and 0x0F)).toByte() // Packet Type
        )
        output.writeInt24(fourCC.value.code)
    }
}

sealed class VideoData(
    val frameType: FrameType,
    val body: IVideoTagBody
) :
    FLVData {

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): VideoData {
            val firstByte = source.readByte()
            val isExHeader = (firstByte.toInt() and 0x80) != 0
            val frameType = FrameType.entryOf(((firstByte shr 4) and 0x07).toByte())
            return if (isExHeader) {
                val packetType = PacketType.entryOf(firstByte.toInt() and 0x0F)
                val fourCC = FourCCs.codeOf(source.readInt())
                val remainingSize = sourceSize - 5
                val body = if (fourCC == FourCCs.HEVC) {
                    HEVCVideoTagBody.decode(source, remainingSize, packetType)
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
                        frameType,
                        codecID,
                        body
                    )
                }
            }
        }
    }
}

/**
 * The AVC Packet type.
 *
 * @param value the value
 */
enum class AVCPacketType(val value: Byte) {
    SEQUENCE_HEADER(0),
    NALU(1),
    END_OF_SEQUENCE(2);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AVCPacketType value: $value"
            )
    }
}

/**
 * The FLV Frame type.
 *
 * @param value the value
 */
enum class FrameType(val value: Byte) {
    KEY(1),
    INTER(2),
    DISPOSABLE_INTER(3),
    GENERATED_KEY(4),
    INFO_COMMAND(5);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid FrameType value: $value"
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
enum class PacketType(
    val value: Int,
    val avcPacketType: AVCPacketType
) {
    SEQUENCE_START(0, AVCPacketType.SEQUENCE_HEADER), // Sequence Start
    CODED_FRAMES(1, AVCPacketType.NALU),
    SEQUENCE_END(2, AVCPacketType.END_OF_SEQUENCE),
    CODED_FRAMES_X(3, AVCPacketType.NALU),
    META_DATA(4, throw NotImplementedError("MetaData is not implemented")),
    MPEG2_TS_SEQUENCE_START(
        5,
        throw NotImplementedError("MPEG2_TS_SEQUENCE_START is not implemented")
    ), ;

    companion object {
        fun entryOf(value: Int) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid PacketType value: $value"
            )
    }
}

