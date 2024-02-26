/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.tags.factories

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.models.tags.ExtendedVideoTag
import io.github.thibaultbee.krtmp.flv.models.tags.FlvTag
import io.github.thibaultbee.krtmp.flv.models.tags.FrameType
import io.github.thibaultbee.krtmp.flv.models.tags.LegacyVideoTag
import io.github.thibaultbee.krtmp.flv.models.tags.PacketType
import kotlinx.io.RawSource

class VideoTagFactory(
    private val timestampMs: Int,
    private val data: RawSource,
    private val dataSize: Int,
    private val mimeType: MimeType,
    private val isKeyFrame: Boolean,
    private val packetType: PacketType
) {
    fun build(): FlvTag {
        val frameType = if (isKeyFrame) {
            FrameType.KEY
        } else {
            FrameType.INTER
        }
        return if (ExtendedVideoTag.isSupportedCodec(mimeType)) {
            ExtendedVideoTag(timestampMs, data, dataSize, mimeType, frameType, packetType)
        } else {
            // Packet type if only for AVC
            LegacyVideoTag(
                timestampMs,
                data,
                dataSize,
                mimeType,
                frameType,
                packetType.avcPacketType
            )
        }
    }
}
