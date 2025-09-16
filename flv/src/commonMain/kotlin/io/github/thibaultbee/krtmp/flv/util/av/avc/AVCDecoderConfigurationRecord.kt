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
package io.github.thibaultbee.krtmp.flv.util.av.avc

import io.github.thibaultbee.krtmp.flv.util.PacketWriter
import io.github.thibaultbee.krtmp.flv.util.av.ChromaFormat
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.buffered

/**
 * Creates an [AVCDecoderConfigurationRecord] from the provided SPS and PPS NAL units.
 *
 * This function extracts necessary profile information from the SPS NAL unit
 * to populate the AVCDecoderConfigurationRecord fields.
 *
 * The SPS and PPS are provided without start codes (0x00000001) or AVCC headers.
 *
 * @param sps A pair containing a RawSource representing the SPS NAL unit and its size in bytes.
 * @param pps A pair containing a RawSource representing the PPS NAL unit and its size in bytes.
 * @return An instance of [AVCDecoderConfigurationRecord] populated with the provided SPS and PPS data.
 */
fun AVCDecoderConfigurationRecord(
    sps: Pair<RawSource, Int>,
    pps: Pair<RawSource, Int>
) = AVCDecoderConfigurationRecord(listOf(sps), listOf(pps))

/**
 * Creates an [AVCDecoderConfigurationRecord] from the provided SPS and PPS NAL units.
 *
 * This function extracts necessary profile information from the first SPS NAL unit
 * to populate the AVCDecoderConfigurationRecord fields.
 *
 * The SPS and PPS are provided without start codes (0x00000001) or AVCC headers.
 *
 * @param sps A list of pairs where each pair contains a RawSource representing an SPS NAL unit and its size in bytes.
 * @param pps A list of pairs where each pair contains a RawSource representing a PPS NAL unit and its size in bytes.
 * @return An instance of [AVCDecoderConfigurationRecord] populated with the provided SPS and PPS data.
 */
fun AVCDecoderConfigurationRecord(
    sps: List<Pair<RawSource, Int>>,
    pps: List<Pair<RawSource, Int>>
): AVCDecoderConfigurationRecord {
    /**
     *  We need to consume the first SPS to get profileIdc, profileCompatibility,
     *  levelIdc,...
     */
    val mutableSps = sps.toMutableList()
    val firstSps = mutableSps.removeAt(0)
    val bufferedSps = firstSps.first.buffered()
    val peekedSps = bufferedSps.peek()

    peekedSps.skip(1) // skip NALU type (1 byte)
    val profileIdc: Byte = peekedSps.readByte()
    val profileCompatibility = peekedSps.readByte()
    val levelIdc = peekedSps.readByte()

    val newSps = mutableListOf<Pair<RawSource, Int>>()
    newSps.add(bufferedSps to firstSps.second)
    newSps.addAll(mutableSps)

    return AVCDecoderConfigurationRecord(
        profileIdc = profileIdc,
        profileCompatibility = profileCompatibility,
        levelIdc = levelIdc,
        chromaFormat = ChromaFormat.YUV420, //TODO: get real values
        bitDepthLumaMinus8 = 0, //TODO: get real values
        bitDepthChromaMinus8 = 0, //TODO: get real values
        sps = newSps, // sps has been read
        pps = pps
    )
}

/**
 * Represents an AVC (H.264) Decoder Configuration Record, which contains important
 * information about the video stream such as profile, level, and parameter sets (SPS and PPS).
 *
 * This record is typically used in container formats like MP4 and FLV to describe the
 * properties of the H.264 video stream.
 *
 * @property configurationVersion The version of the configuration record (default is 1).
 * @property profileIdc The profile indication (e.g., Baseline, Main, High).
 * @property profileCompatibility The profile compatibility flags.
 * @property levelIdc The level indication (e.g., 3.1, 4.0).
 * @property chromaFormat The chroma format (default is YUV420).
 * @property bitDepthLumaMinus8 The bit depth for luma minus 8 (default is 0).
 * @property bitDepthChromaMinus8 The bit depth for chroma minus 8 (default is 0).
 * @property sps A list of pairs where each pair contains a RawSource representing an SPS NAL unit and its size in bytes.
 * @property pps A list of pairs where each pair contains a RawSource representing a PPS NAL unit and its size in bytes.
 */
data class AVCDecoderConfigurationRecord(
    private val configurationVersion: Byte = 0x01,
    private val profileIdc: Byte,
    private val profileCompatibility: Byte,
    private val levelIdc: Byte,
    private val chromaFormat: ChromaFormat = ChromaFormat.YUV420,
    private val bitDepthLumaMinus8: Byte = 0,
    private val bitDepthChromaMinus8: Byte = 0,
    private val sps: List<Pair<RawSource, Int>>,
    private val pps: List<Pair<RawSource, Int>>
) : PacketWriter() {
    override fun write(output: Sink) {
        output.writeByte(configurationVersion) // configurationVersion
        output.writeByte(profileIdc) // AVCProfileIndication
        output.writeByte(profileCompatibility) // profile_compatibility
        output.writeByte(levelIdc) // AVCLevelIndication

        output.writeByte(0xff) // 6 bits reserved + lengthSizeMinusOne - 4 bytes
        output.writeByte(((0b111 shl 5) or (sps.size))) // 3 bits reserved + numOfSequenceParameterSets - 5 bytes
        sps.forEach {
            output.writeShort((it.second).toShort()) // sequenceParameterSetLength
            output.write(it.first, it.second.toLong())
        }

        output.writeByte(pps.size) // numOfPictureParameterSets
        pps.forEach {
            output.writeShort((it.second).toShort()) // pictureParameterSetLength
            output.write(it.first, it.second.toLong())
        }

        if ((profileIdc == 100.toByte()) || (profileIdc == 110.toByte()) || (profileIdc == 122.toByte()) || (profileIdc == 144.toByte())) {
            output.writeByte(
                ((0b111111 shl 2) // reserved
                        or chromaFormat.value.toInt()) // chroma_format
            )
            output.writeByte(
                ((0b11111 shl 3) // reserved
                        or bitDepthLumaMinus8.toInt()) // bit_depth_luma_minus8
            )
            output.writeByte(
                ((0b11111 shl 3) // reserved
                        or bitDepthChromaMinus8.toInt()) // bit_depth_chroma_minus8
            )
            output.writeByte(0)
        }
    }
}