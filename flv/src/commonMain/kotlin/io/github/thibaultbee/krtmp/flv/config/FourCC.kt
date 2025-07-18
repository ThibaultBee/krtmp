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
package io.github.thibaultbee.krtmp.flv.config

/**
 * FourCC is a 4 bytes code used to identify a codec.
 */
open class FourCC internal constructor(
    val a: Char,
    val b: Char,
    val c: Char,
    val d: Char
) {
    /**
     * FourCC code
     */
    val code = (a.code shl 24) or (b.code shl 16) or (c.code shl 8) or d.code

    override fun toString(): String {
        return "$a$b$c$d"
    }
}