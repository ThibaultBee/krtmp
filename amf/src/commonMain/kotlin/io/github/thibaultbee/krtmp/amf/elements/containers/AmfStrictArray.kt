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
import io.github.thibaultbee.krtmp.amf.elements.extensions.addAll
import kotlinx.io.Sink
import kotlinx.io.Source

fun amf0StrictArrayFrom(source: Source): AmfStrictArray {
    val type = source.readByte()
    require(type == Amf0Type.STRICT_ARRAY.value) { "Amf0StrictArray cannot read buffer because it's not STRICT_ARRAY type" }

    val amf0StrictArray = AmfStrictArray()
    val size = source.readInt()
    for (i in 0 until size) {
        amf0StrictArray.add(read(source))
    }
    return amf0StrictArray
}

fun amfStrictArrayOf(initialElements: List<Any?>) =
    AmfStrictArray().apply { addAll(initialElements) }

class AmfStrictArray internal constructor(private val elements: MutableList<AmfElement> = mutableListOf()) :
    AmfElement(), MutableList<AmfElement> by elements {
    override val size0: Int
        get() = 5 + elements.sumOf { it.size0 }

    override val size3: Int
        get() {
            TODO("Not yet implemented")
        }

    override fun write0(sink: Sink) {
        sink.writeByte(Amf0Type.STRICT_ARRAY.value)
        sink.writeInt(elements.size)
        elements.forEach { it.write0(sink) }
    }

    override fun write3(sink: Sink) {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean = elements == other
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String =
        elements.joinToString(prefix = "[", postfix = "]", separator = ", ")
}