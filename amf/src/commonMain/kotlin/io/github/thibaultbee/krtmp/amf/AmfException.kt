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

import kotlinx.serialization.SerializationException

internal open class AmfDecodingException(message: String) : SerializationException(message)

internal fun AmfDecodingException(expected: String, foundByte: Int) =
    AmfDecodingException("Expected $expected, but found ${printByte(foundByte)}")

internal fun printByte(b: Int): String {
    val hexCode = "0123456789ABCDEF"
    return buildString {
        append(hexCode[b shr 4 and 0xF])
        append(hexCode[b and 0xF])
    }
}

internal class UnknownKeyException(key: String, input: String) : AmfDecodingException("Encountered an unknown key: $key for input: $input")