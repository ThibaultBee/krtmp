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
package io.github.thibaultbee.krtmp.rtmp.utils.connections

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.discard
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlin.coroutines.cancellation.CancellationException

internal class HttpConnection(private val urlBuilder: URLBuilder) : IConnection {
    private val client = HttpClient {
        defaultRequest {
            headers {
                rtmptHeaders.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
    }
    private var connectionId: String? = null

    private val postByteReadChannels = mutableListOf<ByteReadChannel>()

    private var _index = 0L
    private val index: Long
        get() = _index++

    override val coroutineContext = client.coroutineContext

    private var _isClosed: Boolean = true
    override val isClosed: Boolean
        get() = _isClosed

    private var _totalBytesRead: Long = 0
    override val totalBytesRead: Long
        get() = _totalBytesRead

    private var _totalBytesWritten: Long = 0
    override val totalBytesWritten: Long
        get() = _totalBytesWritten

    private var _closedCause: Throwable? = null
    override val closedCause: Throwable?
        get() = _closedCause

    override fun invokeOnCompletion(handler: CompletionHandler) {
        client.coroutineContext.job.invokeOnCompletion { handler(it) }
    }

    override suspend fun connect() {
        try {
            var response = post("fcs/ident2", byteArrayOf(0x00))
            // Expected 404 but some servers return other error code
            if (response.status.value !in 400..499) {
                throw IllegalStateException("Connection failed. Expected 404, got ${response.status}")
            }
            response = post("open/1")
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Connection failed. Expected 200, got ${response.status}")
            }
            val sessionId = response.bodyAsText().trimIndent()
            this.connectionId = sessionId

            readIdle {}
            _isClosed = false
        } catch (t: Throwable) {
            throwException(t)
            throw t
        }
    }

    override suspend fun write(
        length: Long,
        block: suspend (ByteWriteChannel) -> Unit
    ) {
        require(!isClosed) { "Connection is closed" }

        try {
            val response =
                post(
                    "send/${connectionId!!}/$index",
                    object : OutgoingContent.WriteChannelContent() {
                        override val contentLength = length

                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            block(channel)
                        }
                    })
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Send failed. Expected 200, got ${response.status}")
            }
            _totalBytesWritten += length

            val body = response.bodyAsChannel().apply {
                discard(1) // Discard first byte
            }
            if (body.availableForRead > 0) {
                _totalBytesRead += body.availableForRead
                postByteReadChannels.add(body)
            }
        } catch (t: Throwable) {
            throwException(t)
            throw t
        }
    }

    override suspend fun <T> read(block: suspend (ByteReadChannel) -> T): T {
        require(!isClosed) { "Connection is closed" }

        val result = readMemory(block)
        if (result != null) {
            return result
        }

        val coroutine = client.async<T> {
            var res: T? = null
            while (isActive) {
                val read = readMemory(block)
                if (read != null) {
                    res = read
                    break
                }
                delay(500)
            }
            res ?: throw CancellationException()
        }
        return coroutine.await()
    }

    private suspend fun <T> readMemory(block: suspend (ByteReadChannel) -> T): T? {
        return if (postByteReadChannels.isNotEmpty()) {
            val body = postByteReadChannels.first()
            val result = block(body)
            if (body.availableForRead == 0) {
                postByteReadChannels.removeFirst()
            }
            result
        } else {
            null
        }
    }

    private suspend fun <T> readIdle(block: suspend (ByteReadChannel) -> T): T? {
        return try {
            val response = post("idle/${connectionId!!}/$index")
            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException("Send failed. Expected 200, got ${response.status}")
            }
            val body = response.bodyAsChannel().apply {
                discard(1) // Discard first byte
            }
            if (body.availableForRead > 0) {
                block(body)
            } else {
                null
            }
        } catch (t: Throwable) {
            throwException(t)
            throw t
        }
    }

    override suspend fun close() {
        connectionId?.let {
            post("close/$it", ByteArray(0))
        }
        client.close()
        client.coroutineContext.cancelChildren()
        postByteReadChannels.clear()
        connectionId = null
        _isClosed = true
    }

    private suspend fun post(path: String, array: ByteArray) =
        post(path, array as Any)

    @OptIn(InternalAPI::class)
    private suspend fun post(path: String, anyBody: Any? = null) =
        client.post {
            url {
                takeFrom(urlBuilder.buildString())
                encodedPath = path
            }
            if (anyBody != null) {
                body = anyBody
            }
        }

    private fun throwException(t: Throwable) {
        _closedCause = t
    }

    companion object {
        private val rtmptHeaders = mapOf(
            "Content-Type" to "application/x-fcs",
            "User-Agent" to "Shockwave Flash"
        )
    }
}