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