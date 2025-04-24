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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.containers.amfEcmaArrayOf
import io.github.thibaultbee.krtmp.amf.elements.containers.amfObjectOf
import io.github.thibaultbee.krtmp.flv.config.FLVAudioConfig
import io.github.thibaultbee.krtmp.flv.config.FLVVideoConfig
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.tags.OnMetadata.Metadata
import io.github.thibaultbee.krtmp.flv.util.AmfUtil.amf
import kotlinx.serialization.Serializable

/**
 * Creates a [OnMetadata] from an ECMA array
 *
 * @param value ECMA array of the [Metadata]
 * @return The onMetaData data
 */
fun OnMetadata(value: AmfEcmaArray) =
    OnMetadata(Metadata.fromArray(value))

/**
 * Creates a [OnMetadata] from multiple audio and video configurations
 *
 * @param audioConfigs The audio configurations
 * @param videoConfigs The video configurations
 */
fun OnMetadata(audioConfigs: Map<Int, FLVAudioConfig>, videoConfigs: Map<Int, FLVVideoConfig>) =
    OnMetadata(Metadata.fromConfigs(audioConfigs, videoConfigs))

/**
 * Creates a [OnMetadata] from audio and video configurations
 *
 * @param audioConfig The audio configuration
 * @param videoConfig The video configuration
 * @return The onMetaData data
 */
fun OnMetadata(audioConfig: FLVAudioConfig?, videoConfig: FLVVideoConfig?) =
    OnMetadata(Metadata.fromConfigs(audioConfig, videoConfig))

/**
 * The onMetaData data
 * @param metadata the Metadata to write
 */
class OnMetadata(
    val metadata: Metadata,
) :
    ScriptDataObject(
        ON_METADATA,
        metadata.encode()
    ) {

    @Serializable
    open class Metadata(
        val duration: Double = 0.0,
        val audiocodecid: Double? = null,
        val audiodatarate: Double? = null,
        val audiosamplerate: Double? = null,
        val audiosamplesize: Double? = null,
        val stereo: Boolean? = null,
        val videocodecid: Double? = null,
        val videodatarate: Double? = null,
        val width: Double? = null,
        val height: Double? = null,
        val framerate: Double? = null,
        val audioTrackIdInfoMap: Map<String, AudioTrackIdInfo>? = null,
        val videoTrackIdInfoMap: Map<String, VideoTrackIdInfo>? = null
    ) {
        fun encode(): AmfEcmaArray {
            return amfEcmaArrayOf(
                (amf.encodeToAmfElement(
                    serializer(),
                    this
                ) as AmfObject)
            )
        }

        companion object {
            fun fromArray(array: AmfEcmaArray): Metadata {
                return amf.decodeFromAmfElement(serializer(), amfObjectOf(array))
            }

            fun fromConfigs(
                audioConfig: FLVAudioConfig?,
                videoConfig: FLVVideoConfig?
            ): Metadata {
                val audioConfigs = audioConfig?.let { mapOf(0 to it) } ?: emptyMap()
                val videoConfigs = videoConfig?.let { mapOf(0 to it) } ?: emptyMap()
                return fromConfigs(audioConfigs, videoConfigs)
            }

            fun fromConfigs(
                audioConfigs: Map<Int, FLVAudioConfig>,
                videoConfigs: Map<Int, FLVVideoConfig>
            ): Metadata {
                require((audioConfigs.isNotEmpty()) or (videoConfigs.isNotEmpty())) {
                    "Either audio or video config must be provided"
                }
                require(audioConfigs.keys.distinct().size == audioConfigs.keys.size) {
                    "Audio config keys must be distinct"
                }
                require(videoConfigs.keys.distinct().size == videoConfigs.keys.size) {
                    "Video config keys must be distinct"
                }
                val firstAudioConfig = audioConfigs.values.firstOrNull()
                val firstVideoConfig = videoConfigs.values.firstOrNull()

                val audioTrackIdInfoMap = mutableMapOf<String, AudioTrackIdInfo>()
                audioConfigs.forEach { (key, config) ->
                    audioTrackIdInfoMap[key.toString()] = AudioTrackIdInfo(
                        audiocodecid = if (config.mediaType != firstAudioConfig?.mediaType) {
                            config.metadataType.toDouble()
                        } else {
                            null
                        },
                        audiodatarate = if (config.bitrateBps != firstAudioConfig?.bitrateBps) {
                            config.bitrateBps.div(1000).toDouble()
                        } else {
                            null
                        },
                        channels = if (config.soundType != firstAudioConfig?.soundType) {
                            config.soundType.value.toDouble()
                        } else {
                            null
                        },
                        audiosamplerate = if (config.soundRate != firstAudioConfig?.soundRate) {
                            config.soundRate.sampleRate.toDouble()
                        } else {
                            null
                        },
                        audiosamplesize = if (config.soundSize != firstAudioConfig?.soundSize) {
                            config.soundSize.byteFormat.numOfBytes.times(Byte.SIZE_BITS)
                                .toDouble()
                        } else {
                            null
                        }
                    )
                }

                val videoTrackIdInfoMap = mutableMapOf<String, VideoTrackIdInfo>()
                videoConfigs.forEach { (key, config) ->
                    videoTrackIdInfoMap[key.toString()] = VideoTrackIdInfo(
                        videocodecid = if (config.mediaType != firstVideoConfig?.mediaType) {
                            config.metadataType.toDouble()
                        } else {
                            null
                        },
                        videodatarate = if (config.bitrateBps != firstVideoConfig?.bitrateBps) {
                            config.bitrateBps.div(1000).toDouble()
                        } else {
                            null
                        },
                        width = if (config.width != firstVideoConfig?.width) {
                            config.width.toDouble()
                        } else {
                            null
                        },
                        height = if (config.height != firstVideoConfig?.height) {
                            config.height.toDouble()
                        } else {
                            null
                        }
                    )
                }

                return Metadata(
                    duration = 0.0,
                    audiocodecid = firstAudioConfig?.metadataType?.toDouble(),
                    audiodatarate = firstAudioConfig?.bitrateBps?.div(1000)?.toDouble(), // to Kbps
                    audiosamplerate = firstAudioConfig?.soundRate?.sampleRate?.toDouble(),
                    audiosamplesize = firstAudioConfig?.soundSize?.byteFormat?.numOfBytes?.times(
                        Byte.SIZE_BITS
                    )?.toDouble(),
                    stereo = firstAudioConfig?.let { (it.soundType == SoundType.STEREO) },
                    videocodecid = firstVideoConfig?.metadataType?.toDouble(),
                    videodatarate = firstVideoConfig?.bitrateBps?.div(1000)?.toDouble(), // to Kbps
                    width = firstVideoConfig?.width?.toDouble(),
                    height = firstVideoConfig?.height?.toDouble(),
                    framerate = firstVideoConfig?.frameRate?.toDouble(),
                    audioTrackIdInfoMap = if (audioTrackIdInfoMap.size > 1) {
                        audioTrackIdInfoMap
                    } else {
                        null
                    },
                    videoTrackIdInfoMap = if (videoTrackIdInfoMap.size > 1) {
                        videoTrackIdInfoMap
                    } else {
                        null
                    }
                )
            }
        }

        /**
         * A multitrack metadata for video
         */
        @Serializable
        data class VideoTrackIdInfo(
            val videocodecid: Double? = null,
            val width: Double? = null,
            val height: Double? = null,
            val videodatarate: Double? = null
        )

        /**
         * A multitrack metadata for audio
         */
        @Serializable
        data class AudioTrackIdInfo(
            val audiocodecid: Double? = null,
            val audiodatarate: Double? = null,
            val channels: Double? = null,
            val audiosamplerate: Double? = null,
            val audiosamplesize: Double? = null
        )
    }
}