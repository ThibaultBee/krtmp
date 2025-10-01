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
package io.github.thibaultbee.krtmp.rtmp.util.sockets

import io.github.thibaultbee.krtmp.rtmp.extensions.write
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.createChunks
import io.ktor.http.URLBuilder
import io.ktor.network.sockets.ASocket
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope

/**
 * Abstraction over a socket connection.
 */
internal interface ISocket : CoroutineScope, ASocket, WritableMessageSocket, ReadableMessageSocket {
    val urlBuilder: URLBuilder

    val isClosed: Boolean
    val totalBytesRead: Long
    val totalBytesWritten: Long

    suspend fun write(
        length: Long, block: suspend ByteWriteChannel.() -> Unit
    )

    override suspend fun write(message: Message, writeChunkSize: Int, previousMessage: Message?) {
        val chunks = message.createChunks(writeChunkSize, previousMessage)
        val length = chunks.sumOf { it.size.toLong() }
        write(length) {
            chunks.write(this)
        }
    }

    suspend fun <T> read(block: suspend ByteReadChannel.() -> T): T

    override suspend fun read(readChunkSize: Int, getPreviousMessage: (Int) -> Message?): Message {
        return read {
            Message.read(this, readChunkSize) { chunkStreamId ->
                getPreviousMessage(chunkStreamId)
            }
        }
    }
}

/**
 * Abstraction over a writable socket connection.
 */
internal interface WritableMessageSocket : ASocket, CoroutineScope {
    suspend fun write(message: Message, writeChunkSize: Int, previousMessage: Message?)
}

/**
 * Abstraction over a readable socket connection.
 */
internal interface ReadableMessageSocket : ASocket, CoroutineScope {
    suspend fun read(readChunkSize: Int, getPreviousMessage: (Int) -> Message?): Message
}