/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.extensions

import io.ktor.http.URLBuilder

fun URLBuilder.validateRtmp() {
    require(protocol.isRtmp) { "Invalid protocol $protocol" }
    require(host.isNotBlank()) { "Invalid host $host" }
    require(port in 0..65535) { "Port must be in range 0..65535" }
    require(pathSegments.size > 2) { "Invalid number of elements in path at least 2 but found ${pathSegments.size}" }
    require(pathSegments.last().isNotBlank()) { "Invalid stream key ${pathSegments.last()}" }
}

val URLBuilder.isValidRtmp: Boolean
    get() {
        return try {
            validateRtmp()
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }


val URLBuilder.streamKey: String
    get() {
        val streamKey = pathSegments.last()
        require(streamKey.isNotBlank()) { "Invalid stream key $streamKey" }
        return streamKey
    }


