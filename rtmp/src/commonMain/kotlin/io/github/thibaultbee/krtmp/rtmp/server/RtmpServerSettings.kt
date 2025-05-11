/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.server

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.rtmp.server.util.DefaultStreamIdProvider
import io.github.thibaultbee.krtmp.rtmp.server.util.IStreamIdProvider
import io.github.thibaultbee.krtmp.rtmp.connection.RtmpSettings
import io.github.thibaultbee.krtmp.rtmp.util.RtmpClock

/**
 * RTMP server settings.
 */
open class RtmpServerSettings(
    writeChunkSize: Int = DEFAULT_CHUNK_SIZE,
    writeWindowAcknowledgementSize: Int = Int.MAX_VALUE,
    amfVersion: AmfVersion = AmfVersion.AMF0,
    clock: RtmpClock = RtmpClock.Default(),
    val streamIdProvider: IStreamIdProvider = DefaultStreamIdProvider()
) : RtmpSettings(writeChunkSize, writeWindowAcknowledgementSize, amfVersion, clock, false, 0L) {
    /**
     * The default instance of [RtmpServerSettings]
     */
    companion object Default : RtmpServerSettings()
}