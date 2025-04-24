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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.containers.amf0ContainerFrom
import io.github.thibaultbee.krtmp.amf.elements.containers.amfContainerOf
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Script tag
 *
 * @param name method or object name
 * @param value AMF arguments or object properties.
 */
open class ScriptDataObject(
    val name: String,
    val value: AmfEcmaArray
) : FLVData {
    private val container = amfContainerOf(listOf(name, value))

    override fun getSize(amfVersion: AmfVersion) =
        container.getSize(amfVersion)

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) =
        container.write(amfVersion, output)

    companion object {
        fun decode(
            source: Source,
            amfVersion: AmfVersion
        ): ScriptDataObject {
            val container = if (amfVersion == AmfVersion.AMF0) {
                amf0ContainerFrom(2, source)
            } else {
                throw NotImplementedError("AMF3 not implemented")
            }
            val name = container[0] as AmfString
            val value = container[1] as AmfEcmaArray

            return when (name.value) {
                ON_METADATA -> OnMetadata(value)
                else -> ScriptDataObject(name.value, value)
            }
        }

        internal const val ON_METADATA = "onMetaData"
    }
}