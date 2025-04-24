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
package io.github.thibaultbee.krtmp.flv.util.av.hevc

import io.github.thibaultbee.krtmp.flv.bitreaders.BitReader
import io.github.thibaultbee.krtmp.flv.bitreaders.H26XBitReader
import io.github.thibaultbee.krtmp.flv.util.av.ChromaFormat
import io.github.thibaultbee.krtmp.flv.util.extensions.rbsp
import kotlinx.io.Source

/**
 * The HEVC VPS
 *
 * It is a simplified version of the VPS, only containing the necessary information for the
 * [HEVCDecoderConfigurationRecord].
 */
internal data class VideoParameterSets(
    val maxSubLayersMinus1: Byte, // 3 bits
    val profileTierLevel: ProfileTierLevel,
) {
    companion object {
        fun parse(input: Source): VideoParameterSets {
            val rbsp = input.rbsp // remove emulation prevention bytes
            val reader = H26XBitReader(rbsp)

            reader.readInt(13) // vps_video_parameter_set_id / vps_reserved_three_2bits / vps_max_layers_minus1

            val maxSubLayersMinus1 = reader.readByte(3) // vps_max_sub_layers_minus1

            reader.readInt(17) // vps_temporal_id_nesting_flag / vps_reserved_0xffff_16bits

            val profileTierLevel = ProfileTierLevel.parse(reader, maxSubLayersMinus1)

            return VideoParameterSets(
                maxSubLayersMinus1,
                profileTierLevel
            )
        }
    }
}

/**
 * The HEVC SPS
 *
 * It is a simplified version of the SPS, only containing the necessary information for the
 * [HEVCDecoderConfigurationRecord].
 */
internal data class SequenceParameterSets(
    val videoParameterSetId: Byte, // 4 bits
    val maxSubLayersMinus1: Byte, // 3 bits
    val temporalIdNesting: Boolean, // 1 bit
    val profileTierLevel: ProfileTierLevel,
    val seqParameterSetId: Int,
    val chromaFormat: ChromaFormat,
    val picWidthInLumaSamples: Int,
    val picHeightInLumaSamples: Int,
    val bitDepthLumaMinus8: Byte,
    val bitDepthChromaMinus8: Byte
) {
    companion object {
        fun parse(input: Source): SequenceParameterSets {
            val rbsp = input.rbsp // remove emulation prevention bytes
            val reader = H26XBitReader(rbsp)
            reader.readLong(16) // Dropping nal_unit_header: forbidden_zero_bit / nal_unit_type / nuh_layer_id / nuh_temporal_id_plus1

            val videoParameterSetId = reader.readByte(4)
            val maxNumSubLayersMinus1 = reader.readByte(3)
            val temporalIdNesting = reader.readBoolean()

            val profileTierLevel = ProfileTierLevel.parse(reader, maxNumSubLayersMinus1)
            val seqParameterSetId = reader.readUE()
            val chromaFormat = ChromaFormat.entryOf(reader.readUE().toByte())
            if (chromaFormat == ChromaFormat.YUV444) {
                reader.readBoolean()
            }

            val picWidthInLumaSamples = reader.readUE()
            val picHeightInLumaSamples = reader.readUE()

            if (reader.readBoolean()) { // conformance_window_flag
                reader.readUE() // conf_win_left_offset
                reader.readUE() // conf_win_right_offset
                reader.readUE() // conf_win_top_offset
                reader.readUE() // conf_win_bottom_offset
            }

            val bitDepthLumaMinus8 = reader.readUE().toByte()
            val bitDepthChromaMinus8 = reader.readUE().toByte()
            reader.readUE() // log2_max_pic_order_cnt_lsb_minus4

            val subLayerOrderingInfoPresentFlag = reader.readBoolean()
            for (i in (if (subLayerOrderingInfoPresentFlag) 0 else maxNumSubLayersMinus1)..maxNumSubLayersMinus1) {
                reader.readUE() // max_dec_pic_buffering_minus1
                reader.readUE() // max_num_reorder_pics
                reader.readUE() // max_latency_increase_plus1
            }

            reader.readUE() // log2_min_luma_coding_block_size_minus3
            reader.readUE() // log2_diff_max_min_luma_coding_block_size
            reader.readUE() // log2_min_transform_block_size_minus2
            reader.readUE() // log2_diff_max_min_transform_block_size
            reader.readUE() // max_transform_hierarchy_depth_inter
            reader.readUE() // max_transform_hierarchy_depth_intra

            return SequenceParameterSets(
                videoParameterSetId,
                maxNumSubLayersMinus1,
                temporalIdNesting,
                profileTierLevel,
                seqParameterSetId,
                chromaFormat,
                picWidthInLumaSamples,
                picHeightInLumaSamples,
                bitDepthLumaMinus8,
                bitDepthChromaMinus8
            )
        }
    }
}

internal data class ProfileTierLevel(
    val generalProfileSpace: Byte, // 2 bits
    val generalTierFlag: Boolean, // 1 bits
    val generalProfileIdc: HEVCProfile, // 5 bits
    val generalProfileCompatibilityFlags: Int, // 32 bits
    val generalConstraintIndicatorFlags: Long, // 48 bits
    val generalLevelIdc: Byte
) {
    companion object {
        fun parse(input: Source, maxNumSubLayersMinus1: Byte) =
            parse(BitReader(input), maxNumSubLayersMinus1)

        fun parse(
            reader: BitReader,
            maxNumSubLayersMinus1: Byte
        ): ProfileTierLevel {
            val generalProfileSpace = reader.readByte(2)
            val generalTierFlag = reader.readBoolean()
            val generalProfileIdc = HEVCProfile.entryOf(reader.readShort(5))

            val generalProfileCompatibilityFlags = reader.readInt(32)
            val generalConstraintIndicatorFlags = reader.readLong(48)
            val generalLevelIdc = reader.readByte(8)

            val subLayerProfilePresentFlag = mutableListOf<Boolean>()
            val subLayerLevelPresentFlag = mutableListOf<Boolean>()
            for (i in 0 until maxNumSubLayersMinus1) {
                subLayerProfilePresentFlag.add(reader.readBoolean())
                subLayerLevelPresentFlag.add(reader.readBoolean())
            }

            if (maxNumSubLayersMinus1 > 0) {
                for (i in maxNumSubLayersMinus1..8) {
                    reader.readLong(2) // reserved_zero_2bits
                }
            }

            for (i in 0 until maxNumSubLayersMinus1) {
                if (subLayerProfilePresentFlag[i]) {
                    reader.readLong(32) // skip
                    reader.readLong(32) // skip
                    reader.readLong(24) // skip
                }

                if (subLayerLevelPresentFlag[i]) {
                    reader.readLong(8) // skip
                }
            }

            return ProfileTierLevel(
                generalProfileSpace,
                generalTierFlag,
                generalProfileIdc,
                generalProfileCompatibilityFlags,
                generalConstraintIndicatorFlags,
                generalLevelIdc
            )
        }
    }
}