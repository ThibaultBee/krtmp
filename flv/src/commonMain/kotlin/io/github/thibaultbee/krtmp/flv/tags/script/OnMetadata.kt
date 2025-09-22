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
package io.github.thibaultbee.krtmp.flv.tags.script

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.amf.elements.containers.amfEcmaArrayOf
import io.github.thibaultbee.krtmp.amf.elements.containers.amfObjectOf
import io.github.thibaultbee.krtmp.flv.config.FLVAudioConfig
import io.github.thibaultbee.krtmp.flv.config.FLVVideoConfig
import io.github.thibaultbee.krtmp.flv.config.SoundType
import kotlinx.serialization.Serializable

/**
 * Creates a [Metadata] from an ECMA array
 *
 * @param value ECMA array of the [Metadata]
 * @return The onMetaData data
 */
fun Metadata(value: AmfElement) =
    Metadata.decode(value as AmfEcmaArray)

/**
 * Creates a [Metadata] from multiple audio and video configurations
 *
 * @param audioConfigs The audio configurations
 * @param videoConfigs The video configurations
 */
fun Metadata(audioConfigs: Map<Int, FLVAudioConfig>, videoConfigs: Map<Int, FLVVideoConfig>) =
    Metadata.fromConfigs(audioConfigs, videoConfigs)

/**
 * Creates a [Metadata] from audio and video configurations
 *
 * @param audioConfig The audio configuration
 * @param videoConfig The video configuration
 * @return The onMetaData data
 */
fun Metadata(audioConfig: FLVAudioConfig?, videoConfig: FLVVideoConfig?) =
    Metadata.fromConfigs(audioConfig, videoConfig)

/**
 * The Metadata data
 * @param duration The duration of the media in seconds
 * @param audiocodecid The audio codec ID, see [FLVAudioConfig.metadataType]
 */
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
            (Amf.encodeToAmfElement(
                serializer(),
                this
            ) as AmfObject)
        )
    }

    override fun toString(): String {
        return "Metadata(duration=$duration, audiocodecid=$audiocodecid, audiodatarate=$audiodatarate, " +
                "audiosamplerate=$audiosamplerate, audiosamplesize=$audiosamplesize, stereo=$stereo, " +
                "videocodecid=$videocodecid, videodatarate=$videodatarate, width=$width, height=$height, " +
                "framerate=$framerate, audioTrackIdInfoMap=$audioTrackIdInfoMap, videoTrackIdInfoMap=$videoTrackIdInfoMap)"
    }

    companion object {
        internal fun decode(array: AmfEcmaArray): Metadata {
            return Amf.decodeFromAmfElement(serializer(), amfObjectOf(array))
        }

        internal fun fromConfigs(
            audioConfig: FLVAudioConfig?,
            videoConfig: FLVVideoConfig?
        ): Metadata {
            require(audioConfig != null || videoConfig != null) {
                "Either audio or video config must be provided"
            }
            val audioConfigs = audioConfig?.let { mapOf(0 to it) } ?: emptyMap()
            val videoConfigs = videoConfig?.let { mapOf(0 to it) } ?: emptyMap()
            return fromConfigs(audioConfigs, videoConfigs)
        }

        internal fun fromConfigs(
            audioConfigs: Map<Int, FLVAudioConfig>,
            videoConfigs: Map<Int, FLVVideoConfig>
        ): Metadata {
            require((audioConfigs.isNotEmpty()) || (videoConfigs.isNotEmpty())) {
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