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


internal const val AMF0_FALSE = 0x00.toByte()
internal const val AMF0_TRUE = 0x01.toByte()

internal enum class Amf0Type(val value: Byte) {
    NUMBER(0x00),
    BOOLEAN(0x01),
    STRING(0x02),
    OBJECT(0x03),
    NULL(0x05),
    ECMA_ARRAY(0x08),
    OBJECT_END(0x09),
    STRICT_ARRAY(0x0A),
    DATE(0x0B),
    LONG_STRING(0x0C);

    companion object {
        internal fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}

internal enum class Amf3Type(val value: Byte) {
    UNDEFINED(0x00),
    NULL(0x01),
    FALSE(0x02),
    TRUE(0x03),
    INTEGER(0x04),
    DOUBLE(0x05),
    STRING(0x06),
    XML_DOC(0x07),
    DATE(0x08),
    ARRAY(0x09),
    OBJECT(0x0A),
    XML(0x0B),
    BYTE_ARRAY(0x0C),
    VECTOR_INT(0x0D),
    VECTOR_UINT(0x0E),
    VECTOR_DOUBLE(0x0F),
    VECTOR_OBJECT(0x10),
    DICTIONARY(0x11);

    companion object {
        internal fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}