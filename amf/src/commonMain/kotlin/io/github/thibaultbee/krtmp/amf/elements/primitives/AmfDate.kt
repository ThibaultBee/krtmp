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
package io.github.thibaultbee.krtmp.amf.elements.primitives

import io.github.thibaultbee.krtmp.amf.elements.Amf0Type
import io.github.thibaultbee.krtmp.amf.elements.AmfElement
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readDouble
import kotlinx.io.writeDouble
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun amf0DateFrom(source: Source): AmfDate {
    val type = source.readByte()
    require(type == Amf0Type.DATE.value) { "Amf0Date cannot read buffer because it's not DATE type" }
    return AmfDate(source.readDouble().toLong(), source.readShort())
}

@OptIn(ExperimentalTime::class)
fun amfDateOf(instant: Instant): AmfDate {
    return AmfDate(instant.toEpochMilliseconds(), 0)
}

class AmfDate(val dateMs: Long, val timeZone: Short) : AmfElement() {
    override val size0 = 9

    override val size3: Int
        get() {
            TODO("Not yet implemented")
        }

    override fun write0(sink: Sink) {
        sink.writeByte(Amf0Type.DATE.value)
        sink.writeDouble(dateMs.toDouble())
        sink.writeShort(timeZone)
    }

    override fun write3(sink: Sink) {
        TODO("Not yet implemented")
    }

    override fun toString() = "Date(dateMs=$dateMs, timeZone=$timeZone)"
}