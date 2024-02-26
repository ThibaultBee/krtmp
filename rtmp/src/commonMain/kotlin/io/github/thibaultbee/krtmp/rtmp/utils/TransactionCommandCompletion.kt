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
package io.github.thibaultbee.krtmp.rtmp.utils

import io.github.thibaultbee.krtmp.rtmp.client.RemoteServerException
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import kotlinx.coroutines.CompletableDeferred


/**
 * This class is used to store channels used to send command responses.
 */
class TransactionCommandCompletion {
    private val deferreds = mutableMapOf<Any, CompletableDeferred<Command>>()
    suspend fun waitForResponse(key: Any): Command {
        val deferred = CompletableDeferred<Command>()
        deferreds[key] = deferred
        return deferred.await()
    }

    fun complete(key: Any, command: Command) {
        val deferred = deferreds[key]
        deferred?.complete(command)
    }

    fun completeExceptionally(key: Any, error: Command) {
        val deferred = deferreds[key]
        deferred?.completeExceptionally(
            RemoteServerException(
                "Command failed with error: $error",
                error
            )
        )
    }

    fun completeExceptionally(key: Any, t: Throwable) {
        val deferred = deferreds[key]
        deferred?.completeExceptionally(Exception("Command failed with error: $t", t))
    }

    fun completeAllExceptionally(t: Throwable) {
        deferreds.forEach { (_, deferred) ->
            deferred.completeExceptionally(Exception("Command failed with error: $t", t))
        }
    }
}