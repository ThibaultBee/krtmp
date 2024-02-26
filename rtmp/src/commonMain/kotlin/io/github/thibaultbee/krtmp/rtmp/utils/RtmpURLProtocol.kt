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
package io.github.thibaultbee.krtmp.rtmp.utils

import io.ktor.http.URLProtocol

object RtmpURLProtocol {
    const val RTMP_DEFAULT_PORT = 1935
    const val SECURE_RTMP_DEFAULT_PORT = 443
    const val TUNNELED_RTMP_DEFAULT_PORT = 80

    val RTMP = URLProtocol("rtmp", RTMP_DEFAULT_PORT)
    val RMTPS = URLProtocol("rtmps", SECURE_RTMP_DEFAULT_PORT)
    val RTMPT = URLProtocol("rtmpt", TUNNELED_RTMP_DEFAULT_PORT)
    val RTMPE = URLProtocol("rtmpe", RTMP_DEFAULT_PORT)
    val RTMFP = URLProtocol("rtmfp", RTMP_DEFAULT_PORT)
    val RTMPTE = URLProtocol("rtmpte", TUNNELED_RTMP_DEFAULT_PORT)
    val RTMPTS = URLProtocol("rtmpts", SECURE_RTMP_DEFAULT_PORT)

    val byName: Map<String, URLProtocol> = listOf(
        RTMP,
        RMTPS,
        RTMPT,
        RTMPE,
        RTMFP,
        RTMPTE,
        RTMPTS
    ).associateBy { it.name }

    fun createOrDefault(name: String): URLProtocol {
        return byName[name] ?: RTMP
    }
}
