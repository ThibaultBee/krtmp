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
package io.github.thibaultbee.krtmp.flv.util.extensions

/**
 * Converts a Boolean to a Byte.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
internal fun Boolean.toByte(): Byte = if (this) 1 else 0

/**
 * Converts a Boolean to an Int.
 *
 * @return 1 if Boolean is True, 0 otherwise
 */
internal fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * Shifts left the value by the number of bits specified in the second operand for Boolean.
 */
internal infix fun Boolean.shl(i: Int) = this.toInt() shl i

internal infix fun Byte.shl(i: Byte) = this.toInt() shl i.toInt()

internal infix fun Byte.shl(i: Int) = this.toInt() shl i

internal infix fun Byte.shr(i: Int) = this.toInt() shr i
