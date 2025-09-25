/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.sources

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals

class LimitedRawSourceTest {
    @Test
    fun `test readAtMostTo with size is equal to buffer size`() {
        val expected = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        val buffer = Buffer().apply {
            write(expected)
        }
        val sizedRawSource = LimitedRawSource(buffer, 4)
        val actual = sizedRawSource.buffered().readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `test readAtMostTo with size is less buffer size`() {
        val expected = byteArrayOf(0x01, 0x02)
        val buffer = Buffer().apply {
            write(expected + byteArrayOf(0x03, 0x04))
        }
        val sizedRawSource = LimitedRawSource(buffer, 2)
        val actual = sizedRawSource.buffered().readByteArray()
        assertContentEquals(expected, actual)
    }

}