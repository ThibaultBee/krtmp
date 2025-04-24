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
package io.github.thibaultbee.krtmp.flv

import io.github.thibaultbee.krtmp.amf.AmfVersion
import io.github.thibaultbee.krtmp.flv.models.util.FlvHeader
import io.github.thibaultbee.krtmp.flv.models.tags.FLVTag
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

/**
 * Creates a [FLVDemuxer] dedicated to read from a file.
 *
 * @param path the path to the file
 * @param amfVersion the AMF version to use
 * @return a [FLVDemuxer]
 */
fun FLVDemuxer(
    path: Path,
    amfVersion: AmfVersion = AmfVersion.AMF0
): FlvDemuxer {
    val source = SystemFileSystem.source(path)
    return FlvDemuxer(source.buffered(), amfVersion)
}

/**
 * Demuxer for FLV format.
 *
 * @param source the source to read from
 * @param amfVersion the AMF version to use
 */
class FlvDemuxer(private val source: Source, private val amfVersion: AmfVersion = AmfVersion.AMF0) {
    private var hasDecoded = false

    /**
     * Decodes a single FLV frames.
     *
     * @return the decoded [FLVTag]
     */
    fun decode(): FLVTag {
        val peek = source.peek()
        val isHeader = try {
            peek.readString(3) == "FLV"
        } catch (e: Exception) {
            false
        }

        if (isHeader) {
            // Skip header
            FlvHeader.decode(source)
        }

        val previousTagSize = source.readInt()
        if (!hasDecoded) {
            hasDecoded = true
            require(previousTagSize == 0) { "Invalid FlvHeader. Expected PreviousTagSize0 to be 0." }
        }

        return FLVTag.decode(source, amfVersion)
    }


    fun decodeFlvHeader(): FlvHeader {
        val peek = source.peek()
        val isHeader = try {
            peek.readString(3) == "FLV"
        } catch (e: Exception) {
            false
        }
        if (isHeader) {
            return FlvHeader.decode(source)
        } else {
            throw IllegalStateException("Not a FLV header")
        }
    }
}

