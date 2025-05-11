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
package io.github.thibaultbee.krtmp.flv.tags

import io.github.thibaultbee.krtmp.amf.AmfVersion
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.readByteArray

/**
 * Interface representing a frame data in FLV format.
 */
interface FLVData {
    /**
     * Gets the size of the data in bytes.
     *
     * @param amfVersion The AMF version to use for size calculation. Only for [ScriptDataObject].
     * @return The size of the data in bytes.
     */
    fun getSize(amfVersion: AmfVersion): Int

    /**
     * Encodes the data into the specified output stream.
     *
     * @param output The output stream to write the encoded data to.
     * @param amfVersion The AMF version to use for encoding. Only for [ScriptDataObject].
     * @param isEncrypted Indicates whether the data is encrypted or not.
     */
    fun encode(output: Sink, amfVersion: AmfVersion, isEncrypted: Boolean)

    /**
     * Reads the raw source of the data, including its size.
     *
     * A special API that avoid copying large data.
     *
     * @param amfVersion The AMF version to use for encoding. Only for [ScriptDataObject].
     * @param isEncrypted Indicates whether the data is encrypted or not.
     */
    fun readRawSource(amfVersion: AmfVersion, isEncrypted: Boolean): RawSource
}

/**
 * Reads the tag as a [Buffer].
 *
 * @param amfVersion the AMF version to use for encoding
 * @return the [Buffer] containing the tag
 */
fun FLVData.readBuffer(amfVersion: AmfVersion = AmfVersion.AMF0): Buffer {
    return Buffer().apply { encode(this, amfVersion, false) }
}

/**
 * Reads the tag as a [ByteArray].
 *
 * @param amfVersion the AMF version to use for encoding
 * @return the [ByteArray] containing the tag
 */
fun FLVData.readByteArray(amfVersion: AmfVersion = AmfVersion.AMF0): ByteArray {
    return readBuffer(amfVersion).readByteArray()
}