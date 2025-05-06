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
package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.amf.elements.containers.AmfEcmaArray
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.flv.models.config.FLVAudioConfig
import io.github.thibaultbee.krtmp.flv.models.config.FLVConfig
import io.github.thibaultbee.krtmp.flv.models.config.FLVVideoConfig
import io.github.thibaultbee.krtmp.flv.models.config.SoundType
import io.github.thibaultbee.krtmp.flv.models.tags.OnMetadata.Metadata
import io.github.thibaultbee.krtmp.flv.models.util.AmfUtil.amf
import kotlinx.serialization.Serializable

/**
 * Creates a onMetaData
 * @param value ECMA array of the [Metadata]
 */
fun OnMetadata(value: AmfEcmaArray) =
    OnMetadata(Metadata.fromArray(value))

/**
 * Creates a onMetaData
 * @param configs List of [FLVConfig] to write metadata from
 */
fun OnMetadata(configs: List<FLVConfig>) =
    OnMetadata(Metadata.fromConfigs(configs))

/**
 * The onMetaData data
 * @param metadata Metadata to write
 */
class OnMetadata(
    val metadata: Metadata,
) :
    ScriptDataObject(
        ON_METADATA,
        metadata.encode()
    ) {

    @Serializable
    class Metadata(
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
        val framerate: Double? = null
    ) {
        fun encode(): AmfEcmaArray {
            return AmfEcmaArray(
                (amf.encodeToAmfElement(
                    serializer(),
                    this
                ) as AmfObject)
            )
        }

        companion object {
            fun fromArray(array: AmfEcmaArray): Metadata {
                return amf.decodeFromAmfElement(serializer(), AmfObject(array))
            }

            fun fromConfigs(configs: List<FLVConfig>): Metadata {
                require(configs.isNotEmpty()) { "Configs list must not be empty" }
                require(configs.size <= 2) { "Configs list must not contain more than 2 elements" }

                val audioConfig = configs.firstOrNull { it is FLVAudioConfig } as FLVAudioConfig?
                val videoConfig = configs.firstOrNull { it is FLVVideoConfig } as FLVVideoConfig?

                return Metadata(
                    duration = 0.0,
                    audiocodecid = audioConfig?.soundFormat?.value?.toDouble(),
                    audiodatarate = audioConfig?.bitrateBps?.div(1000)?.toDouble(), // to Kbps
                    audiosamplerate = audioConfig?.soundRate?.sampleRate?.toDouble(),
                    audiosamplesize = audioConfig?.soundSize?.byteFormat?.numOfBytes?.times(Byte.SIZE_BITS)
                        ?.toDouble(),
                    stereo = audioConfig?.let { (it.soundType == SoundType.STEREO) },
                    videocodecid = videoConfig?.codecID?.value?.toDouble(),
                    videodatarate = videoConfig?.bitrateBps?.div(1000)?.toDouble(), // to Kbps
                    width = videoConfig?.width?.toDouble(),
                    height = videoConfig?.height?.toDouble(),
                    framerate = videoConfig?.frameRate?.toDouble()
                )
            }
        }
    }
}