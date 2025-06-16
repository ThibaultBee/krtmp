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
package io.github.thibaultbee.krtmp.rtmp.connection

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.rtmp.RtmpConfiguration
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock

/**
 * This class contains configuration for RTMP client.
 *
 * @param writeChunkSize RTMP chunk size in bytes
 * @param writeWindowAcknowledgementSize RTMP acknowledgement window size in bytes
 * @param amfVersion AMF version
 * @param clock Clock used to timestamp RTMP messages. You should use the same clock for your video and audio timestamps.
 * @param enableTooLateFrameDrop enable dropping too late frames. Default is false. It will drop frames if they are are too late if set to true. If enable, make sure frame timestamps are on on the same clock as [clock].
 * @param tooLateFrameDropTimeoutInMs the timeout after which a frame will be dropped (from frame timestamps). Default is 3000ms.
 */
open class RtmpSettings(
    val writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
    val writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
    val amfVersion: AmfVersion = AmfVersion.AMF0,
    val clock: RtmpClock = RtmpClock.Default(),
    val enableTooLateFrameDrop: Boolean = false,
    val tooLateFrameDropTimeoutInMs: Long = DEFAULT_TOO_LATE_FRAME_DROP_TIMEOUT_IN_MS
) {
    /**
     * The default instance of [RtmpSettings]
     */
    companion object Default : RtmpSettings() {
        const val DEFAULT_CHUNK_SIZE = RtmpConfiguration.DEFAULT_CHUNK_SIZE // bytes
        const val DEFAULT_TOO_LATE_FRAME_DROP_TIMEOUT_IN_MS = 2000L // ms
    }
}
