package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvAudioConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundType
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AudioTagTest {
    @Test
    fun `test write aac raw`() {
        val expected = Resource("tags/audio/aac/raw/muxed").toByteArray()

        val raw = Resource("tags/audio/aac/raw/raw").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }
        val config = FlvAudioConfig(
            mimeType = MimeType.AUDIO_AAC,
            bitrateBps = 128000,
            soundRate = SoundRate.F_44100HZ,
            soundSize = SoundSize.S_16BITS,
            soundType = SoundType.STEREO
        )

        val audioTag =
            AudioTag(81, rawBuffer, rawBuffer.size.toInt(), config, AACPacketType.RAW)

        assertContentEquals(expected, FlvTagPacket(audioTag).readByteArray())
    }

    @Test
    fun `test write aac body`() {
        val expected = Resource("tags/audio/aac/raw/muxedbody").toByteArray()

        val raw = Resource("tags/audio/aac/raw/raw").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }
        val config = FlvAudioConfig(
            mimeType = MimeType.AUDIO_AAC,
            bitrateBps = 128000,
            soundRate = SoundRate.F_44100HZ,
            soundSize = SoundSize.S_16BITS,
            soundType = SoundType.STEREO
        )

        val audioTag =
            AudioTag(81, rawBuffer, rawBuffer.size.toInt(), config, AACPacketType.RAW)

        assertContentEquals(expected, FlvTagPacket(audioTag).bodyOutputPacket.readByteArray())
    }

    @Test
    fun `test write aac sequence header`() {
        val expected = Resource("tags/audio/aac/sequence/muxed").toByteArray()

        val sequenceHeader = Resource("tags/audio/aac/sequence/sequence").toByteArray()
        val sequenceHeaderBuffer = Buffer().apply {
            write(sequenceHeader)
        }
        val config = FlvAudioConfig(
            mimeType = MimeType.AUDIO_AAC,
            bitrateBps = 128000,
            soundRate = SoundRate.F_44100HZ,
            soundSize = SoundSize.S_16BITS,
            soundType = SoundType.STEREO
        )

        val audioTag = AudioTag(
            81,
            sequenceHeaderBuffer,
            sequenceHeaderBuffer.size.toInt(),
            config,
            AACPacketType.SEQUENCE_HEADER
        )

        assertContentEquals(expected, FlvTagPacket(audioTag).readByteArray())
    }

    @Test
    fun `test read aac raw`() {
        val expected = Resource("tags/audio/aac/raw/raw").toByteArray()

        val muxed = Resource("tags/audio/aac/raw/muxed").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val audioTag = FlvTagPacket.read(muxBuffer) as AudioTag

        assertEquals(SoundFormat.AAC, audioTag.tagHeader.soundFormat)
        assertEquals(SoundRate.F_44100HZ, audioTag.tagHeader.soundRate)
        assertEquals(SoundSize.S_16BITS, audioTag.tagHeader.soundSize)
        assertEquals(SoundType.STEREO, audioTag.tagHeader.soundType)
        assertEquals(AACPacketType.RAW, audioTag.tagHeader.aacPacketType)

        val actualBuffer = Buffer().apply {
            write(audioTag.source, audioTag.sourceSize.toLong())
        }
        assertContentEquals(expected, actualBuffer.readByteArray())
    }

    @Test
    fun `test read aac sequence`() {
        val expected = Resource("tags/audio/aac/sequence/sequence").toByteArray()

        val muxed = Resource("tags/audio/aac/sequence/muxed").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val audioTag = FlvTagPacket.read(muxBuffer) as AudioTag

        assertEquals(SoundFormat.AAC, audioTag.tagHeader.soundFormat)
        assertEquals(SoundRate.F_44100HZ, audioTag.tagHeader.soundRate)
        assertEquals(SoundSize.S_16BITS, audioTag.tagHeader.soundSize)
        assertEquals(SoundType.STEREO, audioTag.tagHeader.soundType)
        assertEquals(AACPacketType.SEQUENCE_HEADER, audioTag.tagHeader.aacPacketType)

        val actualBuffer = Buffer().apply {
            write(audioTag.source, audioTag.sourceSize.toLong())
        }
        assertContentEquals(expected, actualBuffer.readByteArray())
    }
}