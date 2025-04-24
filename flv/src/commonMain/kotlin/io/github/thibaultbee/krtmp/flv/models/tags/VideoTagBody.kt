/*
 * Copyright (C) 2025 Thibault B.
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

import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Interface for video tag body.
 */
interface IVideoTagBody {
    val size: Int
    fun encode(output: Sink)
}

/**
 * Default video tag body.
 *
 */
class DefaultVideoTagBody(
    val data: RawSource,
    val dataSize: Int
) : IVideoTagBody {
    override val size = dataSize

    override fun encode(output: Sink) {
        output.write(data, dataSize.toLong())
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): DefaultVideoTagBody {
            return DefaultVideoTagBody(source, sourceSize)
        }
    }
}

class EmptyVideoTagBody : IVideoTagBody {
    override val size = 0
    override fun encode(output: Sink) = Unit  // End of sequence does not have a body
}

class HEVCVideoTagBody(
    private val packetType: PacketType,
    private val compositionTime: Int, // 24 bits
    val data: RawSource,
    val dataSize: Int
) : IVideoTagBody {
    override val size = if (packetType == PacketType.CODED_FRAMES) {
        4 + dataSize // composition time + dataSize
    } else {
        dataSize
    }

    override fun encode(output: Sink) {
        if (packetType == PacketType.CODED_FRAMES) {
            output.writeInt24(compositionTime)
        }
        output.write(data, dataSize.toLong())
    }

    companion object {
        fun decode(
            source: Source,
            sourceSize: Int,
            packetType: PacketType
        ): HEVCVideoTagBody {
            var compositionTime = 0
            var remainingSize = sourceSize
            if (packetType == PacketType.CODED_FRAMES) {
                remainingSize -= 3
                compositionTime = source.readInt24()
            }
            return HEVCVideoTagBody(packetType, compositionTime, source, sourceSize)
        }
    }
}
