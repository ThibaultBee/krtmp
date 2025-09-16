package io.github.thibaultbee.krtmp.flv.util.av.hevc

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.sources.ByteArrayBackedRawSource
import io.github.thibaultbee.krtmp.flv.util.av.ChromaFormat
import io.github.thibaultbee.krtmp.flv.util.readByteArray
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
                        ByteArrayBackedRawSource(
                            vps
                        ),
                        vps.size
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.SPS,
                        ByteArrayBackedRawSource(
                            sps
                        ),
                        sps.size
                    ),
                    HEVCDecoderConfigurationRecord.NalUnit(
                        HEVCDecoderConfigurationRecord.NalUnit.Type.PPS,
                        ByteArrayBackedRawSource(
                            pps
                        ),
                        pps.size
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
            vps = ByteArrayBackedRawSource(
                vps
            ) to vps.size,
            sps = ByteArrayBackedRawSource(
                sps
            ) to sps.size,
            pps = ByteArrayBackedRawSource(
                pps
            ) to pps.size,
        )

        assertContentEquals(expected, decoderConfigurationRecord.readByteArray())
    }
}