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
import io.github.thibaultbee.krtmp.amf.AmfDecodingException
import io.github.thibaultbee.krtmp.amf.UnknownKeyException
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfStrictArray
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfBoolean
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNull
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfNumber
import io.github.thibaultbee.krtmp.amf.elements.primitives.AmfString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.internal.NamedValueDecoder

private const val PRIMITIVE_TAG = "primitive" // also used in JsonPrimitiveInput

internal fun <T> readAmf(
    amf: Amf,
    element: AmfElement,
    deserializer: DeserializationStrategy<T>
): T {
    val input = when (element) {
        is AmfObject -> AmfObjectDecoder(amf, element)
        is AmfStrictArray -> AmfListDecoder(amf, element)
        is AmfEcmaArray -> AmfMapDecoder(amf, element)
        is AmfPrimitive<*>, AmfNull -> AmfPrimitiveDecoder(amf, element as AmfPrimitive<*>)
        else -> throw AmfDecodingException("Unsupported AMF element type: $element")
    }
    return input.decodeSerializableValue(deserializer)
}

// Writes class as map [fieldName, fieldValue]
@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
private sealed class AbstractAmfDecoder(protected val amf: Amf, open val value: AmfElement) :
    NamedValueDecoder() {
    protected val configuration = amf.configuration
    protected fun currentObject() = currentTagOrNull?.let { currentElement(it) } ?: value

    fun decodedAmfElement(): AmfElement = currentObject()

    override fun composeName(parentName: String, childName: String): String = childName

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val currentObject = currentObject()
        return when (descriptor.kind) {
            StructureKind.LIST, is PolymorphicKind -> AmfListDecoder(
                amf,
                cast(currentObject, descriptor)
            )

            StructureKind.MAP -> AmfMapDecoder(amf, cast(currentObject, descriptor))
            else -> AmfObjectDecoder(amf, cast(currentObject, descriptor))
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    override fun decodeNotNullMark(): Boolean = currentObject() !is AmfNull

    protected fun getPrimitiveValue(tag: String): AmfPrimitive<*> {
        val currentElement = currentElement(tag)
        return currentElement as? AmfPrimitive<*> ?: throw AmfDecodingException(
            "Expected JsonPrimitive at $tag, found $currentElement"
        )
    }

    protected abstract fun currentElement(tag: String): AmfElement
    override fun decodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor): Int =
        throw UnsupportedOperationException("Enums are not supported in AMF")

    override fun decodeTaggedNull(tag: String): Nothing? = null

    override fun decodeTaggedNotNullMark(tag: String): Boolean = currentElement(tag) !== AmfNull

    override fun decodeTaggedBoolean(tag: String): Boolean {
        return (getPrimitiveValue(tag) as AmfBoolean).value
    }

    override fun decodeTaggedByte(tag: String) =
        throw UnsupportedOperationException("Byte is not supported in AMF")

    override fun decodeTaggedShort(tag: String) =
        throw UnsupportedOperationException("Short is not supported in AMF")

    override fun decodeTaggedInt(tag: String): Int {
        return (getPrimitiveValue(tag) as AmfNumber).value.toInt()
    }

    override fun decodeTaggedLong(tag: String) =
        throw UnsupportedOperationException("Long is not supported in AMF")

    override fun decodeTaggedFloat(tag: String) =
        throw UnsupportedOperationException("Float is not supported in AMF")

    override fun decodeTaggedDouble(tag: String): Double {
        return (getPrimitiveValue(tag) as AmfNumber).value
    }

    override fun decodeTaggedChar(tag: String) =
        throw UnsupportedOperationException("Float is not supported in AMF")

    override fun decodeTaggedString(tag: String): String {
        val value = getPrimitiveValue(tag)
        return (value as? AmfString)?.value ?: throw AmfDecodingException(
            "Expected JsonPrimitive at $tag, found $value"
        )
    }

    override fun decodeTaggedInline(tag: String, inlineDescriptor: SerialDescriptor) =
        throw UnsupportedOperationException("Inline classes are not supported in AMF")
}

private class AmfPrimitiveDecoder(amf: Amf, override val value: AmfPrimitive<*>) :
    AbstractAmfDecoder(amf, value) {

    init {
        pushTag(PRIMITIVE_TAG)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun currentElement(tag: String): AmfElement {
        require(tag === PRIMITIVE_TAG) { "This input can only handle primitives with '$PRIMITIVE_TAG' tag" }
        return value
    }
}


private open class AmfObjectDecoder(
    amf: Amf,
    override val value: AmfObject,
    private val polyDiscriminator: String? = null,
    private val polyDescriptor: SerialDescriptor? = null
) : AbstractAmfDecoder(amf, value) {
    private var position = 0
    private var forceNull: Boolean = false

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("INVISIBLE_MEMBER")
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < descriptor.elementsCount) {
            val name = descriptor.getTag(position++)
            val index = position - 1
            forceNull = false
            if ((name in value || absenceIsNull(descriptor, index))) {
                return index
            }
        }
        return CompositeDecoder.DECODE_DONE
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun absenceIsNull(descriptor: SerialDescriptor, index: Int): Boolean {
        forceNull =
            !descriptor.isElementOptional(index) && descriptor.getElementDescriptor(index).isNullable
        return forceNull
    }

    override fun decodeNotNullMark(): Boolean {
        return !forceNull && super.decodeNotNullMark()
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        val baseName = descriptor.getElementName(index)
        // Slow path
      /*  val deserializationNamesMap = amf.deserializationNamesMap(descriptor)
        value.keys.find { deserializationNamesMap[it] == index }?.let {
            return it
        }

        val fallbackName = strategy?.serialNameForJson(
            descriptor,
            index,
            baseName
        ) // Key not found exception should be thrown with transformed name, not original
        return fallbackName ?:*/ return baseName
    }

    override fun currentElement(tag: String): AmfElement = value.getValue(tag)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        // polyDiscriminator needs to be preserved so the check for unknown keys
        // in endStructure can filter polyDiscriminator out.
        if (descriptor === polyDescriptor) {
            return AmfObjectDecoder(
                amf, cast(currentObject(), polyDescriptor), polyDiscriminator, polyDescriptor
            )
        }

        return super.beginStructure(descriptor)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun endStructure(descriptor: SerialDescriptor) {
        if (configuration.ignoreUnknownKeys || descriptor.kind is PolymorphicKind) return
        // Validate keys
        @Suppress("DEPRECATION_ERROR")
        val names: Set<String> = descriptor.cachedSerialNames()

        for (key in value.keys) {
            if (key !in names && key != polyDiscriminator) {
                throw UnknownKeyException(key, value.toString())
            }
        }
    }
}

private class AmfMapDecoder(amf: Amf, override val value: AmfEcmaArray) :
    AbstractAmfDecoder(amf, value) {
    private val keys = value.keys.toList()
    private val size: Int = keys.size * 2
    private var position = -1

    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        val i = index / 2
        return keys[i]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (position < size - 1) {
            position++
            return position
        }
        return CompositeDecoder.DECODE_DONE
    }

    override fun currentElement(tag: String): AmfElement {
        return if (position % 2 == 0) AmfString(tag) else value.getValue(tag)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // do nothing, maps do not have strict keys, so strict mode check is omitted
    }
}

private class AmfListDecoder(amf: Amf, override val value: AmfStrictArray) :
    AbstractAmfDecoder(amf, value) {
    private val size = value.size
    private var currentIndex = -1

    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun currentElement(tag: String): AmfElement {
        return value[tag.toInt()]
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (currentIndex < size - 1) {
            currentIndex++
            return currentIndex
        }
        return CompositeDecoder.DECODE_DONE
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified T : AmfElement> cast(
    value: AmfElement,
    descriptor: SerialDescriptor
): T {
    if (value !is T) {
        throw AmfDecodingException(
            "Expected ${T::class} as the serialized body of ${descriptor.serialName}, but had ${value::class}"
        )
    }
    return value
}


@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.cachedSerialNames(): Set<String> {
    val result = HashSet<String>(elementsCount)
    for (i in 0 until elementsCount) {
        result += getElementName(i)
    }
    return result
}
