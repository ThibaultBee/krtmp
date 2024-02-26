package io.github.thibaultbee.krtmp.amf

import kotlinx.serialization.*
import kotlin.test.*

class Amf0EncoderTest {
    @Test
    fun `write boolean`() {
        val value = true
        val obj = DataBoolean(value)

        assertEquals(
            "03000576616c75650101000009",
            Amf.encodeToHexString(
                DataBoolean.serializer(), obj
            )
        )
    }

    @Test
    fun `write number`() {
        val value = 42.0
        val obj = DataNumber(value)

        assertEquals(
            "03000576616c7565004045000000000000000009",
            Amf.encodeToHexString(DataNumber.serializer(), obj)
        )
    }


    @Test
    fun `write string`() {
        val value = "stringToWrite"
        val obj = DataString(value)

        assertEquals(
            "03000576616c756502000d737472696e67546f5772697465000009",
            Amf.encodeToHexString(DataString.serializer(), obj)
        )
    }

    @Test
    fun `write strict array`() {
        val obj = DataStrictArray(listOf("value"))

        assertEquals(
            "03000576616c75650a0000000102000576616c7565000009",
            Amf.encodeToHexString(DataStrictArray.serializer(), obj)
        )
    }

    @Test
    fun `write ecma array`() {
        val obj = DataEcmaArray(mapOf("key" to "value"))

        assertEquals(
            "03000576616c7565080000000100036b657902000576616c7565000009000009",
            Amf.encodeToHexString(DataEcmaArray.serializer(), obj)
        )
    }
}