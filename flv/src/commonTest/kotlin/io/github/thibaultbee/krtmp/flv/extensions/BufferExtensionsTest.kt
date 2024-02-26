package io.github.thibaultbee.krtmp.flv.extensions

import io.github.thibaultbee.krtmp.flv.util.extensions.rbsp
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals

class BufferExtensionsTest {
    @Test
    fun `extract rbsp with single emulation prevention byte`() {
        val expected = byteArrayOf(
            66,
            0,
            0,
            1
        )

        val testBuffer = Buffer().apply {
            write(
                byteArrayOf(
                    66,
                    0,
                    0,
                    3,
                    1
                )
            )
        }

        val actual = testBuffer.rbsp

        assertContentEquals(expected, actual.readByteArray())
    }

    @Test
    fun `extractRbsp with start code and with multiple emulation prevention bytes`() {
        val expected = byteArrayOf(
            66,
            1,
            1,
            1,
            96,
            0,
            0,
            0,
            -80,
            0,
            0,
            0,
            0,
            0,
            60,
            -96,
            8,
            8,
            5,
            7,
            19,
            -27,
            -82,
            -28,
            -55,
            46,
            -96,
            11,
            -76,
            40,
            74
        )

        val testBuffer = Buffer().apply {
            write(
                byteArrayOf(
                    66,
                    1,
                    1,
                    1,
                    96,
                    0,
                    0,
                    3,
                    0,
                    -80,
                    0,
                    0,
                    3,
                    0,
                    0,
                    3,
                    0,
                    60,
                    -96,
                    8,
                    8,
                    5,
                    7,
                    19,
                    -27,
                    -82,
                    -28,
                    -55,
                    46,
                    -96,
                    11,
                    -76,
                    40,
                    74
                )
            )
        }

        val actual = testBuffer.rbsp

        assertContentEquals(expected, actual.readByteArray())
    }
}