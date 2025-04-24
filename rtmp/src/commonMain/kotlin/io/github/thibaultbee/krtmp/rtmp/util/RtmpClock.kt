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

import kotlin.time.TimeSource

interface RtmpClock {
    /**
     * Returns the current time in milliseconds from the connection.
     */
    val nowInMs: Int

    /**
     * Resets the clock so the first value returned by [nowInMs] will be 0.
     */
    fun reset()

    /**
     * Default implementation of [RtmpClock] using [TimeSource.Monotonic].
     */
    class Default : RtmpClock {
        private var startTimeMark = TimeSource.Monotonic.markNow()
        private var updateTimeMark = false
        
        override val nowInMs: Int
            get() {
                return if (updateTimeMark) {
                    startTimeMark = TimeSource.Monotonic.markNow()
                    updateTimeMark = false
                    0
                } else {
                    (startTimeMark.elapsedNow().inWholeMilliseconds).toInt()
                }
            }

        override fun reset() {
            updateTimeMark = true
        }
    }
}
