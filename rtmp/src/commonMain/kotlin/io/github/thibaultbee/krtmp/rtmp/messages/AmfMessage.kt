/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.rtmp.RtmpConfiguration
import io.ktor.utils.io.ByteWriteChannel

interface AmfMessage {
    fun createMessage(amfVersion: AmfVersion): Message

    suspend fun write(
        writeChannel: ByteWriteChannel,
        amfVersion: AmfVersion,
        chunkSize: Int = RtmpConfiguration.DEFAULT_CHUNK_SIZE,
        previousMessage: Message? = null
    ): Int
}