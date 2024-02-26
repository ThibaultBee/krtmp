package io.github.thibaultbee.krtmp.amf

import kotlinx.serialization.decodeFromHexString
import kotlin.test.Test
import kotlin.test.assertEquals

class Amf0DecoderTest {
    @Test
    fun `read boolean`() {
        val obj = Amf.decodeFromHexString(
            DataBoolean.serializer(),
            "03000576616c75650101000009"
        )

        assertEquals(
            true,
            obj.value
        )
    }

    @Test
    fun `read number`() {
        val obj = Amf.decodeFromHexString(
            DataNumber.serializer(),
            "03000576616c7565004045000000000000000009"
        )

        assertEquals(
            42.0,
            obj.value
        )
    }


    @Test
    fun `read string`() {
        val obj = Amf.decodeFromHexString(
            DataString.serializer(),
            "03000576616c756502000d737472696e67546f5772697465000009"
        )

        assertEquals(
            "stringToWrite",
            obj.value
        )
    }

    @Test
    fun `read strict array`() {
        val obj = Amf.decodeFromHexString(
            DataStrictArray.serializer(),
            "03000576616c75650a0000000102000576616c7565000009"
        )

        assertEquals(1, obj.value.size)
        assertEquals(
            "value",
            obj.value[0]
        )
    }

    @Test
    fun `read ecma array`() {
        val obj = Amf.decodeFromHexString(
            DataEcmaArray.serializer(),
            "03000576616c7565080000000100036b657902000576616c7565000009000009"
        )

        assertEquals(
            1,
            obj.value.size
        )
        assertEquals(
            "value",
            obj.value["key"]
        )
    }
}