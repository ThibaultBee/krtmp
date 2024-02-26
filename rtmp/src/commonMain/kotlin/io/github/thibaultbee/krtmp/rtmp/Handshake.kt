/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.rtmp

import io.github.thibaultbee.krtmp.rtmp.utils.RtmpClock
import io.github.thibaultbee.krtmp.rtmp.utils.connections.IConnection
import io.github.thibaultbee.krtmp.rtmp.utils.connections.TcpConnection
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readFully
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByte
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import kotlin.random.Random

/**
 * Implementation of RTMP handshake.
 */
internal class Handshake(
    private val connection: IConnection,
    private val clock: RtmpClock,
    private val version: Byte = 0x3,
) {
    suspend fun startClient() {
        val c0 = Zero(version)
        val c1 = One(0, Random.nextBytes(RANDOM_DATA_SIZE))
        connection.write(Zero.LENGTH + One.LENGTH.toLong()) {
            c0.write(it)
            c1.write(it)
        }

        val s0 = connection.read {
            Zero.read(it)
        }
        require(s0.version == version) { "Handshake failed: S0 and C0 must have the same version: ${s0.version} instead of $version" }

        val s1 = connection.read {
            One.read(it)
        }

        val time2 = clock.nowInMs

        val c2 = Two(s1.timestamp, time2, s1.random)
        connection.write(Two.LENGTH.toLong()) {
            c2.write(it)
        }

        val s2 = connection.read {
            Two.read(it)
        }

        if (connection is TcpConnection) {
            require(s2.timestamp == c1.timestamp) { "Handshake failed: S2 and C1 must have the same timestamp" }
            require(s2.random.contentEquals(c1.random)) { "Handshake failed: S2 and C1 must have the same random sequence" }
        }
    }

    private class Zero(val version: Byte) {
        suspend fun write(writeChannel: ByteWriteChannel) {
            writeChannel.writeByte(version)
        }

        companion object {
            const val LENGTH = 1

            suspend fun read(readChannel: ByteReadChannel): Zero {
                val version = readChannel.readByte()
                return Zero(version)
            }
        }
    }

    private class One(val timestamp: Int, val random: ByteArray) {
        init {
            require(random.size == RANDOM_DATA_SIZE)
        }

        suspend fun write(writeChannel: ByteWriteChannel) {
            writeChannel.writeInt(timestamp)
            writeChannel.writeInt(0)
            writeChannel.writeFully(random, 0, random.size)
        }

        companion object {
            const val LENGTH = 8 + RANDOM_DATA_SIZE

            suspend fun read(readChannel: ByteReadChannel): One {
                val timestamp = readChannel.readInt()
                readChannel.readInt() // Zeros
                val random = ByteArray(RANDOM_DATA_SIZE)
                readChannel.readFully(random)
                return One(timestamp, random)
            }
        }
    }

    private class Two(
        val timestamp: Int, val timestamp2: Int, val random: ByteArray
    ) {
        init {
            require(random.size == RANDOM_DATA_SIZE)
        }

        suspend fun write(writeChannel: ByteWriteChannel) {
            writeChannel.writeInt(timestamp)
            writeChannel.writeInt(timestamp2)
            writeChannel.writeFully(random, 0, random.size)
        }

        companion object {
            const val LENGTH = 8 + RANDOM_DATA_SIZE

            suspend fun read(readChannel: ByteReadChannel): Two {
                val timestamp = readChannel.readInt()
                val timestamp2 = readChannel.readInt()
                val random = ByteArray(RANDOM_DATA_SIZE)
                readChannel.readFully(random)
                return Two(timestamp, timestamp2, random)
            }
        }
    }

    companion object {
        private const val HANDSHAKE_SIZE = 1536
        private const val RANDOM_DATA_SIZE = HANDSHAKE_SIZE - 8
    }
}