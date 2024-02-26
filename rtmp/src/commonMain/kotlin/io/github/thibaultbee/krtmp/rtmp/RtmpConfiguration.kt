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
package io.github.thibaultbee.krtmp.rtmp

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.rtmp.utils.RtmpClock

/**
 * This class contains configuration for RTMP connection.
 *
 * @param writeChunkSize RTMP chunk size in bytes
 * @param writeWindowAcknowledgementSize RTMP acknowledgement window size in bytes
 * @param amfVersion AMF version
 * @param clock Clock used to timestamp RTMP messages. You should use the same clock for your video and audio timestamps.
 */
abstract class RtmpConfiguration(
    val writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
    val writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
    val amfVersion: AmfVersion = AmfVersion.AMF0,
    val clock: RtmpClock = RtmpClock.Default(),
) {
    companion object {
        const val DEFAULT_CHUNK_SIZE = 128 // bytes
    }
}