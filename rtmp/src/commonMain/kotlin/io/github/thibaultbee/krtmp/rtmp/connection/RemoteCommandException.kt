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
package io.github.thibaultbee.krtmp.rtmp.connection

import io.github.thibaultbee.krtmp.rtmp.messages.Command

/**
 * This exception is thrown when the remote device returns an error.
 *
 * @param message the detail message.
 * @param command the command send by the server.
 */
class RemoteCommandException(message: String, val command: Command) : Exception(message)