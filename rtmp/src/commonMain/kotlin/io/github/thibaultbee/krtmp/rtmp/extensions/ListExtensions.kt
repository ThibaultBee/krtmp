package io.github.thibaultbee.krtmp.rtmp.extensions

import io.github.thibaultbee.krtmp.rtmp.chunk.Chunk
import io.ktor.utils.io.ByteWriteChannel

fun <T> List<T>.orNull(): List<T>? = ifEmpty { null }

suspend fun List<Chunk>.write(writeChannel: ByteWriteChannel) = forEach { it.write(writeChannel) }