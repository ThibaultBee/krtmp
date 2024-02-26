package io.github.thibaultbee.krtmp.flv.models.av.utils.hevc

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.models.NaluRawSource
import io.github.thibaultbee.krtmp.flv.models.av.utils.ChromaFormat
import kotlin.test.Test
import kotlin.test.assertContentEquals

class HEVCDecoderConfigurationRecordTest {
    @Test
    fun `from constructor`() {
        val expected =
            Resource("video/hevc/decoderConfigurationRecord/HEVCDecoderConfigurationRecord").toByteArray()

        val vps = Resource("video/hevc/decoderConfigurationRecord/vps").toByteArray()
        val sps = Resource("video/hevc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/hevc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord =
            HEVCDecoderConfigurationRecord(
                configurationVersion = 1,
                generalProfileSpace = 0,
                generalTierFlag = false,
                generalProfileIdc = HEVCProfile.MAIN,
                generalProfileCompatibilityFlags = 0x60000000,
                generalConstraintIndicatorFlags = 0x800000000000,
                generalLevelIdc = 120,
                minSpatialSegmentationIdc = 0,
                parallelismType = 0,
                chromaFormat = ChromaFormat.YUV420,
                bitDepthLumaMinus8 = 0,
                bitDepthChromaMinus8 = 0,
                averageFrameRate = 0,
                constantFrameRate = 0,
                numTemporalLayers = 1,
                temporalIdNested = true,
                lengthSizeMinusOne = 3,
                parameterSets = listOf(
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.VPS,
                        NaluRawSource(vps)
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.SPS,
                        NaluRawSource(sps)
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.PPS,
                        NaluRawSource(pps)
                    )
                )
            )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }

    @Test
    fun `inferred from vps and sps and pps without header`() {
        val expected =
            Resource("video/hevc/decoderConfigurationRecord/HEVCDecoderConfigurationRecord").toByteArray()

        val vps = Resource("video/hevc/decoderConfigurationRecord/vps").toByteArray()
        val sps = Resource("video/hevc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/hevc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord = HEVCDecoderConfigurationRecord(
            vps = NaluRawSource(vps),
            sps = NaluRawSource(sps),
            pps = NaluRawSource(pps)
        )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }

    @Test
    fun `inferred from vps and sps and pps with start code`() {
        val expected =
            Resource("video/hevc/decoderConfigurationRecord/HEVCDecoderConfigurationRecord").toByteArray()

        val vps = Resource("video/hevc/decoderConfigurationRecord/vps").toByteArray()
        val sps = Resource("video/hevc/decoderConfigurationRecord/sps").toByteArray()
        val pps = Resource("video/hevc/decoderConfigurationRecord/pps").toByteArray()

        val decoderConfigurationRecord = HEVCDecoderConfigurationRecord(
            vps = NaluRawSource(byteArrayOf(0x00, 0x00, 0x00, 0x01) + vps),
            sps = NaluRawSource(byteArrayOf(0x00, 0x00, 0x00, 0x01) + sps),
            pps = NaluRawSource(byteArrayOf(0x00, 0x00, 0x00, 0x01) + pps)
        )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }
}