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

import io.github.thibaultbee.krtmp.rtmp.messages.NetInformation
import io.github.thibaultbee.krtmp.rtmp.messages.command.ConnectObject.Companion.DEFAULT_CAPABILITIES
import io.github.thibaultbee.krtmp.rtmp.util.NetConnectionConnectCode
import io.github.thibaultbee.krtmp.rtmp.util.NetConnectionConnectCodeReconnect
import io.github.thibaultbee.krtmp.rtmp.util.NetStreamOnStatusLevelStatus
import kotlinx.serialization.Serializable

fun NetConnectionResultInformation(
    level: String,
    code: NetConnectionConnectCode,
    description: String,
    objectEncoding: ObjectEncoding
) = NetConnectionConnectResultInformation(
    level = level,
    code = code,
    description = description,
    objectEncoding = objectEncoding.value.toDouble()
)

/**
 * Represents the result information for a NetConnection command.
 *
 * @property level The level of the result (e.g., "status", "error").
 * @property code The specific code indicating the result type.
 * @property description A human-readable description of the result.
 * @property objectEncoding The object encoding version used in the connection.
 */
@Serializable
class NetConnectionConnectResultInformation(
    override val level: String,
    override val code: NetConnectionConnectCode,
    override val description: String,
    val objectEncoding: Double,
) : NetInformation

/**
 * Represents the result of a NetConnection command in RTMP.
 *
 * @property fmsVer The version of the Flash Media Server.
 * @property capabilities The capabilities of the server.
 */
@Serializable
class NetConnectionConnectResultObject(
    val fmsVer: String = DEFAULT_FMS_VER,
    val capabilities: Double = DEFAULT_CAPABILITIES.toDouble(),
) {
    companion object Companion {
        internal const val DEFAULT_FMS_VER = "FMS/3,0,1,123"

        internal val default = NetConnectionConnectResultObject()
    }
}

/**
 * Represents the result information for a NetConnection command.
 *
 * @property level The level of the result (e.g., "status", "error").
 * @property code The specific code indicating the result type.
 * @property description A human-readable description of the result.
 */
@Serializable
class NetConnectionResultInformation(
    override val level: String,
    override val code: NetConnectionConnectCode,
    override val description: String
) : NetInformation

/**
 * Represents the result information for a NetConnection reconnect request in RTMP.
 *
 * @property description A human-readable description of the result.
 * @property tcUrl The TCUrl of the connection, which is the target URL for the RTMP connection.
 */
@Serializable
class NetConnectionReconnectRequestInformation(
    override val description: String,
    val tcUrl: String?,
) : NetInformation {
    override val level = NetStreamOnStatusLevelStatus
    override val code = NetConnectionConnectCodeReconnect
}
