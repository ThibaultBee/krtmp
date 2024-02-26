package io.github.thibaultbee.krtmp.flv.tags.audio

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.config.SoundFormat
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.readByteArray
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LegacyAudioDataTest {
    @Test
    fun `test write aac raw tag`() {
        val expected = Resource("tags/audio/aac/raw/tag").toByteArray()

        val raw = Resource("tags/audio/aac/raw/raw").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val audioTagBody = RawAudioTagBody(rawBuffer, rawBuffer.size.toInt())
        val audioData =
            LegacyAudioData(
                soundFormat = SoundFormat.AAC,
                soundRate = SoundRate.F_44100HZ,
                soundSize = SoundSize.S_16BITS,
                soundType = SoundType.STEREO,
                aacPacketType = AACPacketType.RAW,
                body = audioTagBody
            )

        assertContentEquals(expected, FLVTag(81, audioData).readByteArray())
    }

    @Test
    fun `test write aac data`() {
        val expected = Resource("tags/audio/aac/raw/data").toByteArray()

        val raw = Resource("tags/audio/aac/raw/raw").toByteArray()
        val rawBuffer = Buffer().apply {
            write(raw)
        }

        val audioTagBody = RawAudioTagBody(rawBuffer, rawBuffer.size.toInt())
        val audioData =
            LegacyAudioData(
                soundFormat = SoundFormat.AAC,
                soundRate = SoundRate.F_44100HZ,
                soundSize = SoundSize.S_16BITS,
                soundType = SoundType.STEREO,
                aacPacketType = AACPacketType.RAW,
                body = audioTagBody
            )

        assertContentEquals(expected, audioData.readByteArray())
    }

    @Test
    fun `test write aac sequence header tag`() {
        val expected = Resource("tags/audio/aac/sequence/tag").toByteArray()

        val sequenceHeader = Resource("tags/audio/aac/sequence/sequence").toByteArray()
        val sequenceHeaderBuffer = Buffer().apply {
            write(sequenceHeader)
        }

        val audioTagBody =
            RawAudioTagBody(sequenceHeaderBuffer, sequenceHeaderBuffer.size.toInt())
        val audioData =
            LegacyAudioData(
                soundFormat = SoundFormat.AAC,
                soundRate = SoundRate.F_44100HZ,
                soundSize = SoundSize.S_16BITS,
                soundType = SoundType.STEREO,
                aacPacketType = AACPacketType.SEQUENCE_HEADER,
                body = audioTagBody
            )

        assertContentEquals(expected, FLVTag(81, audioData).readByteArray())
    }

    @Test
    fun `test read aac raw`() {
        val expected = Resource("tags/audio/aac/raw/raw").toByteArray()

        val muxed = Resource("tags/audio/aac/raw/tag").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val audioTag = FLVTag.decode(muxBuffer)
        val audioData = audioTag.data as LegacyAudioData

        assertEquals(SoundFormat.AAC, audioData.soundFormat)
        assertEquals(SoundRate.F_44100HZ, audioData.soundRate)
        assertEquals(SoundSize.S_16BITS, audioData.soundSize)
        assertEquals(SoundType.STEREO, audioData.soundType)
        assertEquals(AACPacketType.RAW, audioData.aacPacketType)

        val body = audioData.body as RawAudioTagBody
        val actual = Buffer().apply {
            write(body.data, body.dataSize.toLong())
        }

        assertContentEquals(expected, actual.readByteArray())
    }

    @Test
    fun `test read aac sequence`() {
        val expected = Resource("tags/audio/aac/sequence/sequence").toByteArray()

        val muxed = Resource("tags/audio/aac/sequence/tag").toByteArray()
        val muxBuffer = Buffer().apply {
            write(muxed)
        }

        val audioTag = FLVTag.decode(muxBuffer)
        val audioData = audioTag.data as LegacyAudioData

        assertEquals(SoundFormat.AAC, audioData.soundFormat)
        assertEquals(SoundRate.F_44100HZ, audioData.soundRate)
        assertEquals(SoundSize.S_16BITS, audioData.soundSize)
        assertEquals(SoundType.STEREO, audioData.soundType)
        assertEquals(AACPacketType.SEQUENCE_HEADER, audioData.aacPacketType)

        val body = audioData.body as RawAudioTagBody
        val actual = Buffer().apply {
            write(body.data, body.dataSize.toLong())
        }

        assertContentEquals(expected, actual.readByteArray())
    }
}