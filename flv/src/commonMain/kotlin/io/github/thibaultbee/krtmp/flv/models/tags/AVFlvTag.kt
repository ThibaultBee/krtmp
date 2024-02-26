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

import io.github.thibaultbee.krtmp.flv.models.sources.MultiRawSource
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink

sealed class AVFlvTag(
    timestampMs: Int,
    type: Type,
    val source: RawSource,
    val sourceSize: Int
) : FlvTag(timestampMs, type) {
    abstract override val bodySize: Int

    internal abstract fun writeTagHeader(output: Sink)
    internal abstract fun writeTagBody(output: Sink)

    override fun writeBodyToSink(output: Sink, isEncrypted: Boolean) {
        writeTagHeader(output)
        writeTagBody(output)
    }

    override fun readRawSource(isEncrypted: Boolean): RawSource {
        return MultiRawSource(listOf(Buffer().apply { writeTagHeader(this) }, source))
    }
}


