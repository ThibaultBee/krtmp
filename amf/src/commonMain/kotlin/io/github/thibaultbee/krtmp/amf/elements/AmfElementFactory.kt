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

import io.github.thibaultbee.krtmp.amf.elements.containers.AmfContainer
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfStrictArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfBoolean
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfDate
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNull
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import kotlinx.datetime.Instant
import kotlinx.io.IOException

object AmfElementFactory {
    fun buildContainer(value: List<Any?>) = AmfContainer(value)

    fun buildEcmaArray(value: Map<String, Any?>) = AmfEcmaArray(value)

    fun buildObject(value: Map<String, Any?>) = AmfObject(value)

    fun buildStrictArray(value: List<Any?>) = AmfStrictArray(value)

    fun build(value: Any?): AmfElement {
        if (value == null) {
            return AmfNull()
        }
        return when (value) {
            is AmfElement -> value
            is Boolean -> AmfBoolean(value)
            is Double -> AmfNumber(value)
            is String -> AmfString(value)
            is List<*> -> AmfStrictArray(value)
            is Instant -> AmfDate(value)
            is Map<*, *> -> {
                if (value.keys.any { it !is String }) {
                    throw IOException("AMF ECMA array keys must be String. At least one is not a String in ${value.keys}")
                }
                @Suppress("UNCHECKED_CAST")
                AmfEcmaArray(value as Map<String, Any?>)
            }
            else -> throw IOException("Can't build an AmfParameter for ${value::class.simpleName}: $value")
        }
    }
}