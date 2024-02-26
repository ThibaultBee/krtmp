package io.github.thibaultbee.krtmp.common

/**
 * FourCC object
 *
 * Just AV1, VP9 and HEVC are described because these are the only FourCC required for enhanced RTMP.
 */

enum class FourCCs(val value: FourCC) {
    AV1(
        FourCC(
            'a', 'v', '0', '1', MimeType.VIDEO_AV1
        )
    ),
    VP9(FourCC('v', 'p', '0', '9', MimeType.VIDEO_VP9)),
    HEVC(FourCC('h', 'v', 'c', '1', MimeType.VIDEO_HEVC));

    companion object {
        fun mimeTypeOf(mimeType: MimeType) =
            entries.first { it.value.mimeType == mimeType }

        fun mimeTypeOf(mimeType: String) =
            entries.first { it.value.mimeType.value == mimeType }

        fun codeOf(value: Int) = entries.first { it.value.code == value }
    }
}

/**
 * FourCC is a 4 bytes code used to identify a codec.
 */
data class FourCC(val a: Char, val b: Char, val c: Char, val d: Char, val mimeType: MimeType) {

    /**
     * FourCC code
     */
    val code = (a.code shl 24) or (b.code shl 16) or (c.code shl 8) or d.code

    override fun toString(): String {
        return "$a$b$c$d"
    }
}