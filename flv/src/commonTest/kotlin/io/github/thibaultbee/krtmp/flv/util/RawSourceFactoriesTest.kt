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
package io.github.thibaultbee.krtmp.flv.util

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class RawSourceFactoriesTest {
    @Test
    fun `build nalu raw source from array in avcc format`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)
        val sizedRawSource = transformToAVCC(array)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from array in annex b`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x00, 0x00)
        val sizedRawSource = transformToAVCC(array)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from array without prefix`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val array = byteArrayOf(0x00, 0x00)
        val sizedRawSource = transformToAVCC(array)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer in avcc format`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeInt(2)
            writeShort(0)
        }
        val sizedRawSource = transformToAVCC(buffer)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer in annex b`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeInt(1)
            writeShort(0)
        }
        val sizedRawSource = transformToAVCC(buffer)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }

    @Test
    fun `build nalu raw source from buffer without prefix`() {
        val expected = byteArrayOf(0x00, 0x00, 0x00, 0x02, 0x00, 0x00)

        val buffer = Buffer().apply {
            writeShort(0)
        }
        val sizedRawSource = transformToAVCC(buffer)
        assertEquals(6, sizedRawSource.second)

        val actual = Buffer().apply { sizedRawSource.first.readAtMostTo(this, 6) }.readByteArray()
        assertContentEquals(expected, actual)
    }
}