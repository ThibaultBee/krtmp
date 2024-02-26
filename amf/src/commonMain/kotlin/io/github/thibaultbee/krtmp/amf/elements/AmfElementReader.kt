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
package io.github.thibaultbee.krtmp.amf.elements

import io.github.thibaultbee.krtmp.amf.elements.containers.amf0ContainerFrom
import io.github.thibaultbee.krtmp.amf.elements.containers.amf0EcmaArrayFrom
import io.github.thibaultbee.krtmp.amf.elements.containers.amf0ObjectFrom
import io.github.thibaultbee.krtmp.amf.elements.containers.amf0StrictArrayFrom
import io.github.thibaultbee.krtmp.amf.elements.primitives.amf0BooleanFrom
import io.github.thibaultbee.krtmp.amf.elements.primitives.amf0DateFrom
import io.github.thibaultbee.krtmp.amf.elements.primitives.amf0NullFrom
import io.github.thibaultbee.krtmp.amf.elements.primitives.amf0NumberFrom
import io.github.thibaultbee.krtmp.amf.elements.primitives.amf0StringFrom
import kotlinx.io.IOException
import kotlinx.io.Source

interface AmfElementReader {
    fun readContainer(numOfElements: Int, source: Source): AmfElement

    fun readEcmaArray(source: Source): AmfElement

    fun readObject(source: Source): AmfElement

    fun buildStrictArray(source: Source): AmfElement

    fun read(source: Source): AmfElement
}

object Amf0ElementReader : AmfElementReader {
    override fun readContainer(numOfElements: Int, source: Source) =
        amf0ContainerFrom(numOfElements, source)

    override fun readEcmaArray(source: Source) = amf0EcmaArrayFrom(source)

    override fun readObject(source: Source) = amf0ObjectFrom(source)

    override fun buildStrictArray(source: Source) = amf0StrictArrayFrom(source)

    override fun read(source: Source): AmfElement {
        return when (val type = source.peek().readByte()) {
            Amf0Type.NUMBER.value -> amf0NumberFrom(source)
            Amf0Type.BOOLEAN.value -> amf0BooleanFrom(source)
            Amf0Type.STRING.value -> amf0StringFrom(source)
            Amf0Type.LONG_STRING.value -> amf0StringFrom(source)
            Amf0Type.OBJECT.value -> amf0ObjectFrom(source)
            Amf0Type.NULL.value -> amf0NullFrom(source)
            Amf0Type.ECMA_ARRAY.value -> amf0EcmaArrayFrom(source)
            Amf0Type.STRICT_ARRAY.value -> amf0StrictArrayFrom(source)
            Amf0Type.DATE.value -> amf0DateFrom(source)
            else -> throw IOException("Invalid AMF0 type: $type")
        }
    }
}


object Amf3ElementReader : AmfElementReader {
    override fun readContainer(numOfElements: Int, source: Source) =
        TODO("Not yet implemented")

    override fun readEcmaArray(source: Source) = TODO("Not yet implemented")

    override fun readObject(source: Source) = TODO("Not yet implemented")

    override fun buildStrictArray(source: Source) = TODO("Not yet implemented")

    override fun read(source: Source) = TODO("Not yet implemented")
}