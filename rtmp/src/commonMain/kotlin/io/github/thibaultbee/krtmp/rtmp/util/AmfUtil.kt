package io.github.thibaultbee.krtmp.rtmp.util

import io.github.thibaultbee.krtmp.amf.Amf
import kotlinx.serialization.ExperimentalSerializationApi

object AmfUtil {
    /**
     * AMF serializer for FLV and RTMP.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val amf = Amf {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }
}