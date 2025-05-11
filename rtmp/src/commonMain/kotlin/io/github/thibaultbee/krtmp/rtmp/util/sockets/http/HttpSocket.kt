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
package io.github.thibaultbee.krtmp.rtmp.util.sockets.http

import io.github.thibaultbee.krtmp.rtmp.util.sockets.ISocket
import io.github.thibaultbee.krtmp.rtmp.util.sockets.http.HttpSocket.Companion.createRtmptClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.discard
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException


internal suspend fun HttpSocket(
    urlBuilder: URLBuilder,
): HttpSocket {
    val client = createRtmptClient()
    try {
        val sessionId = HttpSocket.connect(urlBuilder, client)
        return HttpSocket(urlBuilder, client, sessionId)
    } catch (e: Throwable) {
        client.close()
        throw e
    }
}

internal class HttpSocket internal constructor(
    override val urlBuilder: URLBuilder,
    private val client: HttpClient,
    private val sessionId: String
) :
    ISocket {
    private val postByteReadChannels = mutableListOf<ByteReadChannel>()

    private var _index = 1L
    private val index: Long
        get() = _index++

    override val coroutineContext = client.coroutineContext
    override val socketContext = coroutineContext as CompletableJob

    override val isClosed: Boolean
        get() = !client.isActive

    override var totalBytesRead: Long = 0
        private set

    override var totalBytesWritten: Long = 0
        private set

    override suspend fun write(
        length: Long,
        block: suspend (ByteWriteChannel) -> Unit
    ) {
        require(!isClosed) { "Connection is closed" }

        val response =
            post(
                "send/$sessionId/$index",
                object : OutgoingContent.WriteChannelContent() {
                    override val contentLength = length

                    override suspend fun writeTo(channel: ByteWriteChannel) {
                        block(channel)
                    }
                })
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Send failed. Expected 200, got ${response.status}")
        }
        totalBytesWritten += length

        val body = response.bodyAsChannel().apply {
            discard(1) // Discard first byte
        }
        if (body.availableForRead > 0) {
            totalBytesRead += body.availableForRead
            postByteReadChannels.add(body)
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

    override fun close() {
        runBlocking {
            post("close/$sessionId", ByteArray(0))
        }
        client.close()
        postByteReadChannels.clear()
    }

    private suspend fun post(path: String, body: Any? = null) =
        client.post(urlBuilder, path) {
            setBody(body)
        }

    companion object {
        private val rtmptHeaders = mapOf(
            "Content-Type" to "application/x-fcs",
            "User-Agent" to "Shockwave Flash"
        )

        internal fun createRtmptClient(): HttpClient {
            return HttpClient {
                defaultRequest {
                    headers {
                        rtmptHeaders.forEach { (key, value) ->
                            append(key, value)
                        }
                    }
                }
            }
        }

        internal suspend fun connect(
            urlBuilder: URLBuilder,
            client: HttpClient
        ): String {
            try {
                var response = client.post(urlBuilder, "fcs/ident2") {
                    setBody(byteArrayOf(0x00))
                }
                // Expected 404 but some servers return other error code
                if (response.status.value !in 400..499) {
                    throw IllegalStateException("Connection failed. Expected 404, got ${response.status}")
                }

                response = client.post(urlBuilder, "open/1")
                if (response.status != HttpStatusCode.OK) {
                    throw IllegalStateException("Connection failed. Expected 200, got ${response.status}")
                }

                val sessionId = response.bodyAsText().trimIndent()

                readIdle(urlBuilder, client, sessionId) {}
                return sessionId
            } catch (t: Throwable) {
                throw t
            }
        }

        private suspend fun <T> readIdle(
            urlBuilder: URLBuilder,
            client: HttpClient,
            sessionId: String,
            block: suspend (ByteReadChannel) -> T
        ): T? {
            return try {
                val response = client.post(urlBuilder, "idle/$sessionId/0")
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
                throw t
            }
        }
    }
}