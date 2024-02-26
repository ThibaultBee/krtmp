package io.github.thibaultbee.krtmp.flv.models.tags

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvAudioConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvVideoConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundType
import io.github.thibaultbee.krtmp.flv.models.packets.FlvTagPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class OnMetadataTest {
    @Test
    fun `test write onMetadata from config for video only`() {
        val expected = Resource("tags/onMetadata/onMetadataForLegacyVideo").toByteArray()

        val configs = listOf(
            FlvVideoConfig(
                mimeType = MimeType.VIDEO_AVC,
                bitrateBps = 2000000,
                width = 640,
                height = 480,
                frameRate = 30
            ),
        )
        val onMetadata = OnMetadata(0, configs)

        assertContentEquals(expected, FlvTagPacket(onMetadata).readByteArray())
    }

    @Test
    fun `test write onMetadata from config for audio only`() {
        val expected = Resource("tags/onMetadata/onMetadataForLegacyAudio").toByteArray()

        val configs = listOf(
            FlvAudioConfig(
                mimeType = MimeType.AUDIO_AAC,
                bitrateBps = 128000,
                soundRate = SoundRate.F_44100HZ,
                soundSize = SoundSize.S_16BITS,
                soundType = SoundType.STEREO
            ),
        )
        val onMetadata = OnMetadata(0, configs)

        assertContentEquals(expected, FlvTagPacket(onMetadata).readByteArray())
    }

    @Test
    fun `test write onMetadata for video only`() {
        val expected = Resource("tags/onMetadata/onMetadataForLegacyVideo").toByteArray()

        val metadata = OnMetadata.Metadata(
            videocodecid = 7.0,
            videodatarate = 2000.0,
            width = 640.0,
            height = 480.0,
            framerate = 30.0
        )
        val onMetadata = OnMetadata(0, metadata)

        assertContentEquals(expected, FlvTagPacket(onMetadata).readByteArray())
    }

    @Test
    fun `test read onMetadata for video only`() {
        val expected = OnMetadata.Metadata(
            videocodecid = 7.0,
            videodatarate = 2000.0,
            width = 640.0,
            height = 480.0,
            framerate = 30.0
        )

        val source = Resource("tags/onMetadata/onMetadataForLegacyVideo").toSource()
        val onMetadata = FlvTagPacket.read(source)
        val metadata = (onMetadata as OnMetadata).metadata

        assertEquals(expected.videocodecid, metadata.videocodecid)
        assertEquals(expected.videodatarate, metadata.videodatarate)
        assertEquals(expected.width, metadata.width)
        assertEquals(expected.height, metadata.height)
        assertEquals(expected.framerate, metadata.framerate)
    }
}