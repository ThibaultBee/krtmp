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

import io.github.thibaultbee.krtmp.rtmp.messages.ResultInformation
import io.github.thibaultbee.krtmp.rtmp.util.NetConnectionConnectCode
import kotlinx.serialization.Serializable

fun NetConnectionResultInformation(
    level: String,
    code: NetConnectionConnectCode,
    description: String,
    objectEncoding: ObjectEncoding
) = NetConnectionResultInformation(
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
class NetConnectionResultInformation(
    override val level: String,
    override val code: NetConnectionConnectCode,
    override val description: String,
    val objectEncoding: Double,
) : ResultInformation
