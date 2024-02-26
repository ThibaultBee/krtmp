/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.extensions


/**
 * Convert a Boolean to an Int.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
fun Boolean.toInt() = if (this) 1 else 0

/**
 * Shifts this [Boolean] value left by the [bitCount] number of bits.
 */
infix fun Boolean.shl(bitCount: Int) =
    this.toInt() shl bitCount

/**
 * Shifts this [Boolean] value right by the [bitCount] number of bits.
 */
infix fun Boolean.shr(bitCount: Int) =
    this.toInt() shr bitCount

/**
 * Shifts this [Byte] value left by the [bitCount] number of bits.
 */
infix fun Byte.shl(bitCount: Int) =
    this.toInt() shl bitCount

/**
 * Shifts this [Byte] value right by the [bitCount] number of bits.
 */
infix fun Byte.shr(bitCount: Int) =
    this.toInt() shr bitCount

/**
 * Performs a bitwise OR operation between the two byte values.
 */
infix fun Byte.or(b: Byte) =
    this.toInt() or b.toInt()

/**
 * Performs a bitwise OR operation between the a byte and an integer.
 */
infix fun Byte.or(other: Int) =
    this.toInt() or other