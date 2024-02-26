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
package io.github.thibaultbee.krtmp.rtmp.extensions

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readFully
import kotlinx.io.Buffer

internal suspend fun ByteReadChannel.readInt24() =
    ((readByte().toInt() and 0xff shl 16) or (readByte().toInt() and 0xff shl 8) or (readByte().toInt() and 0xff))

internal suspend fun ByteReadChannel.readFully(buffer: Buffer, size: Int) {
    val byteArray = ByteArray(size)
    readFully(byteArray)
    buffer.write(byteArray, 0, size)
}
