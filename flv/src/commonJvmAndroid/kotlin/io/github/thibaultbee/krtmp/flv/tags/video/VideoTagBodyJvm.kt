package io.github.thibaultbee.krtmp.flv.tags.video

import io.github.thibaultbee.krtmp.flv.sources.ByteBufferBackedRawSource
import java.nio.ByteBuffer

/**
 * Extensions to create [VideoTagBody] from [ByteBuffer]s.
 */

/**
 * Creates a [RawVideoTagBody] from a [ByteBuffer].
 *
 * @param body the coded [ByteBuffer]
 * @return the [RawVideoTagBody]
 */
fun RawVideoTagBody(body: ByteBuffer) =
    RawVideoTagBody(ByteBufferBackedRawSource(body), body.remaining())