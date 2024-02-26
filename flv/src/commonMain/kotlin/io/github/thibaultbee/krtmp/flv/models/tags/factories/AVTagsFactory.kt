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
package io.github.thibaultbee.krtmp.flv.models.tags.factories

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.models.AudioFrame
import io.github.thibaultbee.krtmp.flv.models.Frame
import io.github.thibaultbee.krtmp.flv.models.VideoFrame
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvAudioConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvConfig
import io.github.thibaultbee.krtmp.flv.models.tags.AACPacketType
import io.github.thibaultbee.krtmp.flv.models.tags.AudioTag
import io.github.thibaultbee.krtmp.flv.models.tags.FlvTag
import io.github.thibaultbee.krtmp.flv.models.tags.PacketType


class AVTagsFactory(
    private val frame: Frame,
    private val config: FlvConfig
) {
    fun build(): List<FlvTag> {
        return when (frame) {
            is VideoFrame -> {
                createVideoTags(frame)
            }

            is AudioFrame -> {
                createAudioTags(frame, config as FlvAudioConfig)
            }
        }
    }

    private fun createAudioTags(
        frame: AudioFrame,
        config: FlvAudioConfig
    ): List<FlvTag> {
        val audioTags = mutableListOf<FlvTag>()
        if (frame.descriptor != null) {
            audioTags.add(
                AudioTag(
                    frame.timestampMs,
                    frame.descriptor.source,
                    frame.descriptor.byteCount,
                    config,
                    if (config.mimeType == MimeType.AUDIO_AAC) {
                        AACPacketType.SEQUENCE_HEADER
                    } else {
                        null
                    }
                )
            )
        }
        audioTags.add(
            AudioTag(
                frame.timestampMs,
                frame.data.source,
                frame.data.byteCount,
                config,
                if (frame.mimeType == MimeType.AUDIO_AAC) {
                    AACPacketType.RAW
                } else {
                    null
                },
            )
        )

        return audioTags
    }

    private fun createVideoTags(
        frame: VideoFrame,
    ): List<FlvTag> {
        val videoTags = mutableListOf<FlvTag>()
        if (frame.descriptor != null) {
            videoTags.add(
                VideoTagFactory(
                    frame.timestampMs,
                    frame.descriptor.source,
                    frame.descriptor.byteCount,
                    frame.mimeType,
                    frame.isKeyFrame,
                    PacketType.SEQUENCE_START
                ).build()
            )
        }

        videoTags.add(
            VideoTagFactory(
                frame.timestampMs,
                frame.data.source,
                frame.data.byteCount,
                config.mimeType,
                frame.isKeyFrame,
                PacketType.CODED_FRAMES_X, // For extended codec only.
            ).build()
        )

        return videoTags
    }
}
