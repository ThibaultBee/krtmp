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
package io.github.thibaultbee.krtmp.rtmp.server.util

/**
 * Interface for providing unique stream IDs to `createStream` and `deleteStream` calls.
 */
interface IStreamIdProvider {
    /**
     * Gets the next stream ID.
     *
     * The returned stream ID must be unique.
     * It must be greater than 0 and different from 2 (reserved for control messages).
     *
     * @return the next stream ID.
     */
    fun create(): Int

    /**
     * Resets the stream ID counter.
     *
     * @param streamId the stream ID to delete.
     */
    fun delete(streamId: Int)

    /**
     * Whether the given stream ID is already in use.
     *
     * @param streamId the stream ID to check
     * @return true if the stream ID is known by the [IStreamIdProvider] implementation, false otherwise.
     */
    fun hasStreamId(streamId: Int): Boolean
}

/**
 * Default implementation of [IStreamIdProvider].
 *
 * This implementation starts from 3 and increments the stream ID for each call to `create()`.
 * It keeps track of used stream IDs to ensure uniqueness.
 *
 * The [hasStreamId] method always returns true, indicating that all stream IDs are considered valid.
 */
class DefaultStreamIdProvider : IStreamIdProvider {
    private var nextStreamId = 3 // Start from 3 to avoid reserved IDs (0, 2)
    private val usedStreamIds = mutableSetOf<Int>()

    override fun create(): Int {
        val streamId = nextStreamId++
        usedStreamIds.add(streamId)
        return streamId
    }

    override fun delete(streamId: Int) {
        usedStreamIds.remove(streamId)
    }

    override fun hasStreamId(streamId: Int) = true
}