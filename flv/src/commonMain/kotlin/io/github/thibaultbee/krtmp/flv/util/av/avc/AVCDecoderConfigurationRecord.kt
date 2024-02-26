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

import io.github.thibaultbee.krtmp.flv.sources.NaluRawSource
import io.github.thibaultbee.krtmp.flv.util.PacketWriter
import io.github.thibaultbee.krtmp.flv.util.av.ChromaFormat
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import kotlinx.io.Sink
import kotlinx.io.buffered


fun AVCDecoderConfigurationRecord(
    sps: NaluRawSource,
    pps: NaluRawSource
) = AVCDecoderConfigurationRecord(listOf(sps), listOf(pps))

fun AVCDecoderConfigurationRecord(
    sps: List<NaluRawSource>,
    pps: List<NaluRawSource>
): AVCDecoderConfigurationRecord {
    /**
     *  We need to consume the first SPS to get profileIdc, profileCompatibility,
     *  levelIdc,...
     */
    val mutableSps = sps.toMutableList()
    val firstSps = mutableSps.removeAt(0)
    val bufferedSps = firstSps.nalu.buffered()
    val peekedSps = bufferedSps.peek()

    peekedSps.skip(1) // skip NALU type (1 byte)
    val profileIdc: Byte = peekedSps.readByte()
    val profileCompatibility = peekedSps.readByte()
    val levelIdc = peekedSps.readByte()

    val newSps = mutableListOf<NaluRawSource>()
    newSps.add(NaluRawSource(bufferedSps, firstSps.naluSize))
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

data class AVCDecoderConfigurationRecord(
    private val configurationVersion: Byte = 0x01,
    private val profileIdc: Byte,
    private val profileCompatibility: Byte,
    private val levelIdc: Byte,
    private val chromaFormat: ChromaFormat = ChromaFormat.YUV420,
    private val bitDepthLumaMinus8: Byte = 0,
    private val bitDepthChromaMinus8: Byte = 0,
    private val sps: List<NaluRawSource>,
    private val pps: List<NaluRawSource>
) : PacketWriter() {
    override fun write(output: Sink) {
        output.writeByte(configurationVersion) // configurationVersion
        output.writeByte(profileIdc) // AVCProfileIndication
        output.writeByte(profileCompatibility) // profile_compatibility
        output.writeByte(levelIdc) // AVCLevelIndication

        output.writeByte(0xff) // 6 bits reserved + lengthSizeMinusOne - 4 bytes
        output.writeByte(((0b111 shl 5) or (sps.size))) // 3 bits reserved + numOfSequenceParameterSets - 5 bytes
        sps.forEach {
            output.writeShort((it.naluSize).toShort()) // sequenceParameterSetLength
            output.write(it.nalu, it.naluSize.toLong())
        }

        output.writeByte(pps.size) // numOfPictureParameterSets
        pps.forEach {
            output.writeShort((it.naluSize).toShort()) // pictureParameterSetLength
            output.write(it.nalu, it.naluSize.toLong())
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