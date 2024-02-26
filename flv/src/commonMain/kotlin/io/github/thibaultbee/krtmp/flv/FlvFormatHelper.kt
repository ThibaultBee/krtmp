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
package io.github.thibaultbee.krtmp.flv

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.models.av.config.CodecID
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.tags.ExtendedVideoTag

object FlvMuxerHelper {
    val video = VideoFlvMuxerHelper
    val audio = AudioFlvMuxerHelper
}

object VideoFlvMuxerHelper {
    /**
     * Get FLV Muxer supported video encoders list
     */
    val supportedEncoders: List<MimeType>
        get() {
            val extendedSupportedCodec = ExtendedVideoTag.supportedCodecs
            val supportedCodecList = CodecID.entries.mapNotNull {
                try {
                    it.mimeType
                } catch (e: Exception) {
                    null
                }
            }.filter {
                listOf(
                    MimeType.VIDEO_AVC
                ).contains(it)
            }
            return supportedCodecList + extendedSupportedCodec
        }
}

object AudioFlvMuxerHelper {
    /**
     * Get FLV Muxer supported audio encoders list
     */
    val supportedEncoders: List<MimeType>
        get() {
            return SoundFormat.entries.mapNotNull {
                try {
                    it.mimeType
                } catch (e: Exception) {
                    null
                }
            }
        }

    fun getSupportedSampleRates() = SoundRate.entries.map { it.sampleRate }

    fun getSupportedByteFormats() = SoundSize.entries.map { it.byteFormat }
}