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
package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.MultitrackType
import io.github.thibaultbee.krtmp.flv.tags.SinkEncoder
import io.github.thibaultbee.krtmp.flv.util.extensions.shl
import io.github.thibaultbee.krtmp.flv.util.extensions.shr
import io.github.thibaultbee.krtmp.flv.util.extensions.writeByte
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlin.experimental.and

/**
 * Represents audio data in legacy FLV format.
 */
class LegacyAudioData(
    soundFormat: SoundFormat,
    val soundRate: SoundRate,
    val soundSize: SoundSize,
    val soundType: SoundType,
    body: RawAudioTagBody,
    val aacPacketType: AACPacketType? = null,
) : AudioData(
    soundFormat,
    (soundRate.value.toInt() shl 2) or (soundSize.value.toInt() shl 1) or (soundType.value).toInt(),
    body
) {
    override val size = super.size + if (soundFormat == SoundFormat.AAC) 1 else 0

    override fun encodeHeaderImpl(output: Sink) {
        if (soundFormat == SoundFormat.AAC) {
            output.writeByte(aacPacketType!!.value)
        }
    }

    override fun toString(): String {
        return "LegacyAudioData(soundFormat=$soundFormat, soundRate=$soundRate, soundSize=$soundSize, soundType=$soundType, body=$body)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): LegacyAudioData {
            val byte = source.readByte()
            val soundFormat = SoundFormat.entryOf(((byte.toInt() and 0xF0) shr 4).toByte())
            val soundRate = SoundRate.entryOf(((byte.toInt() and 0x0C) shr 2).toByte())
            val soundSize = SoundSize.entryOf(((byte.toInt() and 0x02) shr 1).toByte())
            val soundType = SoundType.entryOf((byte.toInt() and 0x01).toByte())
            return decode(
                soundFormat, soundRate, soundSize, soundType, source, sourceSize - 1, isEncrypted
            )
        }

        internal fun decode(
            soundFormat: SoundFormat,
            soundRate: SoundRate,
            soundSize: SoundSize,
            soundType: SoundType,
            source: Source,
            sourceSize: Int,
            isEncrypted: Boolean
        ): LegacyAudioData {
            return if (soundFormat == SoundFormat.AAC) {
                val aacPacketType = AACPacketType.entryOf(source.readByte())
                val remainingSize = sourceSize - 2
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = RawAudioTagBody.decode(source, remainingSize)
                LegacyAudioData(soundFormat, soundRate, soundSize, soundType, body, aacPacketType)
            } else {
                val remainingSize = sourceSize - 1
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = RawAudioTagBody.decode(source, remainingSize)
                LegacyAudioData(soundFormat, soundRate, soundSize, soundType, body)
            }
        }
    }
}

class ExtendedAudioData(
    val packetDescriptor: AudioPacketDescriptor
) : AudioData(
    SoundFormat.EX_HEADER, packetDescriptor.packetType.value.toInt(), packetDescriptor.body
) {
    override val size = super.size + packetDescriptor.size
    val packetType = packetDescriptor.packetType
    
    override fun encodeHeaderImpl(output: Sink) {
        packetDescriptor.encode(output)
    }

    override fun toString(): String {
        return "ExtendedAudioData(packetType=${packetType}, packetDescriptor=$packetDescriptor)"
    }

    companion object {
        fun decode(source: Source, sourceSize: Int): ExtendedAudioData {
            val byte = source.readByte()
            val soundFormat = SoundFormat.entryOf(((byte.toInt() and 0xF0) shr 4).toByte())
            val packetType = AudioPacketType.entryOf((byte.toInt() and 0x0F).toByte())
            return decode(soundFormat, packetType, source, sourceSize - 1)
        }

        fun decode(
            soundFormat: SoundFormat,
            packetType: AudioPacketType,
            source: Source,
            sourceSize: Int
        ): ExtendedAudioData {
            require(soundFormat == SoundFormat.EX_HEADER) { "Invalid sound format: $soundFormat. Only EX_HEADER is supported" }
            val packetDescriptor = when (packetType) {
                AudioPacketType.MULTITRACK -> MultitrackAudioPacketDescriptor.decode(
                    source,
                    sourceSize
                )

                else -> SingleAudioPacketDescriptor.decode(packetType, source, sourceSize)
            }
            return ExtendedAudioData(packetDescriptor)
        }
    }

    interface AudioPacketDescriptor : SinkEncoder {
        val packetType: AudioPacketType
        val body: AudioTagBody
    }

    class SingleAudioPacketDescriptor internal constructor(
        override val packetType: AudioPacketType,
        val fourCC: AudioFourCC,
        override val body: SingleAudioTagBody
    ) : AudioPacketDescriptor {
        override val size = 4

        init {
            require(packetType != AudioPacketType.MULTITRACK) {
                "Invalid packet type for single audio: $packetType. Use MultitrackAudioPacketDescriptor instead."
            }
            require(packetType != AudioPacketType.MOD_EX) {
                "Invalid packet type for single audio: $packetType. MOD_EX is not a valid packet type."
            }
        }

        override fun encode(output: Sink) {
            output.writeInt(fourCC.value.code)
        }

        companion object {
            fun decode(
                packetType: AudioPacketType,
                source: Source,
                sourceSize: Int
            ): SingleAudioPacketDescriptor {
                val fourCC = AudioFourCC.codeOf(source.readInt())
                val body = RawAudioTagBody.decode(source, sourceSize - 4)
                return SingleAudioPacketDescriptor(
                    packetType = packetType,
                    fourCC = fourCC,
                    body = body
                )
            }
        }
    }

    /**
     * The multitrack extended audio packet descriptor.
     */
    sealed class MultitrackAudioPacketDescriptor(
        val multitrackType: MultitrackType,
        val framePacketType: AudioPacketType
    ) :
        AudioPacketDescriptor {
        override val packetType = AudioPacketType.MULTITRACK
        override val size = 1

        init {
            require(framePacketType != AudioPacketType.MULTITRACK) {
                "Invalid packet type for multitrack: $framePacketType."
            }
        }

        abstract fun encodeImpl(output: Sink)

        override fun encode(output: Sink) {
            output.writeByte((multitrackType.value shl 4) or framePacketType.value.toInt())
            encodeImpl(output)
        }

        private interface OneCodec : SinkEncoder {
            val fourCC: AudioFourCC
        }

        companion object {
            fun decode(
                source: Source,
                sourceSize: Int
            ): MultitrackAudioPacketDescriptor {
                val byte = source.readByte()
                val multitrackType =
                    MultitrackType.entryOf(((byte and 0xF0.toByte()) shr 4).toByte())
                val framePacketType = AudioPacketType.entryOf(byte and 0x0F.toByte())
                val remainingSize = sourceSize - 1
                return when (multitrackType) {
                    MultitrackType.ONE_TRACK -> OneTrackAudioPacketDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK -> ManyTrackAudioPacketDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK_MANY_CODEC -> ManyTrackManyCodecAudioPacketDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )
                }
            }
        }

        class OneTrackAudioPacketDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val fourCC: AudioFourCC,
            override val body: OneTrackAudioTagBody
        ) : MultitrackAudioPacketDescriptor(MultitrackType.ONE_TRACK, framePacketType),
            OneCodec {
            override val size = super.size + 4

            override fun encodeImpl(output: Sink) {
                output.writeInt(fourCC.value.code)
            }

            override fun toString(): String {
                return "OneTrackAudioPacketDescriptor(packetType=$framePacketType, fourCC=$fourCC, body=$body)"
            }

            companion object {
                fun decode(
                    packetType: AudioPacketType,
                    source: Source,
                    sourceSize: Int
                ): OneTrackAudioPacketDescriptor {
                    val fourCC = AudioFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        OneTrackAudioTagBody.decode(source, remainingSize)
                    return OneTrackAudioPacketDescriptor(packetType, fourCC, body)
                }
            }
        }

        class ManyTrackAudioPacketDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val fourCC: AudioFourCC,
            override val body: ManyTrackOneCodecAudioTagBody
        ) : MultitrackAudioPacketDescriptor(MultitrackType.MANY_TRACK, framePacketType),
            OneCodec {
            override val size = super.size + 4

            override fun encodeImpl(output: Sink) {
                output.writeInt(fourCC.value.code)
            }

            override fun toString(): String {
                return "ManyTrackAudioPacketDescriptor(packetType=$framePacketType, fourCC=$fourCC, body=$body)"
            }

            companion object {
                fun decode(
                    packetType: AudioPacketType,
                    source: Source,
                    sourceSize: Int
                ): ManyTrackAudioPacketDescriptor {
                    val fourCC = AudioFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        ManyTrackOneCodecAudioTagBody.decode(
                            source,
                            remainingSize
                        )
                    return ManyTrackAudioPacketDescriptor(packetType, fourCC, body)
                }
            }
        }

        class ManyTrackManyCodecAudioPacketDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val body: ManyTrackManyCodecAudioTagBody
        ) : MultitrackAudioPacketDescriptor(
            MultitrackType.MANY_TRACK_MANY_CODEC,
            framePacketType
        ) {
            override val size = super.size

            override fun encodeImpl(output: Sink) = Unit

            override fun toString(): String {
                return "ManyTrackManyCodecAudioPacketDescriptor(framePacketType=$framePacketType, body=$body)"
            }

            companion object {
                fun decode(
                    packetType: AudioPacketType,
                    source: Source,
                    sourceSize: Int
                ): ManyTrackManyCodecAudioPacketDescriptor {
                    val body = ManyTrackManyCodecAudioTagBody.decode(source, sourceSize)
                    return ManyTrackManyCodecAudioPacketDescriptor(
                        packetType,
                        body
                    )
                }
            }
        }
    }
}

sealed class AudioData(
    val soundFormat: SoundFormat, val next4bitsValue: Int, val body: AudioTagBody
) : FLVData {
    open val size = body.size + 1
    override fun getSize(amfVersion: AmfVersion) = size

    abstract fun encodeHeaderImpl(
        output: Sink
    )

    private fun encodeBody(output: Sink) {
        body.encode(output)
    }

    override fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean) {
        output.writeByte(
            ((soundFormat.value shl 4) or // SoundFormat
                    next4bitsValue).toByte()
        )
        encodeHeaderImpl(output)
        encodeBody(output)
    }

    companion object {
        fun decode(source: Source, sourceSize: Int, isEncrypted: Boolean): AudioData {
            val byte = source.readByte()
            val soundFormat = SoundFormat.entryOf(((byte.toInt() and 0xF0) shr 4).toByte())
            val remainingSize = sourceSize - 1
            return if (soundFormat != SoundFormat.EX_HEADER) {
                val soundRate = SoundRate.entryOf(((byte.toInt() and 0x0C) shr 2).toByte())
                val soundSize = SoundSize.entryOf(((byte.toInt() and 0x02) shr 1).toByte())
                val soundType = SoundType.entryOf((byte.toInt() and 0x01).toByte())
                LegacyAudioData.decode(
                    soundFormat,
                    soundRate,
                    soundSize,
                    soundType,
                    source,
                    remainingSize,
                    isEncrypted
                )
            } else {
                val packetType = AudioPacketType.entryOf((byte.toInt() and 0x0F).toByte())
                ExtendedAudioData.decode(soundFormat, packetType, source, remainingSize)
            }
        }
    }
}

enum class AACPacketType(val value: Byte) {
    SEQUENCE_HEADER(0), RAW(1);

    companion object {
        fun entryOf(value: Byte) = entries.first { it.value == value }
    }
}

enum class AudioPacketType(val value: Byte) {
    SEQUENCE_START(0), CODED_FRAME(1), SEQUENCE_END(2), MULTICHANNEL_CONFIG(4), MULTITRACK(5), MOD_EX(
        6
    );

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AudioPacketType value: $value"
            )
    }
}

enum class AudioChannelOrder(val value: Byte) {
    /**
     * Only the channel count is specified
     */
    UNSPECIFIED(0),

    /**
     * The native channel order
     */
    NATIVE(1),

    /**
     * The channel order does not correspond to any predefined order and is stored as an explicit map.
     */
    CUSTOM(2);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AudioChannelOrder value: $value"
            )
    }
}