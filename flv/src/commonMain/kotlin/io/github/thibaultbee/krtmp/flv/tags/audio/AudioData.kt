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
import io.github.thibaultbee.krtmp.amf.internal.utils.readInt24
import io.github.thibaultbee.krtmp.amf.internal.utils.writeInt24
import io.github.thibaultbee.krtmp.flv.config.AudioFourCC
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.tags.FLVData
import io.github.thibaultbee.krtmp.flv.tags.ModEx
import io.github.thibaultbee.krtmp.flv.tags.ModExCodec
import io.github.thibaultbee.krtmp.flv.tags.ModExEncoder
import io.github.thibaultbee.krtmp.flv.tags.MultitrackType
import io.github.thibaultbee.krtmp.flv.util.SinkEncoder
import io.github.thibaultbee.krtmp.flv.util.WithValue
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
                val remainingSize = sourceSize - 1
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = RawAudioTagBody.decode(source, remainingSize)
                LegacyAudioData(soundFormat, soundRate, soundSize, soundType, body, aacPacketType)
            } else {
                require(!isEncrypted) { "Encrypted audio is not supported." }
                val body = RawAudioTagBody.decode(source, sourceSize)
                LegacyAudioData(soundFormat, soundRate, soundSize, soundType, body)
            }
        }
    }
}

/**
 * Creates an [ExtendedAudioData] from the given [AudioPacketType], [AudioFourCC], and [SingleAudioTagBody].
 *
 * @param packetType the [AudioPacketType] of the audio data
 * @param fourCC the [AudioFourCC] of the audio data
 * @param body the [SingleAudioTagBody] of the audio data
 */
fun ExtendedAudioData(
    packetType: AudioPacketType,
    fourCC: AudioFourCC,
    body: SingleAudioTagBody
) = ExtendedAudioData(
    packetDescriptor = ExtendedAudioData.SingleAudioDataDescriptor(
        packetType = packetType,
        fourCC = fourCC,
        body = body
    )
)

class ExtendedAudioData internal constructor(
    val packetDescriptor: AudioDataDescriptor,
    val modExs: Set<ModEx<AudioPacketModExType, *>> = emptySet()
) : AudioData(
    SoundFormat.EX_HEADER, if (modExs.isEmpty()) {
        packetDescriptor.packetType.value
    } else {
        AudioPacketType.MOD_EX.value
    }.toInt(), packetDescriptor.body
) {
    override val size =
        super.size + packetDescriptor.size + AudioModExCodec.encoder.getSize(modExs)
    val packetType = packetDescriptor.packetType

    override fun encodeHeaderImpl(output: Sink) {
        AudioModExCodec.encoder.encode(output, modExs, packetType.value)
        packetDescriptor.encode(output)
    }

    override fun toString(): String {
        return "ExtendedAudioData(packetType=$packetType, packetDescriptor=$packetDescriptor, modExs=$modExs)"
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
            var nextPacketType = packetType
            val modExs = if (packetType == AudioPacketType.MOD_EX) {
                val modExDatas = AudioModExCodec.encoder.decode(source)
                nextPacketType = AudioPacketType.entryOf(modExDatas.nextPacketType)
                modExDatas.modExs
            } else {
                emptySet()
            }

            val remainingSize = sourceSize - AudioModExCodec.encoder.getSize(modExs)
            val packetDescriptor = when (packetType) {
                AudioPacketType.MULTITRACK -> MultitrackAudioDataDescriptor.decode(
                    source,
                    remainingSize
                )

                else -> SingleAudioDataDescriptor.decode(nextPacketType, source, remainingSize)
            }
            return ExtendedAudioData(packetDescriptor)
        }
    }

    interface OneAudioCodec {
        val fourCC: AudioFourCC
    }

    interface AudioDataDescriptor : SinkEncoder {
        val packetType: AudioPacketType
        val body: AudioTagBody
    }

    class SingleAudioDataDescriptor internal constructor(
        override val packetType: AudioPacketType,
        val fourCC: AudioFourCC,
        override val body: SingleAudioTagBody
    ) : AudioDataDescriptor {
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
            ): SingleAudioDataDescriptor {
                val fourCC = AudioFourCC.codeOf(source.readInt())
                val body = RawAudioTagBody.decode(source, sourceSize - 4)
                return SingleAudioDataDescriptor(
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
    sealed class MultitrackAudioDataDescriptor(
        val multitrackType: MultitrackType,
        val framePacketType: AudioPacketType
    ) :
        AudioDataDescriptor {
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

        companion object {
            fun decode(
                source: Source,
                sourceSize: Int
            ): MultitrackAudioDataDescriptor {
                val byte = source.readByte()
                val multitrackType =
                    MultitrackType.entryOf(((byte and 0xF0.toByte()) shr 4).toByte())
                val framePacketType = AudioPacketType.entryOf(byte and 0x0F.toByte())
                val remainingSize = sourceSize - 1
                return when (multitrackType) {
                    MultitrackType.ONE_TRACK -> OneTrackAudioDataDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK -> ManyTrackAudioDataDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )

                    MultitrackType.MANY_TRACK_MANY_CODEC -> ManyTrackManyCodecAudioDataDescriptor.decode(
                        framePacketType,
                        source,
                        remainingSize
                    )
                }
            }
        }

        class OneTrackAudioDataDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val fourCC: AudioFourCC,
            override val body: OneTrackAudioTagBody
        ) : MultitrackAudioDataDescriptor(MultitrackType.ONE_TRACK, framePacketType),
            OneAudioCodec {
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
                ): OneTrackAudioDataDescriptor {
                    val fourCC = AudioFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        OneTrackAudioTagBody.decode(source, remainingSize)
                    return OneTrackAudioDataDescriptor(packetType, fourCC, body)
                }
            }
        }

        class ManyTrackAudioDataDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val fourCC: AudioFourCC,
            override val body: ManyTrackOneCodecAudioTagBody
        ) : MultitrackAudioDataDescriptor(MultitrackType.MANY_TRACK, framePacketType),
            OneAudioCodec {
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
                ): ManyTrackAudioDataDescriptor {
                    val fourCC = AudioFourCC.codeOf(source.readInt())
                    val remainingSize = sourceSize - 4
                    val body =
                        ManyTrackOneCodecAudioTagBody.decode(
                            source,
                            remainingSize
                        )
                    return ManyTrackAudioDataDescriptor(packetType, fourCC, body)
                }
            }
        }

        class ManyTrackManyCodecAudioDataDescriptor internal constructor(
            framePacketType: AudioPacketType,
            override val body: ManyTrackManyCodecAudioTagBody
        ) : MultitrackAudioDataDescriptor(
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
                ): ManyTrackManyCodecAudioDataDescriptor {
                    val body = ManyTrackManyCodecAudioTagBody.decode(source, sourceSize)
                    return ManyTrackManyCodecAudioDataDescriptor(
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
        7
    );

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AudioPacketType value: $value"
            )
    }
}

enum class AudioPacketModExType(override val value: Byte) : WithValue<Byte> {
    TIMESTAMP_OFFSET_NANO(0);

    companion object {
        fun entryOf(value: Byte) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException(
                "Invalid AudioPacketModExType value: $value"
            )
    }
}

sealed class AudioModEx<T>(type: AudioPacketModExType, value: T) :
    ModEx<AudioPacketModExType, T>(type, value) {
    override fun toString(): String {
        return "AudioModEx(type=$type, value=$value)"
    }

    class TimestampOffsetNano(value: Int) : AudioModEx<Int>(
        AudioPacketModExType.TIMESTAMP_OFFSET_NANO,
        value
    )
}

internal sealed class AudioModExCodec<T> : ModExCodec<AudioPacketModExType, T> {
    object timestampOffsetNano : AudioModExCodec<Int>() {
        override val type = AudioPacketModExType.TIMESTAMP_OFFSET_NANO
        override val size = 3
        override fun encode(output: Sink, value: Int) {
            output.writeInt24(value)
        }

        override fun decode(source: Source): AudioModEx.TimestampOffsetNano {
            return AudioModEx.TimestampOffsetNano(source.readInt24())
        }
    }

    companion object {
        private val codecs =
            setOf(timestampOffsetNano)

        internal fun codecOf(type: AudioPacketModExType) =
            codecs.firstOrNull { it.type == type }
                ?: throw IllegalArgumentException("Invalid AudioModExCodec type: $type")

        internal val encoder = ModExEncoder(codecs, AudioPacketType.MOD_EX.value)
    }
}