package io.github.thibaultbee.krtmp.flv.util

import io.github.thibaultbee.krtmp.amf.Amf
import kotlinx.serialization.ExperimentalSerializationApi

internal object AmfUtil {
    /**
     * AMF serializer for FLV and RTMP.
     */
    @OptIn(ExperimentalSerializationApi::class)
    internal val amf = Amf {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    }
}