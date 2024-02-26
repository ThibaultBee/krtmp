package io.github.thibaultbee.krtmp.amf

import flex.messaging.io.SerializationContext
import flex.messaging.io.amf.Amf0Output
import flex.messaging.io.amf.TraitsInfo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class AmfCompatibilityTest {
    private val serializerContext = SerializationContext()

    interface Serializer {
        fun serialize(output: Amf0Output)
    }

    @Serializable
    data class SomeClass(val prop: Int = 0) : Serializer {
        override fun serialize(output: Amf0Output) {
            serialize(output, this)
        }

        companion object {
            fun serialize(output: Amf0Output, obj: SomeClass) {
                output.writeObjectTraits(TraitsInfo(null))
                output.writeObjectProperty("prop", obj.prop)
                output.writeObjectEnd()
            }
        }
    }

    @Serializable
    data class IntData(val intV: Int) : Serializer {
        override fun serialize(output: Amf0Output) {
            serialize(output, this)
        }

        companion object {
            fun serialize(output: Amf0Output, obj: IntData) {
                output.writeObjectTraits(TraitsInfo(null))
                output.writeObjectProperty("intV", obj.intV)
                output.writeObjectEnd()
            }
        }
    }

    @Serializable
    data class StringData(val data: String) : Serializer {
        override fun serialize(output: Amf0Output) {
            serialize(output, this)
        }

        companion object {
            fun serialize(output: Amf0Output, obj: StringData) {
                output.writeObjectTraits(TraitsInfo(null))
                output.writeObjectProperty("data", obj.data)
                output.writeObjectEnd()
            }
        }
    }

    @Serializable
    data class DoubleData(val field: Double) : Serializer {
        override fun serialize(output: Amf0Output) {
            serialize(output, this)
        }

        companion object {
            fun serialize(output: Amf0Output, obj: DoubleData) {
                output.writeObjectTraits(TraitsInfo(null))
                output.writeObjectProperty("field", obj.field)
                output.writeObjectEnd()
            }
        }
    }

    private inline fun <reified T> compare(obj: T, serializer: KSerializer<T>) {
        val outputStream = ByteArrayOutputStream()
        Amf0Output(serializerContext).apply {
            setOutputStream(outputStream)
            (obj as Serializer).serialize(this)
        }
        assertEquals(obj, Amf.decodeFromByteArray(serializer, outputStream.toByteArray()))
    }

    private fun compareDouble(value: Double) {
        val doubleWrapper = DoubleData(value)
        val outputStream = ByteArrayOutputStream()
        Amf0Output(serializerContext).apply {
            setOutputStream(outputStream)
            doubleWrapper.serialize(this)
        }
        assertEquals(
            doubleWrapper,
            Amf.decodeFromByteArray(DoubleData.serializer(), outputStream.toByteArray())
        )
    }

    private fun compareString(value: String) {
        val stringWrapper = StringData(value)
        val outputStream = ByteArrayOutputStream()
        Amf0Output(serializerContext).apply {
            setOutputStream(outputStream)
            stringWrapper.serialize(this)
        }
        assertEquals(
            stringWrapper,
            Amf.decodeFromByteArray(StringData.serializer(), outputStream.toByteArray())
        )
    }

    @Test
    fun basicClassFromAnotherLibrary() {
        compare(SomeClass(), SomeClass.serializer())
    }

    @Test
    fun testString() {
        compareString("testString")
    }

    @Test
    fun testDouble() {
        compareDouble(Double.NaN)
        compareDouble(Double.POSITIVE_INFINITY)
        compareDouble(Double.NEGATIVE_INFINITY)
        compareDouble(Double.MAX_VALUE)
        compareDouble(Double.MIN_VALUE)
        compareDouble(0.0)
        compareDouble(-0.0)
        compareDouble(-1.0)
        compareDouble(1.0)
        compareDouble(123.56)
        compareDouble(123.0)
        // minimal denormalized value in half-precision
        compareDouble(5.9604644775390625E-8)
        // maximal denormalized value in half-precision
        compareDouble(0.00006097555160522461)
    }
}