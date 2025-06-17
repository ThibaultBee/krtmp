package io.github.thibaultbee.krtmp.rtmp.extensions

import io.github.thibaultbee.krtmp.rtmp.messages.chunk.Chunk
import io.ktor.utils.io.ByteWriteChannel

internal fun <T> List<T>.orNull(): List<T>? = ifEmpty { null }

internal suspend fun List<Chunk>.write(writeChannel: ByteWriteChannel) = forEach { it.write(writeChannel) }