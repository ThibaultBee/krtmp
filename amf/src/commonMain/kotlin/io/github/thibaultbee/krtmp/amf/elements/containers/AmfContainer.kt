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

import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.extensions.addAll
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Reads a container from a buffer.
 *
 * @param expectedNumOfElements the number of elements in the container
 * @param source the source containing elements
 */
fun amf0ContainerFrom(expectedNumOfElements: Int, source: Source): AmfContainer {
    val amfContainer = AmfContainer()
    var numOfElements = 0
    while (numOfElements < expectedNumOfElements) {
        amfContainer.add(Amf0ElementReader.read(source))
        numOfElements++
    }
    return amfContainer
}

fun amfContainerOf(initialElements: List<Any?>) =
    AmfContainer().apply { addAll(initialElements) }

/**
 * A container is a list of [AmfElement]. Contrary to [AmfStrictArray], it doesn't have a size.
 */
class AmfContainer internal constructor(private val elements: MutableList<AmfElement> = mutableListOf()) :
    AmfElement(),
    MutableList<AmfElement> by elements {

    override val size0: Int
        get() = elements.sumOf { it.size0 }

    override val size3: Int
        get() = elements.sumOf { it.size3 }

    override fun write0(sink: Sink) {
        elements.forEach { it.write0(sink) }
    }

    override fun write3(sink: Sink) {
        elements.forEach { it.write3(sink) }
    }

    override fun equals(other: Any?): Boolean = elements == other
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String =
        elements.joinToString(prefix = "[", postfix = "]", separator = ", ")
}