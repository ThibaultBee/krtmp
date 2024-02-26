package io.github.thibaultbee.krtmp.common

enum class MimeType(val value: String, val type: Type) {
    // Audio
    AUDIO_RAW("audio/raw", Type.AUDIO),
    AUDIO_AAC("audio/mp4a-latm", Type.AUDIO),
    AUDIO_MP3("audio/mpeg", Type.AUDIO),
    AUDIO_OPUS("audio/opus", Type.AUDIO),
    AUDIO_VORBIS("audio/vorbis", Type.AUDIO),
    AUDIO_G711A("audio/g711-alaw", Type.AUDIO),
    AUDIO_G711U("audio/g711-ulaw", Type.AUDIO),

    // Video
    VIDEO_RAW("video/raw", Type.VIDEO),
    VIDEO_H263("video/3gpp", Type.VIDEO),
    VIDEO_AVC("video/avc", Type.VIDEO),
    VIDEO_HEVC("video/hevc", Type.VIDEO),
    VIDEO_VP8("video/x-vnd.on2.vp8", Type.VIDEO),
    VIDEO_VP9("video/x-vnd.on2.vp9", Type.VIDEO),
    VIDEO_AV1("video/av01", Type.VIDEO);

    enum class Type {
        AUDIO,
        VIDEO
    }
}

