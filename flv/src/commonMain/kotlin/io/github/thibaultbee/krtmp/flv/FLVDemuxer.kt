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

import io.github.thibaultbee.krtmp.flv.tags.FLVTag
import io.github.thibaultbee.krtmp.flv.tags.RawFLVTag
import io.github.thibaultbee.krtmp.flv.util.FLVHeader
import kotlinx.coroutines.coroutineScope
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
    path: Path
): FLVDemuxer {
    val source = SystemFileSystem.source(path)
    return FLVDemuxer(source.buffered())
}

/**
 * Demuxer for FLV format.
 *
 * @param source the source to read from
 * @param amfVersion the AMF version to use
 */
class FLVDemuxer(private val source: Source) {
    private var hasDecoded = false

    /**
     * Whether the source contains any FLV frame.
     */
    val isEmpty: Boolean
        get() {
            /**
             * After decoding the last frame, the PreviousTagSizeN-1 (4 bits) is still there.
             * but there isn't a frame to decode.
             */
            return !source.request(5)
        }

    /**
     * Decodes a single FLV frames, the data is parsed.
     *
     * @return the decoded [FLVTag]
     */
    fun decode(): FLVTag {
        return decode { FLVTag.decode(source) }
    }

    /**
     * Decodes only the raw FLV tag of the next frame.
     * The data is not parsed.
     *
     * @return the decoded [RawFLVTag]
     */
    fun decodeRaw(): RawFLVTag {
        return decode { RawFLVTag.decode(source) }
    }

    private fun <T> decode(block: (Source) -> T): T {
        val peek = source.peek()
        val isHeader = try {
            peek.readString(3) == "FLV"
        } catch (e: Exception) {
            false
        }

        if (isHeader) {
            // Skip header
            FLVHeader.decode(source)
        }

        val previousTagSize = source.readInt()
        if (!hasDecoded) {
            hasDecoded = true
            require(previousTagSize == 0) { "Invalid FlvHeader. Expected PreviousTagSize0 to be 0." }
        }

        return block(source)
    }

    /**
     * Decodes the FLV header.
     *
     * @return the decoded [FLVHeader]
     */
    fun decodeFlvHeader(): FLVHeader {
        val peek = source.peek()
        val isHeader = try {
            peek.readString(3) == "FLV"
        } catch (e: Exception) {
            false
        }
        if (isHeader) {
            return FLVHeader.decode(source)
        } else {
            throw IllegalStateException("Not a FLV header")
        }
    }

    /**
     * Closes the demuxer and releases any resources.
     */
    fun close() {
        source.close()
    }
}

/**
 * Decodes all the FLV tags from the source.
 *
 * @param block the block to execute for each FLV tag
 */
suspend fun FLVDemuxer.decodeAll(block: suspend (FLVTag) -> Unit) {
    coroutineScope {
        while (!isEmpty) {
            block(decode())
        }
    }
}

/**
 * Decodes all the raw FLV tags from the source.
 *
 * @param block the block to execute for each raw FLV tag
 */
suspend fun FLVDemuxer.decodeAllRaw(block: suspend (RawFLVTag) -> Unit) {
    coroutineScope {
        while (!isEmpty) {
            block(decodeRaw())
        }
    }
}

