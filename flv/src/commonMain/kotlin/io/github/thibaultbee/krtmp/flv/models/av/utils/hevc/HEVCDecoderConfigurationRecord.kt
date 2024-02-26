/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.av.utils.hevc

import io.github.thibaultbee.krtmp.flv.extensions.shl
import io.github.thibaultbee.krtmp.flv.extensions.shr
import io.github.thibaultbee.krtmp.flv.extensions.writeByte
import io.github.thibaultbee.krtmp.flv.extensions.writeLong48
import io.github.thibaultbee.krtmp.flv.extensions.writeShort
import io.github.thibaultbee.krtmp.flv.models.packets.PacketWriter
import io.github.thibaultbee.krtmp.flv.models.NaluRawSource
import io.github.thibaultbee.krtmp.flv.models.av.utils.ChromaFormat
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlin.experimental.and

fun HEVCDecoderConfigurationRecord(
    sps: NaluRawSource,
    pps: NaluRawSource,
    vps: NaluRawSource
) = HEVCDecoderConfigurationRecord(
    listOf(sps),
    listOf(pps),
    listOf(vps)
)

fun HEVCDecoderConfigurationRecord(
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>,
    vps: List<NaluRawSource>
) = HEVCDecoderConfigurationRecord(sps + pps + vps)

fun HEVCDecoderConfigurationRecord(
    parameterSets: List<NaluRawSource>
): HEVCDecoderConfigurationRecord {
    var sps: Source? = null
    val nalUnitParameterSets = parameterSets.map {
        val bufferedSource = it.nalu.buffered()
        val peekedSource = bufferedSource.peek()
        val nalType = ((peekedSource.readByte() shr 1) and 0x3F).toByte()
        val type = HEVCDecoderConfigurationRecord.NalUnit.Type.entryOf(nalType)
        if ((sps == null) && (type == HEVCDecoderConfigurationRecord.NalUnit.Type.SPS)) {
            sps = bufferedSource.peek()
        }
        HEVCDecoderConfigurationRecord.NalUnit(
            type,
            NaluRawSource(bufferedSource, it.naluSize)
        )
    }

    require(sps != null) { "SPS is missing" }
    val parsedSps =
        SequenceParameterSets.parse(sps!!)

    return HEVCDecoderConfigurationRecord(
        generalProfileSpace = parsedSps.profileTierLevel.generalProfileSpace,
        generalTierFlag = parsedSps.profileTierLevel.generalTierFlag,
        generalProfileIdc = parsedSps.profileTierLevel.generalProfileIdc,
        generalProfileCompatibilityFlags = parsedSps.profileTierLevel.generalProfileCompatibilityFlags,
        generalConstraintIndicatorFlags = parsedSps.profileTierLevel.generalConstraintIndicatorFlags,
        generalLevelIdc = parsedSps.profileTierLevel.generalLevelIdc,
        chromaFormat = parsedSps.chromaFormat,
        bitDepthLumaMinus8 = parsedSps.bitDepthLumaMinus8,
        bitDepthChromaMinus8 = parsedSps.bitDepthChromaMinus8,
        numTemporalLayers = (parsedSps.maxSubLayersMinus1 + 1).toByte(),
        temporalIdNested = parsedSps.temporalIdNesting,
        // TODO get minSpatialSegmentationIdc from VUI
        parameterSets = nalUnitParameterSets
    )
}

data class HEVCDecoderConfigurationRecord(
    private val configurationVersion: Byte = 0x01,
    private val generalProfileSpace: Byte,
    private val generalTierFlag: Boolean,
    private val generalProfileIdc: HEVCProfile,
    private val generalProfileCompatibilityFlags: Int,
    private val generalConstraintIndicatorFlags: Long,
    private val generalLevelIdc: Byte,
    private val minSpatialSegmentationIdc: Int = 0,
    private val parallelismType: Byte = 0, // 0 = Unknown
    private val chromaFormat: ChromaFormat = ChromaFormat.YUV420,
    private val bitDepthLumaMinus8: Byte = 0,
    private val bitDepthChromaMinus8: Byte = 0,
    private val averageFrameRate: Short = 0, // 0 - Unspecified
    private val constantFrameRate: Byte = 0, // 0 - Unknown
    private val numTemporalLayers: Byte = 0, // 0 = Unknown
    private val temporalIdNested: Boolean = false,
    private val lengthSizeMinusOne: Byte = 3,
    private val parameterSets: List<NalUnit>,
) : PacketWriter() {
    override fun writeToSink(output: Sink) {
        output.writeByte(configurationVersion) // configurationVersion

        // profile_tier_level
        output.writeByte(
            (generalProfileSpace shl 6)
                    or (generalTierFlag shl 5)
                    or (generalProfileIdc.value.toInt())
        )
        output.writeInt(generalProfileCompatibilityFlags)
        output.writeLong48(generalConstraintIndicatorFlags)
        output.writeByte(generalLevelIdc)

        output.writeShort(0xf000 or (minSpatialSegmentationIdc)) // min_spatial_segmentation_idc 12 bits
        output.writeByte(0xfc or (parallelismType.toInt())) // parallelismType 2 bits
        output.writeByte(0xfc or (chromaFormat.value.toInt())) // chromaFormat 2 bits
        output.writeByte(0xf8 or (bitDepthLumaMinus8.toInt())) // bitDepthLumaMinus8 3 bits
        output.writeByte(0xf8 or (bitDepthChromaMinus8.toInt())) // bitDepthChromaMinus8 3 bits

        output.writeShort(averageFrameRate) // avgFrameRate
        output.writeByte(
            (constantFrameRate shl 6)
                    or ((numTemporalLayers and 0x7) shl 3)
                    or (temporalIdNested shl 2)
                    or (lengthSizeMinusOne.toInt() and 0x3)
        ) // constantFrameRate 2 bits = 1 for stable / numTemporalLayers 3 bits /  temporalIdNested 1 bit / lengthSizeMinusOne 2 bits

        output.writeByte(parameterSets.size) // numOfArrays

        // It is recommended that the arrays be in the order VPS, SPS, PPS, prefix SEI, suffix SEI.
        val vpsSets = mutableListOf<NalUnit>()
        val spsSets = mutableListOf<NalUnit>()
        val ppsSets = mutableListOf<NalUnit>()
        val prefixSeiSets = mutableListOf<NalUnit>()
        val suffixSeiSets = mutableListOf<NalUnit>()
        val otherSets = mutableListOf<NalUnit>()
        parameterSets.forEach {
            when (it.type) {
                NalUnit.Type.VPS -> vpsSets.add(it)
                NalUnit.Type.SPS -> spsSets.add(it)
                NalUnit.Type.PPS -> ppsSets.add(it)
                NalUnit.Type.PREFIX_SEI -> prefixSeiSets.add(it)
                NalUnit.Type.SUFFIX_SEI -> suffixSeiSets.add(it)
                else -> otherSets.add(it)
            }
        }
        vpsSets.forEach { it.writeToSink(output) }
        spsSets.forEach { it.writeToSink(output) }
        ppsSets.forEach { it.writeToSink(output) }
        prefixSeiSets.forEach { it.writeToSink(output) }
        suffixSeiSets.forEach { it.writeToSink(output) }
        otherSets.forEach { it.writeToSink(output) }
    }

    data class NalUnit(
        val type: Type,
        val source: NaluRawSource,
        val completeness: Boolean = true
    ) {
        fun writeToSink(output: Sink) {
            output.writeByte((completeness shl 7) or type.value.toInt()) // array_completeness + reserved 1bit + naluType 6 bytes
            output.writeShort(1) // numNalus
            output.writeShort(source.naluSize) // nalUnitLength
            output.write(source.nalu, source.naluSize.toLong())
        }

        enum class Type(val value: Byte) {
            VPS(32),
            SPS(33),
            PPS(34),
            AUD(35),
            EOS(36),
            EOB(37),
            FD(38),
            PREFIX_SEI(39),
            SUFFIX_SEI(40);

            companion object {
                fun entryOf(value: Byte) = entries.first { it.value == value }
            }
        }
    }
}
