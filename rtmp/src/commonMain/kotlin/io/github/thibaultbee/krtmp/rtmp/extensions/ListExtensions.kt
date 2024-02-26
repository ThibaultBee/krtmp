package io.github.thibaultbee.krtmp.rtmp.extensions

fun <T> List<T>.orNull(): List<T>? = ifEmpty { null }