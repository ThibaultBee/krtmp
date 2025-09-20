package io.github.thibaultbee.krtmp.flv.util.av.avc

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.common.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.util.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals

class AVCDecoderConfigurationRecordTest {
    @Test
    fun `from constructor`() {
        val expected =
            Resource("video/avc/decoderConfigurationRecord/AVCDecoderConfigurationRecord").toByteArray()

        val sps = Resource("video/avc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/avc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord =
            AVCDecoderConfigurationRecord(
                profileIdc = 66,
                profileCompatibility = 192.toByte(),
                levelIdc = 50,
                sps = listOf(
                    ByteArrayBackedRawSource(
                        sps
                    ) to sps.size
                ), // 4 bytes to remove header
                pps = listOf(
                    ByteArrayBackedRawSource(
                        pps
                    ) to pps.size
                ) // 4 bytes to remove header
            )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }

    @Test
    fun `inferred from sps and pps without header`() {
        val expected =
            Resource("video/avc/decoderConfigurationRecord/AVCDecoderConfigurationRecord").toByteArray()

        val sps = Resource("video/avc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/avc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord =
            AVCDecoderConfigurationRecord(
                sps = ByteArrayBackedRawSource(
                    sps
                ) to sps.size, pps = ByteArrayBackedRawSource(
                    pps
                ) to pps.size
            )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }
}