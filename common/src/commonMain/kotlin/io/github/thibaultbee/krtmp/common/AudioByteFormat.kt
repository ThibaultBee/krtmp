package io.github.thibaultbee.krtmp.common

enum class AudioByteFormat(val numOfBytes: Int) {
    S_8(1),
    U_8(1),
    S_16(2),
    U_16(2),
    S_24(3),
    U_24(3),
    S_32(4),
    U_32(4),
    FLOAT(4),
    DOUBLE(8)
}

