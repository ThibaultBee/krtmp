package io.github.thibaultbee.krtmp.flv.util

/**
 * A way to export the source to a multiple [RawSource].
 * The purpose is to read huge data (for a video frame for example) the latest possible.
 */
internal interface SourceExporter {
    // fun toRawSources(): List<RawSource>
}