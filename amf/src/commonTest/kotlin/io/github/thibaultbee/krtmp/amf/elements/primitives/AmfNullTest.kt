package io.github.thibaultbee.krtmp.amf.elements.primitives

import kotlin.test.Test
import kotlin.test.assertEquals

class AmfNullTest {
    @Test
    fun `to string`() {
        assertEquals("null", AmfNull().toString())
    }
}