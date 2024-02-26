package io.github.thibaultbee.krtmp.rtmp.utils.connections

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.CoroutineScope

interface IConnection : CoroutineScope {
    val isClosed: Boolean
    val totalBytesRead: Long
    val totalBytesWritten: Long
    val closedCause: Throwable?

    fun invokeOnCompletion(handler: CompletionHandler)

    suspend fun connect()

    suspend fun write(
        length: Long,
        block: suspend (ByteWriteChannel) -> Unit
    )

    suspend fun <T> read(block: suspend (ByteReadChannel) -> T): T

    suspend fun close()
}