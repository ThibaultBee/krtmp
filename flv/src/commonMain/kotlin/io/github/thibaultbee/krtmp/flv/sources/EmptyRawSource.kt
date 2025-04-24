package io.github.thibaultbee.krtmp.flv.sources

import kotlinx.io.Buffer
import kotlinx.io.RawSource

class EmptyRawSource : RawSource {
    override fun close() = Unit

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        return 0L
    }
}