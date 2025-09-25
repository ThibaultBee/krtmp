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


class FLVHeaderTest {
    @Test
    fun `test audio video flv header`() {
        val expected = byteArrayOf(
            0x46,
            0x4C,
            0x56,
            0x01,
            0x05,
            0x00,
            0x00,
            0x00,
            0x09
        ) // FLV header
        val flvHeader = FLVHeader(hasAudio = true, hasVideo = true)
        val buffer = Buffer()
        flvHeader.encode(buffer)
        assertContentEquals(expected, buffer.readByteArray())
    }
}