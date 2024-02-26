package io.github.thibaultbee.krtmp.rtmp.extensions

inline fun <T> List<T>.orNull(): List<T>? = ifEmpty { null }