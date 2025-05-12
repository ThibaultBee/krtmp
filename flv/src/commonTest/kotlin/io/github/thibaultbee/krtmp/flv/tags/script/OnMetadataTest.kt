package io.github.thibaultbee.krtmp.flv.tags.script

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.config.FLVAudioConfig
import io.github.thibaultbee.krtmp.flv.config.FLVVideoConfig
import io.github.thibaultbee.krtmp.flv.config.SoundRate
import io.github.thibaultbee.krtmp.flv.config.SoundSize
import io.github.thibaultbee.krtmp.flv.config.SoundType
import io.github.thibaultbee.krtmp.flv.config.VideoMediaType
import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class OnMetadataTest {
    @Test
    fun `test write onMetadata from config for video only`() {
        val expected = Resource("tags/onMetadata/onMetadataForLegacyVideo").toByteArray()

        val onMetadata = OnMetadata(
            audioConfig = null,
            videoConfig = FLVVideoConfig(
                mediaType = VideoMediaType.AVC,
                bitrateBps = 2000000,
                width = 640,
                height = 480,
                frameRate = 30
            )
        )

        assertContentEquals(expected, FLVTag(0, onMetadata).readByteArray())
    }

    @Test
    fun `test write onMetadata from config for audio only`() {
        val expected = Resource("tags/onMetadata/onMetadataForLegacyAudio").toByteArray()

        val onMetadata = OnMetadata(
            audioConfig = FLVAudioConfig(
                mediaType = AudioMediaType.AAC,
                bitrateBps = 128000,
                soundRate = SoundRate.F_44100HZ,
                soundSize = SoundSize.S_16BITS,
                soundType = SoundType.STEREO
            ),
            videoConfig = null
        )

        assertContentEquals(expected, FLVTag(0, onMetadata).readByteArray())
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
        val onMetadata = OnMetadata(metadata)

        assertContentEquals(expected, FLVTag(0, onMetadata).readByteArray())
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
        val flvTag = FLVTag.decode(source, AmfVersion.AMF0)
        val metadata = (flvTag.data as OnMetadata).metadata

        assertEquals(expected.videocodecid, metadata.videocodecid)
        assertEquals(expected.videodatarate, metadata.videodatarate)
        assertEquals(expected.width, metadata.width)
        assertEquals(expected.height, metadata.height)
        assertEquals(expected.framerate, metadata.framerate)
    }

    @Test
    fun `test videocodecid fromConfigs`() {
        var metadata = OnMetadata.Metadata.fromConfigs(
            audioConfig = null,
            videoConfig = FLVVideoConfig(
                mediaType = VideoMediaType.AVC,
                bitrateBps = 2000000,
                width = 640,
                height = 480,
                frameRate = 30
            )
        )
        assertEquals(7.0, metadata.videocodecid)

        metadata = OnMetadata.Metadata.fromConfigs(
            audioConfig = null,
            videoConfig = FLVVideoConfig(
                mediaType = VideoMediaType.AV1,
                bitrateBps = 2000000,
                width = 640,
                height = 480,
                frameRate = 30
            )
        )
        assertEquals(1635135537.0, metadata.videocodecid)
    }
}