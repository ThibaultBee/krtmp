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
package io.github.thibaultbee.krtmp.amf.elements.primitives

import io.github.thibaultbee.krtmp.amf.elements.AMF0_TRUE
import io.github.thibaultbee.krtmp.amf.elements.Amf0Type
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import kotlinx.io.Sink
import kotlinx.io.Source

fun Amf0Boolean(source: Source): AmfBoolean {
    val type = source.readByte()
    require(type == Amf0Type.BOOLEAN.value) { "Amf0Boolean cannot read buffer because it's not BOOLEAN type" }
    return AmfBoolean(source.readByte() == AMF0_TRUE)
}

class AmfBoolean(override val value: Boolean) : AmfPrimitive<Boolean>() {
    override val size0 = 2

    override val size3: Int
        get() {
            TODO("Not yet implemented")
        }

    override fun write0(sink: Sink) {
        sink.writeByte(Amf0Type.BOOLEAN.value)
        sink.writeByte(if (value) 0x01 else 0x0)
    }

    override fun write3(sink: Sink) {
        TODO("Not yet implemented")
    }
}