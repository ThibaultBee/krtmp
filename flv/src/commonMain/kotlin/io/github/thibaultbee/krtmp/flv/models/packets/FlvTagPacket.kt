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
package io.github.thibaultbee.krtmp.flv.models.packets

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.extensions.shl
import io.github.thibaultbee.krtmp.flv.models.tags.AudioTag
import io.github.thibaultbee.krtmp.flv.models.tags.FlvTag
import io.github.thibaultbee.krtmp.flv.models.tags.ScriptTag
import io.github.thibaultbee.krtmp.flv.models.tags.VideoTag
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * An [Packet] that wrapped a [FlvTag].
 */
class FlvTagPacket
internal constructor(
    val tag: FlvTag
) : Packet(tag.timestampMs) {
    val isEncrypted: Boolean = false

    private val bodySize = tag.bodySize
    private val tagSize = bodySize + FLV_HEADER_TAG_SIZE

    override fun writeToSink(output: Sink) {
        val header = Header(tag.timestampMs, tag.type, bodySize, isEncrypted)
        header.writeToSink(output)
        tag.writeBodyToSink(output, isEncrypted)
        output.writeInt(tagSize)
    }

    /**
     * Gets the body of the [FlvTag] wrapped in an [Packet].
     */
    val bodyOutputPacket: Packet
        get() {
            return object : Packet(tag.timestampMs) {
                override fun writeToSink(output: Sink) {
                    tag.writeBodyToSink(output, isEncrypted)
                }

                override fun readRawSource(): RawSource {
                    return tag.readRawSource(isEncrypted)
                }
            }
        }

    companion object {
        private const val FLV_HEADER_TAG_SIZE = 11

        fun read(source: Source): FlvTag {
            val header = Header.read(source)
            val tag = when (header.type) {
                FlvTag.Type.AUDIO -> AudioTag.read(source, header)
                FlvTag.Type.VIDEO -> VideoTag.read(source, header)
                FlvTag.Type.SCRIPT -> ScriptTag.read(source, header, AmfVersion.AMF0)
            }
            return tag
        }
    }

    class Header(
        val timestampMs: Int,
        val type: FlvTag.Type,
        val bodySize: Int,
        val isEncrypted: Boolean,
        val streamId: Int = 0
    ) {
        fun writeToSink(output: Sink) {
            output.writeByte(((isEncrypted shl 5) or (type.value)).toByte())
            output.writeInt24(bodySize)
            output.writeInt24(timestampMs)
            output.writeByte((timestampMs shr 24).toByte())
            output.writeInt24(streamId) // Stream ID
        }

        companion object {
            fun read(source: Source): Header {
                val flags = source.readByte().toInt()
                val isEncrypted = (flags and 0x20) != 0
                val type = FlvTag.Type.entryOf(flags and 0x1F)
                val bodySize = source.readInt24()
                val timestamp = source.readInt24() or (source.readByte().toInt() shl 24)
                val streamId = source.readInt24() // Stream ID
                return Header(timestamp, type, bodySize, isEncrypted, streamId)
            }
        }
    }
}
