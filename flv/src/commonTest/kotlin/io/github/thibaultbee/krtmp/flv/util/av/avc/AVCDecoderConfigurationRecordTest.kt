package io.github.thibaultbee.krtmp.flv.util.av.avc

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.sources.NaluRawSource
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
                sps = listOf(NaluRawSource(sps)),
                pps = listOf(NaluRawSource(pps))
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
            AVCDecoderConfigurationRecord(sps = NaluRawSource(sps), pps = NaluRawSource(pps))

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }

    @Test
    fun `inferred from sps and pps with start code`() {
        val expected =
            Resource("video/avc/decoderConfigurationRecord/AVCDecoderConfigurationRecord").toByteArray()

        val sps = Resource("video/avc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/avc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord = AVCDecoderConfigurationRecord(
            sps = NaluRawSource(byteArrayOf(0x00, 0x00, 0x00, 0x01) + sps),
            pps = NaluRawSource(byteArrayOf(0x00, 0x00, 0x00, 0x01) + pps)
        )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }
}