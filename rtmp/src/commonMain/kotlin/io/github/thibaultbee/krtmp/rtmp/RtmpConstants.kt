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

object RtmpConstants {
    /**
     * The minimum chunk size used for RTMP connections.
     */
    const val MIN_CHUNK_SIZE = 128 // bytes

    /**
     * The maximum chunk size used for RTMP connections.
     */
    const val MAX_CHUNK_SIZE = 65536 // bytes

    /**
     * The range of valid chunk sizes for RTMP connections.
     */
    internal val chunkSizeRange = MIN_CHUNK_SIZE..MAX_CHUNK_SIZE

    /**
     * The default chunk size used for RTMP connections.
     * This value is used when the client does not specify a chunk size during the handshake.
     * The default value is 128 bytes, which is the minimum chunk size allowed by the RTMP protocol.
     */
    const val DEFAULT_CHUNK_SIZE = MIN_CHUNK_SIZE // bytes
}