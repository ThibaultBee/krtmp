package io.github.thibaultbee.krtmp.flv.models.sources

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