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
package io.github.thibaultbee.krtmp.amf

import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader
import io.github.thibaultbee.krtmp.amf.elements.Amf3ElementReader
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.internal.AmfWriter
import io.github.thibaultbee.krtmp.amf.internal.readAmf
import kotlinx.io.Buffer
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * A [BinaryFormat] implementation that uses Action Message Format as its serialization format.
 */
sealed class Amf(
    val configuration: AmfConfiguration,
    override val serializersModule: SerializersModule
) : BinaryFormat {
    /**
     * The [AmfElement] reader
     */
    internal val reader =
        if (configuration.version == AmfVersion.AMF0) Amf0ElementReader else Amf3ElementReader

    /**
     * The default instance of [Amf]
     */
    companion object Default : Amf(AmfConfiguration(), EmptySerializersModule())

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val buffer = encodeToBuffer(serializer, value)
        val bytes = ByteArray(buffer.size.toInt())
        buffer.readAtMostTo(bytes, 0, bytes.size)
        return bytes
    }

    fun <T> encodeToBuffer(serializer: SerializationStrategy<T>, value: T): Buffer {
        val amfElements = encodeToAmfElement(serializer, value)
        return amfElements.write(configuration.version)
    }

    fun <T> encodeToAmfElement(serializer: SerializationStrategy<T>, value: T): AmfElement {
        lateinit var result: AmfElement
        val dumper = AmfWriter(this) { result = it }
        dumper.encodeSerializableValue(serializer, value)
        return result
    }


    override fun <T> decodeFromByteArray(
        deserializer: DeserializationStrategy<T>,
        bytes: ByteArray
    ): T {
        val buffer = Buffer()
        buffer.write(bytes, 0, bytes.size)
        return decodeFromBuffer(deserializer, buffer)
    }

    fun <T> decodeFromBuffer(
        deserializer: DeserializationStrategy<T>,
        buffer: Buffer
    ): T {
        val element = reader.read(buffer)
        return decodeFromAmfElement(deserializer, element)
    }

    fun <T> decodeFromAmfElement(
        deserializer: DeserializationStrategy<T>,
        element: AmfElement
    ): T {
        return readAmf(this, element, deserializer)
    }
}

private class AmfImpl(
    configuration: AmfConfiguration,
    serializersModule: SerializersModule
) :
    Amf(configuration, serializersModule)

/**
 * Creates an instance of [Amf] configured from the optionally given [Amf instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
fun Amf(from: Amf = Amf, builderAction: AmfBuilder.() -> Unit): Amf {
    val builder = AmfBuilder(from)
    builder.builderAction()
    val configuration = builder.build()
    return AmfImpl(
        configuration,
        builder.serializersModule
    )
}

/**
 * Builder of the [Amf] instance provided by `Amf` factory function.
 */
@ExperimentalSerializationApi
class AmfBuilder internal constructor(amf: Amf) {
    /**
     * Specifies the AMF version. Either 0 or 3.
     */
    var version: AmfVersion = amf.configuration.version

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    var encodeDefaults: Boolean = amf.configuration.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input AMF
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = amf.configuration.ignoreUnknownKeys

    /**
     * Specifies whether nulls should be encoded as explicit AMF nulls.
     *
     * When this flag is disabled properties with `null` values without default are not encoded.
     */
    var explicitNulls: Boolean = amf.configuration.explicitNulls

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Amf] instance.
     */
    var serializersModule: SerializersModule = amf.serializersModule

    fun build(): AmfConfiguration = AmfConfiguration(
        version,
        encodeDefaults,
        ignoreUnknownKeys,
        explicitNulls
    )
}

data class AmfConfiguration(
    /**
     * Specifies the AMF version. Either 0 or 3.
     */
    val version: AmfVersion = AmfVersion.AMF0,

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    val encodeDefaults: Boolean = false,

    /**
     * Specifies whether encounters of unknown properties in the input AMF
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    val ignoreUnknownKeys: Boolean = false,

    /**
     * Specifies whether nulls should be encoded as explicit AMF nulls.
     *
     * When this flag is disabled properties with `null` values without default are not encoded.
     */
    val explicitNulls: Boolean = true
)