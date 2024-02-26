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
package io.github.thibaultbee.krtmp.amf.elements

import io.github.thibaultbee.krtmp.amf.AmfVersion
import kotlinx.io.Buffer
import kotlinx.io.Sink

abstract class AmfElement {
    /**
     * Get AMF0 element size in bytes.
     *
     * @return size in bytes
     */
    abstract val size0: Int

    /**
     * Get AMF3 element size in bytes.
     *
     * @return size in bytes
     */
    abstract val size3: Int

    /**
     * Get AMF element size in bytes.
     *
     * @param version AMF version
     * @return size in bytes
     */
    fun getSize(version: AmfVersion): Int {
        return when (version) {
            AmfVersion.AMF0 -> size0
            AmfVersion.AMF3 -> size3
        }
    }

    /**
     * Write AMF0 element to buffer.
     */
    abstract fun write0(sink: Sink)

    /**
     * Write AMF3 element to buffer.
     */
    abstract fun write3(sink: Sink)

    /**
     * Write AMF element to buffer.
     *
     * AMF version is defined by [version].
     */
    fun write(version: AmfVersion, sink: Sink) {
        when (version) {
            AmfVersion.AMF0 -> write0(sink)
            AmfVersion.AMF3 -> write3(sink)
        }
    }

    /**
     * Creates a buffer and write AMF element to it.
     *
     * AMF version is defined by [version].
     */
    fun write(version: AmfVersion) = Buffer().apply {
        write(version, this)
    }
}