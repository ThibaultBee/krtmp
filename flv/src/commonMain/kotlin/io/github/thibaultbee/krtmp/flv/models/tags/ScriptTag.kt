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
package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.Amf0Container
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfContainer
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Script tag
 *
 * @param name method or object name
 * @param value AMF arguments or object properties.
 */
open class ScriptTag(
    timestampMs: Int,
    val name: String,
    val value: AmfEcmaArray
) :
    FlvTag(timestampMs, Type.SCRIPT) {

    /**
     * AMF version to use when writing metadata
     */
    var amfVersion = AmfVersion.AMF0

    private val container = AmfContainer(listOf(name, value))

    override fun writeBodyToSink(output: Sink, isEncrypted: Boolean) {
        container.write(amfVersion, output)
    }

    override val bodySize = container.getSize(amfVersion)

    companion object {
        fun read(
            source: Source,
            header: FlvTagPacket.Header,
            amfVersion: AmfVersion
        ): ScriptTag {
            val container = if (amfVersion == AmfVersion.AMF0) {
                Amf0Container(2, source)
            } else {
                throw NotImplementedError("AMF3 not implemented")
            }
            val name = container[0] as AmfString
            val value = container[1] as AmfEcmaArray

            return when (name.value) {
                ON_METADATA -> OnMetadata(header.timestampMs, value)
                else -> ScriptTag(header.timestampMs, name.value, value)
            }
        }

        internal const val ON_METADATA = "onMetaData"
    }
}