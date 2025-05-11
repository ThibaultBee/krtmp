/*
 * Copyright (C) 2025 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp.messages.command

import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_CAPABILITIES
import kotlinx.serialization.Serializable

/**
 * Represents the result of a NetConnection command in RTMP.
 *
 * @property fmsVer The version of the Flash Media Server.
 * @property capabilities The capabilities of the server.
 */
@Serializable
class NetConnectionResultObject(
    val fmsVer: String = DEFAULT_FMS_VER,
    val capabilities: Double = DEFAULT_CAPABILITIES.toDouble(),
) {
    companion object {
        internal const val DEFAULT_FMS_VER = "FMS/3,0,1,123"

        internal val default = NetConnectionResultObject()
    }
}
