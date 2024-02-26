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

import io.github.thibaultbee.krtmp.amf.elements.containers.Amf0Container
import io.github.thibaultbee.krtmp.amf.elements.containers.Amf0EcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.Amf0Object
import io.github.thibaultbee.krtmp.amf.elements.containers.Amf0StrictArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.Amf0Boolean
import io.github.thibaultbee.krtmp.amf.elements.primitives.Amf0Date
import io.github.thibaultbee.krtmp.amf.elements.primitives.Amf0Null
import io.github.thibaultbee.krtmp.amf.elements.primitives.Amf0Number
import io.github.thibaultbee.krtmp.amf.elements.primitives.Amf0String
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
        Amf0Container(numOfElements, source)

    override fun readEcmaArray(source: Source) = Amf0EcmaArray(source)

    override fun readObject(source: Source) = Amf0Object(source)

    override fun buildStrictArray(source: Source) = Amf0StrictArray(source)

    override fun read(source: Source): AmfElement {
        return when (val type = source.peek().readByte()) {
            Amf0Type.NUMBER.value -> Amf0Number(source)
            Amf0Type.BOOLEAN.value -> Amf0Boolean(source)
            Amf0Type.STRING.value -> Amf0String(source)
            Amf0Type.LONG_STRING.value -> Amf0String(source)
            Amf0Type.OBJECT.value -> Amf0Object(source)
            Amf0Type.NULL.value -> Amf0Null(source)
            Amf0Type.ECMA_ARRAY.value -> Amf0EcmaArray(source)
            Amf0Type.STRICT_ARRAY.value -> Amf0StrictArray(source)
            Amf0Type.DATE.value -> Amf0Date(source)
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