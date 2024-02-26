/*
 * Copyright (C) 2022 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.tags

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink

sealed class FlvTag(
    val timestampMs: Int,
    val type: Type
) {
    internal abstract val bodySize: Int

    internal abstract fun writeBodyToSink(output: Sink, isEncrypted: Boolean)

    internal open fun readRawSource(isEncrypted: Boolean): RawSource {
        return Buffer().apply { writeBodyToSink(this, isEncrypted) }
    }

    enum class Type(val value: Int) {
        AUDIO(8),
        VIDEO(9),
        SCRIPT(18);

        companion object {
            fun entryOf(value: Int) = entries.first { it.value == value }
        }
    }
}



