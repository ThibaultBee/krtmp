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

/**
 * Represents the type of stream publishing.
 *
 * @property value The string representation of the stream publish type.
 */
enum class StreamPublishType(val value: String) {
    LIVE("live"), RECORD("record"), APPEND("append");

    companion object {
        fun valueOf(value: String): StreamPublishType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown stream type: $value")
        }
    }
}
