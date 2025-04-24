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
package io.github.thibaultbee.krtmp.amf.internal

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfElementFactory.build
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfStrictArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNull
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import io.github.thibaultbee.krtmp.amf.elements.primitives.amfDateOf
import io.github.thibaultbee.krtmp.amf.internal.amf0.isDate
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule


// ECMA array is a map [fieldName, fieldValue]
private class AmfMapWriter(amf: Amf, nodeConsumer: (AmfElement) -> Unit) :
    AmfWriter(amf, nodeConsumer) {
    private lateinit var tag: String
    private var isKey = true

    override fun putElement(key: String, element: AmfElement) {
        if (isKey) { // writing key
            tag = when (element) {
                is AmfString -> element.value
                else -> throw UnsupportedOperationException("ECMA Map key must be a string")
            }
            isKey = false
        } else {
            content[tag] = element
            isKey = true
        }
    }

    override fun getCurrent() = AmfEcmaArray(content)
}

// Strict array is a list of values
private open class AmfListWriter(
    amf: Amf, nodeConsumer: (AmfElement) -> Unit
) : AbstractAmfWriter(amf, nodeConsumer) {
    private val array: ArrayList<AmfElement> = arrayListOf()
    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun putElement(key: String, element: AmfElement) {
        val idx = key.toInt()
        array.add(idx, element)
    }

    override fun getCurrent(): AmfElement = AmfStrictArray(array)
}

internal open class AmfWriter(
    amf: Amf, nodeConsumer: (AmfElement) -> Unit
) : AbstractAmfWriter(amf, nodeConsumer) {

    protected val content: MutableMap<String, AmfElement> = linkedMapOf()

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null || amf.configuration.explicitNulls) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun putElement(key: String, element: AmfElement) {
        content[key] = element
    }

    override fun getCurrent(): AmfElement = AmfObject(content)
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
internal sealed class AbstractAmfWriter(
    protected val amf: Amf,
    protected val nodeConsumer: (AmfElement) -> Unit
) :
    NamedValueEncoder() {
    override val serializersModule: SerializersModule
        get() = amf.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        amf.configuration.encodeDefaults

    override fun composeName(parentName: String, childName: String): String = childName
    abstract fun putElement(key: String, element: AmfElement)
    abstract fun getCurrent(): AmfElement

    override fun encodeTaggedValue(tag: String, value: Any) {
        putElement(tag, build(value))
    }

    // has no tag when encoding a nullable element at root level
    override fun encodeNotNullMark() {}

    // has no tag when encoding a nullable element at root level
    override fun encodeNull() {
        val tag = currentTagOrNull ?: return nodeConsumer(AmfNull())
        encodeTaggedNull(tag)
    }

    override fun encodeTaggedNull(tag: String) = putElement(tag, AmfNull())

    fun encodeDate(value: Instant) = amfDateOf(value)

    fun encodeTaggedDate(tag: String, value: Instant) {
        putElement(tag, encodeDate(value))
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when {
            serializer.descriptor.isDate -> {
                if (currentTagOrNull != null) {
                    encodeTaggedDate(currentTag, value as Instant)
                } else {
                    nodeConsumer(encodeDate(value as Instant))
                }
            }

            else -> {
                super.encodeSerializableValue(serializer, value)
            }
        }

    }

    override fun encodeTaggedEnum(
        tag: String,
        enumDescriptor: SerialDescriptor,
        ordinal: Int
    ) = putElement(tag, AmfString(enumDescriptor.getElementName(ordinal)))

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        throw UnsupportedOperationException("encodeInline is not supported in AMF")
        // return if (currentTagOrNull != null) super.encodeInline(descriptor)
        // else JsonPrimitiveEncoder(json, nodeConsumer).encodeInline(descriptor)
    }


    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) nodeConsumer
            else { node -> putElement(currentTag, node) }

        val writer = when (descriptor.kind) {
            StructureKind.LIST -> AmfListWriter(amf, consumer)
            StructureKind.MAP -> AmfMapWriter(amf, consumer)
            else -> AmfWriter(amf, consumer)
        }

        return writer
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        nodeConsumer(getCurrent())
    }
}
