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

import io.github.thibaultbee.krtmp.rtmp.util.RtmpURLProtocol
import io.ktor.http.URLProtocol

val URLProtocol.isRtmp: Boolean
    get() = RtmpURLProtocol.byName.containsKey(name)


val URLProtocol.isSecureRtmp: Boolean
    get() {
        return this.name == RtmpURLProtocol.RMTPS.name ||
                this.name == RtmpURLProtocol.RTMPTS.name
    }

val URLProtocol.isTunneledRtmp: Boolean
    get() {
        return this.name == RtmpURLProtocol.RTMPT.name ||
                this.name == RtmpURLProtocol.RTMPTE.name ||
                this.name == RtmpURLProtocol.RTMPTS.name
    }

