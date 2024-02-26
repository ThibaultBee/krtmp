package io.github.thibaultbee.krtmp.flv.models.av.utils.aac

import io.github.thibaultbee.krtmp.flv.Resource
import io.github.thibaultbee.krtmp.flv.models.av.utils.AudioObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class AACTest {

    @Test
    fun `test ADTS with payload size of 378 bytes`() {
        val adtsSource = Resource("audio/aac/adts/adts-378bytes").toSource()

        val adts = AAC.ADTS(adtsSource)

        assertEquals(true, adts.protectionAbsent)
        assertEquals(AudioObjectType.AAC_LC, adts.profile)
        assertEquals(44100, adts.sampleRate)
        assertEquals(ADTS.ChannelConfiguration.CHANNEL_2, adts.channelConfiguration)
        assertEquals(371, adts.frameLength)
    }

    @Test
    fun `test ADTS with payload size of 516 bytes`() {
        val adtsSource = Resource("audio/aac/adts/adts-516bytes").toSource()

        val adts = AAC.ADTS(adtsSource)

        assertEquals(true, adts.protectionAbsent)
        assertEquals(AudioObjectType.AAC_LC, adts.profile)
        assertEquals(44100, adts.sampleRate)
        assertEquals(ADTS.ChannelConfiguration.CHANNEL_2, adts.channelConfiguration)
        assertEquals(509, adts.frameLength)
    }
}