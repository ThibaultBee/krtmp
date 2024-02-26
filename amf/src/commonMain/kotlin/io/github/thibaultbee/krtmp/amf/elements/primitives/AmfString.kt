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

import io.github.thibaultbee.krtmp.amf.elements.Amf0Type
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString

fun Amf0String(source: Source): AmfString {
    val type = source.readByte()
    require((type == Amf0Type.STRING.value) || (type == Amf0Type.LONG_STRING.value)) { "Amf0String cannot read buffer because it's not STRING type" }
    val length = if (type == Amf0Type.STRING.value) {
        source.readShort().toLong()
    } else {
        source.readInt().toLong()
    }
    return AmfString(source.readString(length))
}

class AmfString(override val value: String) : AmfPrimitive<String>() {
    override val size0 = if (value.length < 65536) {
        3 + value.length
    } else {
        5 + value.length
    }

    override val size3: Int
        get() {
            TODO("Not yet implemented")
        }

    override fun write0(sink: Sink) {
        if (value.length < 65536) {
            sink.writeByte(Amf0Type.STRING.value)
            sink.writeShort(value.length.toShort())
        } else {
            sink.writeByte(Amf0Type.LONG_STRING.value)
            sink.writeInt(value.length)
        }
        sink.writeString(value)
    }

    override fun write3(sink: Sink) {
        TODO("Not yet implemented")
    }
}