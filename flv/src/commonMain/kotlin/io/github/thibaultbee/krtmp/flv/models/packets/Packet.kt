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
package io.github.thibaultbee.krtmp.flv.models.packets

/**
 * A convenient class for output packet.
 *
 * It will transform the packet to the desired format (ByteArray, Buffer, Sink, RawSource).
 *
 * @param timestampMs the timestamp of the packet in milliseconds
 */
abstract class Packet(val timestampMs: Int) : PacketWriter()