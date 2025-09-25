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
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MultiRawSourceTest {
    @Test
    fun `read from a single source`() {
        val source = Buffer().apply { writeInt(0x01020304) }
        val multiRawSource = MultiRawSource(source)

        val sink = Buffer()
        multiRawSource.readAtMostTo(sink, 4)

        assertEquals(4, sink.size)
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), sink.readByteArray())
    }

    @Test
    fun `read from a multiple source at once`() {
        val source1 = Buffer().apply { writeInt(0x01020304) }
        val source2 = Buffer().apply { writeInt(0x05060708) }
        val multiRawSource = MultiRawSource(listOf(source1, source2))

        val sink = Buffer()
        multiRawSource.readAtMostTo(sink, 8)

        assertEquals(8, sink.size)
        assertContentEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            sink.readByteArray()
        )
    }

    @Test
    fun `read from a multiple source at once above source size`() {
        val source1 = Buffer().apply { writeInt(0x01020304) }
        val source2 = Buffer().apply { writeInt(0x05060708) }
        val multiRawSource = MultiRawSource(listOf(source1, source2))

        val sink = Buffer()
        multiRawSource.readAtMostTo(sink, 10)

        assertEquals(8, sink.size)
        assertContentEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08),
            sink.readByteArray()
        )
    }

    @Test
    fun `read from a multiple source multiple times at first source size`() {
        val source1 = Buffer().apply { writeInt(0x01020304) }
        val source2 = Buffer().apply { writeInt(0x05060708) }
        val multiRawSource = MultiRawSource(listOf(source1, source2))

        var sink = Buffer()
        multiRawSource.readAtMostTo(sink, 4)

        assertEquals(4, sink.size)
        assertContentEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04),
            sink.readByteArray()
        )

        sink = Buffer()
        multiRawSource.readAtMostTo(sink, 4)

        assertEquals(4, sink.size)
        assertContentEquals(
            byteArrayOf(0x05, 0x06, 0x07, 0x08),
            sink.readByteArray()
        )
    }

    @Test
    fun `read from a multiple source`() {
        val source1 = Buffer().apply { writeInt(0x01020304) }
        val source2 = Buffer().apply { writeInt(0x05060708) }
        val multiRawSource = MultiRawSource(listOf(source1, source2))

        var sink = Buffer()
        multiRawSource.readAtMostTo(sink, 5)

        assertEquals(5, sink.size)
        assertContentEquals(
            byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05),
            sink.readByteArray()
        )

        sink = Buffer()
        multiRawSource.readAtMostTo(sink, 4)

        assertEquals(3, sink.size)
        assertContentEquals(
            byteArrayOf(0x06, 0x07, 0x08),
            sink.readByteArray()
        )
    }

    @Test
    fun `read from already expired source`() {
        val source1 = Buffer().apply { writeInt(0x01020304) }
        val source2 = Buffer().apply { writeInt(0x05060708) }
        val multiRawSource = MultiRawSource(listOf(source1, source2))

        val sink = Buffer()
        multiRawSource.readAtMostTo(sink, 9)

        assertEquals(-1, multiRawSource.readAtMostTo(sink, 8))
    }
}