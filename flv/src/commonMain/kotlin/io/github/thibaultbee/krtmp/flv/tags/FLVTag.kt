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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.tags.FLVTag.Type
import io.github.thibaultbee.krtmp.flv.tags.audio.AudioData
import io.github.thibaultbee.krtmp.flv.tags.script.ScriptDataObject
import io.github.thibaultbee.krtmp.flv.tags.video.VideoData
import io.github.thibaultbee.krtmp.flv.util.extensions.readSource
import io.github.thibaultbee.krtmp.flv.util.extensions.shl
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Represents a FLV tag.
 *
 * @property timestampMs The timestamp of the tag in milliseconds.
 * @property data The data contained in the tag, which can be audio, video, or script data.
 * @property streamId The stream ID of the tag, default is 0.
 */
data class FLVTag(
    val timestampMs: Int,
    val data: FLVData,
    val streamId: Int = 0,
) {
    /**
     * Encodes the FLV tag to the given output stream.
     *
     * @param output The output stream to write the encoded tag to.
     * @param amfVersion The AMF version to use for encoding.
     */
    fun encode(output: Sink, amfVersion: AmfVersion) {
        val isEncrypted = false
        val bodySize = data.getSize(amfVersion)
        val type = when (data) {
            is AudioData -> Type.AUDIO
            is VideoData -> Type.VIDEO
            is ScriptDataObject -> if (amfVersion == AmfVersion.AMF0) Type.SCRIPT_AMF0 else Type.SCRIPT_AMF3
            else -> throw IllegalArgumentException("Unknown FLV data type: ${data::class.simpleName}")
        }

        output.writeByte(((isEncrypted shl 5) or (type.value)).toByte())
        output.writeInt24(bodySize)
        output.writeInt24(timestampMs)
        output.writeByte((timestampMs shr 24).toByte())
        output.writeInt24(streamId) // Stream ID
        data.encode(output, amfVersion, isEncrypted)
    }

    override fun toString(): String {
        return "FLVTag(timestampMs=$timestampMs, data=$data, streamId=$streamId)"
    }

    companion object {
        val HEADER_SIZE = 11 // 11 bytes for the tag header

        /**
         * Decodes a FLV tag from the given input stream.
         * It also decodes the body of the tag.
         *
         * @param source The input stream to read the FLV tag from.
         * @param amfVersion The AMF version to use for decoding.
         */
        fun decode(source: Source): FLVTag {
            val flags = source.readByte().toInt()
            val isEncrypted = (flags and 0x20) != 0
            val type = Type.entryOf(flags and 0x1F)
            val bodySize = source.readInt24()
            val timestampMs = source.readInt24() or (source.readByte().toInt() shl 24)
            val streamId = source.readInt24() // Stream ID

            val data = when (type) {
                Type.AUDIO -> AudioData.decode(source, bodySize, isEncrypted)
                Type.VIDEO -> VideoData.decode(source, bodySize, isEncrypted)
                Type.SCRIPT_AMF0 -> ScriptDataObject.decode(source, AmfVersion.AMF0)
                Type.SCRIPT_AMF3 -> ScriptDataObject.decode(source, AmfVersion.AMF3)
            }
            return FLVTag(timestampMs, data, streamId)
        }
    }

    enum class Type(val value: Int) {
        AUDIO(8),
        VIDEO(9),
        SCRIPT_AMF3(15),
        SCRIPT_AMF0(18);

        companion object {
            fun entryOf(value: Int) = entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown FLV tag type: $value")
        }
    }
}

/**
 * Reads the tag as a [Buffer].
 *
 * @param amfVersion the AMF version to use for encoding
 * @return the [Buffer] containing the tag
 */
fun FLVTag.readBuffer(amfVersion: AmfVersion = AmfVersion.AMF0): Buffer {
    return Buffer().apply { encode(this, amfVersion) }
}

/**
 * Reads the tag as a [ByteArray].
 *
 * @param amfVersion the AMF version to use for encoding
 * @return the [ByteArray] containing the tag
 */
fun FLVTag.readByteArray(amfVersion: AmfVersion = AmfVersion.AMF0): ByteArray {
    return readBuffer(amfVersion).readByteArray()
}

/**
 * Represents a raw FLV tag. Raw means that the body is not decoded.
 */
data class RawFLVTag internal constructor(
    val isEncrypted: Boolean,
    val type: Type,
    val bodySize: Int,
    val timestampMs: Int,
    val body: Source,
    val streamId: Int = 0
) {
    fun decodeTag(): FLVTag {
        val data = when (type) {
            Type.AUDIO -> AudioData.decode(body, bodySize, isEncrypted)
            Type.VIDEO -> VideoData.decode(body, bodySize, isEncrypted)
            Type.SCRIPT_AMF0 -> ScriptDataObject.decode(body, AmfVersion.AMF0)
            Type.SCRIPT_AMF3 -> ScriptDataObject.decode(body, AmfVersion.AMF3)
        }
        return FLVTag(timestampMs, data, streamId)
    }

    fun encode(output: Sink) {
        output.writeByte(((isEncrypted shl 5) or (type.value)).toByte())
        output.writeInt24(bodySize)
        output.writeInt24(timestampMs)
        output.writeByte((timestampMs shr 24).toByte())
        output.writeInt24(streamId) // Stream ID
        output.write(body, bodySize.toLong())
    }

    override fun toString(): String {
        return "RawFLVTag(type=$type, timestampMs=$timestampMs, streamId=$streamId, bodySize=$bodySize)"
    }

    companion object {
        /**
         * Decodes a RAW FLV tag from the given input stream.
         * It does not decode the body of the tag.
         *
         * @param source The input stream to read the FLV tag from.
         */
        fun decode(source: Source): RawFLVTag {
            val flags = source.readByte().toInt()
            val isEncrypted = (flags and 0x20) != 0
            val type = Type.entryOf(flags and 0x1F)
            val bodySize = source.readInt24()
            val timestampMs = source.readInt24() or (source.readByte().toInt() shl 24)
            val streamId = source.readInt24() // Stream ID
            val body = source.readSource(bodySize.toLong())

            return RawFLVTag(isEncrypted, type, bodySize, timestampMs, body, streamId)
        }
    }
}

/**
 * Peeks the [body] of the [RawFLVTag] without decoding it.
 *
 * Use it when you want to read multiple times the same tag.
 */
fun RawFLVTag.peek() = copy(body = body.peek())




