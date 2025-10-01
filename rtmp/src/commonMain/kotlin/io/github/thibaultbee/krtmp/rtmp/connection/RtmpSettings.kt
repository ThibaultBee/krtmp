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
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock

/**
 * This class contains configuration for RTMP client.
 *
 * @param writeWindowAcknowledgementSize RTMP acknowledgement window size in bytes
 * @param amfVersion AMF version
 * @param clock Clock used to timestamp RTMP messages. You should use the same clock for your video and audio timestamps.
 * @param tooLateFrameDropTimeoutInMs the timeout after which a frame will be dropped (from frame timestamps). Make sure frame timestamps are on on the same clock as [clock]. If null is provided, frames will never be dropped. Default is null
 */
open class RtmpSettings(
    val writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
    val amfVersion: AmfVersion = AmfVersion.AMF0,
    val clock: RtmpClock = RtmpClock.Default(),
    val tooLateFrameDropTimeoutInMs: Long? = null
) {
    /**
     * The default instance of [RtmpSettings]
     */
    companion object Default : RtmpSettings()
}
