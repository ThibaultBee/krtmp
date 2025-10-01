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
package io.github.thibaultbee.krtmp.rtmp.util

import kotlinx.coroutines.CompletableDeferred

/**
 * A data class that holds data of type [T] and a [CompletableDeferred] of type [V]
 * to signal when the data has been processed.
 *
 * @param T The type of the data to be processed.
 * @property data The data to be processed.
 * @property processed A [CompletableDeferred] that will be completed when the data has been processed.
 */
data class Processable<T>(
    val data: T,
    val processed: CompletableDeferred<Unit> = CompletableDeferred()
)