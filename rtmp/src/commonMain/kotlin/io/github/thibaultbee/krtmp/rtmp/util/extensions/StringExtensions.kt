package io.github.thibaultbee.krtmp.rtmp.util.extensions


private val schemeRegex = "^[a-zA-Z][a-zA-Z0-9+.-]*://".toRegex()

/**
 * Whether the string starts with a scheme.
 *
 * For example, "rtmp://", "http://", "https://", etc.
 */
internal fun String.startWithScheme() =
    schemeRegex.containsMatchIn(this)