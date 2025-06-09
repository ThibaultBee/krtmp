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
package io.github.thibaultbee.krtmp.rtmp.util

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom

/**
 * Creates a [URLBuilder] from an RTMP URL.
 *
 * @param url the RTMP URL to build from
 * @return a [URLBuilder] initialized with the RTMP URL
 */
fun RtmpURLBuilder(url: Url): URLBuilder {
    val urlBuilder = URLBuilder().takeFrom(url)
    if (urlBuilder.port == 0) {
        urlBuilder.port = RtmpURLProtocol.createOrDefault(urlBuilder.protocol.name).defaultPort
    }
    return urlBuilder
}

/**
 * Creates a [URLBuilder] from a string representation of an RTMP URL.
 *
 * @param urlString the string representation of the RTMP URL
 * @return a [URLBuilder] initialized with the RTMP URL
 */
fun RtmpURLBuilder(urlString: String): URLBuilder {
    val urlBuilder = URLBuilder().takeFrom(urlString)
    if (urlBuilder.port == 0) {
        urlBuilder.port = RtmpURLProtocol.createOrDefault(urlBuilder.protocol.name).defaultPort
    }
    return urlBuilder
}