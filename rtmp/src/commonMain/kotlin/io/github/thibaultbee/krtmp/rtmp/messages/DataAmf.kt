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

import io.github.thibaultbee.krtmp.amf.Amf
import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import io.github.thibaultbee.krtmp.amf.elements.AmfElementFactory
import io.github.thibaultbee.krtmp.amf.elements.containers.amfContainerOf
import io.github.thibaultbee.krtmp.amf.elements.containers.amfEcmaArrayOf
import io.github.thibaultbee.krtmp.amf.elements.containers.AmfObject
import io.github.thibaultbee.krtmp.flv.models.tags.OnMetadata
import io.github.thibaultbee.krtmp.rtmp.chunk.ChunkStreamId
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.RawSource

internal open class DataAmfMessage(
    messageStreamId: Int,
    timestamp: Int,
    messageType: MessageType,
    payload: RawSource
) : Message(
    chunkStreamId = ChunkStreamId.COMMAND_CHANNEL.value,
    messageStreamId = messageStreamId,
    timestamp = timestamp,
    messageType = messageType,
    payload = payload
) {
    class SetDataFrame(
        amfVersion: AmfVersion,
        messageStreamId: Int,
        timestamp: Int,
        payload: RawSource
    ) :
        DataAmfMessage(
            messageStreamId,
            timestamp,
            if (amfVersion == AmfVersion.AMF0) MessageType.DATA_AMF0 else MessageType.DATA_AMF3,
            payload
        )
}

internal open class DataAmf(
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
            payload = payload.write(amfVersion)
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

    class SetDataFrame(
        messageStreamId: Int,
        timestamp: Int,
        metadata: OnMetadata.Metadata,
    ) :
        DataAmf(
            messageStreamId,
            timestamp,
            "@setDataFrame",
            amfContainerOf(
                listOf(
                    "onMetaData",
                    // Swapping elements to ECMA array
                    amfEcmaArrayOf(
                        (Amf.encodeToAmfElement(
                            OnMetadata.Metadata.serializer(),
                            metadata
                        ) as AmfObject)
                    )
                )
            )
        )
}
