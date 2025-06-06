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
package io.github.thibaultbee.krtmp.rtmp.messages

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.Amf0ElementReader
import io.github.thibaultbee.krtmp.amf.elements.Amf3ElementReader
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfElementFactory
import io.github.thibaultbee.krtmp.amf.elements.AmfPrimitive
import io.github.thibaultbee.krtmp.amf.elements.containers.amfContainerOf
import io.github.thibaultbee.krtmp.flv.tags.script.OnMetadata
import io.github.thibaultbee.krtmp.rtmp.chunk.ChunkStreamId
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf.Companion.DATAAMF_SET_DATA_FRAME_NAME
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.RawSource
import kotlinx.io.buffered

internal open class DataAmfMessage(
    messageStreamId: Int,
    timestamp: Int,
    messageType: MessageType,
    payload: RawSource,
    payloadSize: Int,
    chunkStreamId: Int = ChunkStreamId.COMMAND_CHANNEL.value
) : Message(
    chunkStreamId = chunkStreamId,
    messageStreamId = messageStreamId,
    timestamp = timestamp,
    messageType = messageType,
    payload = payload,
    payloadSize = payloadSize
)

internal fun SetDataFrame(
    amfVersion: AmfVersion,
    messageStreamId: Int,
    timestamp: Int,
    payload: RawSource,
    payloadSize: Int
) =
    DataAmfMessage(
        messageStreamId,
        timestamp,
        if (amfVersion == AmfVersion.AMF0) MessageType.DATA_AMF0 else MessageType.DATA_AMF3,
        payload,
        payloadSize
    )

open class DataAmf(
    val messageStreamId: Int,
    val timestamp: Int,
    val name: String,
    val parameters: AmfElement?,
) : AmfMessage {
    override fun createMessage(amfVersion: AmfVersion): Message {
        val payload =
            AmfElementFactory.buildContainer(
                listOf(
                    name,
                    parameters
                )
            )
        val messageType = when (amfVersion) {
            AmfVersion.AMF0 -> MessageType.DATA_AMF0
            AmfVersion.AMF3 -> MessageType.DATA_AMF3
        }
        return DataAmfMessage(
            messageStreamId = messageStreamId,
            timestamp = timestamp,
            messageType = messageType,
            payload = payload.write(amfVersion),
            payloadSize = payload.getSize(amfVersion),
        )
    }

    override suspend fun write(
        writeChannel: ByteWriteChannel,
        amfVersion: AmfVersion,
        chunkSize: Int,
        previousMessage: Message?
    ): Int {
        return createMessage(amfVersion).write(writeChannel, chunkSize, previousMessage)
    }

    override fun toString(): String {
        return "DataAmf(name=$name, timestamp=$timestamp, messageStreamId=$messageStreamId, parameters=$parameters)"
    }

    companion object {
        const val DATAAMF_SET_DATA_FRAME_NAME = "@setDataFrame"

        internal fun read(
            dataAmfMessage: DataAmfMessage
        ): DataAmf {
            val amfElementReader = when (dataAmfMessage.messageType) {
                MessageType.DATA_AMF0 -> Amf0ElementReader
                MessageType.DATA_AMF3 -> Amf3ElementReader
                else -> throw IllegalArgumentException("Unknown message type: ${dataAmfMessage.messageType}")
            }
            val payload = dataAmfMessage.payload.buffered()

            @Suppress("UNCHECKED_CAST")
            val name = (amfElementReader.read(payload) as AmfPrimitive<String>).value
            val parameter =
                if (!payload.exhausted()) amfElementReader.read(payload) else null
            val parameterContent =
                if (!payload.exhausted()) amfElementReader.read(payload) else null
            val parameters = if (parameterContent != null) {
                amfContainerOf(
                    listOf(
                        parameter!!,
                        parameterContent
                    )
                )
            } else {
                parameter
            }
            return DataAmf(
                dataAmfMessage.messageStreamId,
                dataAmfMessage.timestamp,
                name,
                parameters
            )
        }
    }
}

fun SetDataFrame(
    messageStreamId: Int,
    timestamp: Int,
    metadata: OnMetadata.Metadata,
) =
    DataAmf(
        messageStreamId,
        timestamp,
        DATAAMF_SET_DATA_FRAME_NAME,
        amfContainerOf(
            listOf(
                "onMetaData",
                metadata.encode()
            )
        )
    )