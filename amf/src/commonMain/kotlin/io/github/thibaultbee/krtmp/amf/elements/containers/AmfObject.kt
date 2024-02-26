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
package io.github.thibaultbee.krtmp.amf.elements.containers

import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader.read
import io.github.thibaultbee.krtmp.amf.elements.Amf0Type
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.extensions.putAll
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString

fun amf0ObjectFrom(source: Source): AmfObject {
    val type = source.readByte()
    require(type == Amf0Type.OBJECT.value) { "Amf0Object cannot read buffer because it's not OBJECT type" }

    val amf0Object = AmfObject()
    while (source.peek().readInt24() != Amf0Type.OBJECT_END.value.toInt()) {
        val keyLength = source.readShort()
        val key = source.readString(keyLength.toLong())
        val value = read(source)
        amf0Object[key] = value
    }
    source.readInt24() // Skip OBJECT_END
    return amf0Object
}

fun amfObjectOf(initialElements: Map<String, Any?> = emptyMap()) =
    AmfObject().apply { putAll(initialElements) }

class AmfObject internal constructor(private val elements: MutableMap<String, AmfElement> = mutableMapOf()) :
    AmfElement(), MutableMap<String, AmfElement> by elements {
    override val size0: Int
        get() {
            return 4 /* 1 byte for type + 3 bytes for footer */ + elements.keys.sumOf { 2 + it.length } + elements.values.sumOf {
                it.size0
            }
        }

    override val size3: Int
        get() {
            TODO("Not yet implemented")
        }

    override fun write0(sink: Sink) {
        sink.writeByte(Amf0Type.OBJECT.value)
        elements.forEach { entry ->
            sink.writeShort(entry.key.length.toShort())
            sink.writeString(entry.key)
            entry.value.write0(sink)
        }
        sink.writeInt24(Amf0Type.OBJECT_END.value.toInt())
    }

    override fun write3(sink: Sink) {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean = elements == other
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String {
        return elements.entries.joinToString(
            separator = ", ",
            prefix = "{",
            postfix = "}",
            transform = { (k, v) ->
                buildString {
                    append(k)
                    append('=')
                    append(v)
                }
            }
        )
    }
}