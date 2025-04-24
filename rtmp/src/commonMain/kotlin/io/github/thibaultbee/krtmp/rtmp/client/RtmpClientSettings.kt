/*
 * Copyright (C) 2024 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.client

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.models.config.AudioMediaType
import io.github.thibaultbee.krtmp.flv.models.config.VideoMediaType
import io.github.thibaultbee.krtmp.rtmp.RtmpConfiguration
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_AUDIO_CODECS
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_FLASH_VER
import io.github.thibaultbee.krtmp.rtmp.messages.Command.Connect.ConnectObject.Companion.DEFAULT_VIDEO_CODECS
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock

/**
 * This class contains configuration for RTMP client.
 *
 * @param writeChunkSize RTMP chunk size in bytes
 * @param writeWindowAcknowledgementSize RTMP acknowledgement window size in bytes
 * @param amfVersion AMF version
 * @param clock Clock used to timestamp RTMP messages. You should use the same clock for your video and audio timestamps.
 */
open class RtmpClientSettings(
    writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
    writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
    amfVersion: AmfVersion = AmfVersion.AMF0,
    clock: RtmpClock = RtmpClock.Default()
) : RtmpConfiguration(
    writeChunkSize,
    writeWindowAcknowledgementSize,
    amfVersion,
    clock
) {
    /**
     * The default instance of [RtmpClientSettings]
     */
    companion object Default : RtmpClientSettings()
}

/**
 * Connect information used by the connect command to RTMP server.
 *
 * @param flashVer Flash version
 * @param audioCodecs List of supported audio codecs
 * @param videoCodecs List of supported video codecs (including extended RTMP codecs)
 */
open class RtmpClientConnectInformation(
    val flashVer: String = DEFAULT_FLASH_VER,
    val audioCodecs: List<AudioMediaType>? = DEFAULT_AUDIO_CODECS,
    val videoCodecs: List<VideoMediaType>? = DEFAULT_VIDEO_CODECS,
) {
    /**
     * The default instance of [RtmpClientConnectInformation]
     */
    companion object Default : RtmpClientConnectInformation()
}
