package io.github.thibaultbee.krtmp.rtmp.client

import io.github.thibaultbee.krtmp.common.MimeType
import io.github.thibaultbee.krtmp.common.logger.Logger
import io.github.thibaultbee.krtmp.flv.FlvMuxer
import io.github.thibaultbee.krtmp.flv.models.AACFrame
import io.github.thibaultbee.krtmp.flv.models.SizedRawSource
import io.github.thibaultbee.krtmp.flv.models.av.config.FlvAudioConfig
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundRate
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundSize
import io.github.thibaultbee.krtmp.flv.models.av.config.SoundType
import io.github.thibaultbee.krtmp.rtmp.client.publish.RtmpPublishClient
import io.github.thibaultbee.krtmp.rtmp.client.publish.RtmpPublishClientConnectionFactory
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.utils.RtmpClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test

class RtmpClientTest {
    @Test
    fun `test connection for publishing to server`() = runTest {
        val clock = RtmpClock.Default()
        val client = RtmpPublishClientConnectionFactory(RtmpPublishClient.Settings(clock = clock, enableTooLateFrameDrop = true)).create(
            //"rtmp://127.0.0.1/s/32dfd7ef-9ce9-4b18-808e-fb76e6564dee"
            "rtmps://a.rtmps.youtube.com/live2/awp3-7v6s-d2dq-7vzw-66jz"
        )
        client.coroutineContext.invokeOnCompletion { cause ->
            Logger.e("TEST", "Closed cause $cause")
        }
        try {
            client.connect()
            client.createStream()
            client.publish(Command.Publish.Type.LIVE)
            val muxer = client.flvMuxer
            muxer.addStream(
                FlvAudioConfig(
                    MimeType.AUDIO_AAC,
                    1000,
                    SoundRate.F_44100HZ,
                    SoundSize.S_16BITS,
                    SoundType.STEREO
                )
            )
            sendFrames(muxer, clock)
        } catch (e: Exception) {
            Logger.e("TEST", "Exception $e")
        }
        client.close()
    }


    private suspend fun sendFrames(muxer: FlvMuxer, clock: RtmpClock) = withContext(Dispatchers.Default) {
        val job = async {
            for (i in 0 until 5000) {
                muxer.write(AACFrame(SizedRawSource(byteArrayOf(0, 0, 2, 9)), clock.nowInMs, null))
                delay(1000L)
            }
        }
        job.await()
    }

}