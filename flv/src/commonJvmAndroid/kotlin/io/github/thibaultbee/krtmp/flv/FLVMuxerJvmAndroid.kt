package io.github.thibaultbee.krtmp.flv

import io.github.thibaultbee.krtmp.amf.AmfVersion
import kotlinx.io.asSink
import kotlinx.io.buffered
import java.io.OutputStream

/**
 * Creates a [FLVMuxer] dedicated to write to an [OutputStream].
 *
 * @param outputStream the output stream to write to
 * @param amfVersion the AMF version to use
 * @return a [FLVMuxer]
 */
fun FLVMuxer(
    outputStream: OutputStream,
    amfVersion: AmfVersion = AmfVersion.AMF0
) = FLVMuxer(outputStream.asSink().buffered(), amfVersion)