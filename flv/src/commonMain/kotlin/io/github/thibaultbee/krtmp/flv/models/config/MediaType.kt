/*
 * Copyright (C) 2023 Thibault B.
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
package io.github.thibaultbee.krtmp.flv.models.config

enum class MediaType(val value: String, val type: Type) {
    // Audio
    AUDIO_RAW("audio/raw", Type.AUDIO),
    AUDIO_AAC("audio/mp4a-latm", Type.AUDIO),
    AUDIO_MP3("audio/mpeg", Type.AUDIO),
    AUDIO_OPUS("audio/opus", Type.AUDIO),
    AUDIO_VORBIS("audio/vorbis", Type.AUDIO),
    AUDIO_G711A("audio/g711-alaw", Type.AUDIO),
    AUDIO_G711U("audio/g711-ulaw", Type.AUDIO),
    AUDIO_SPEEX("audio/speex", Type.AUDIO),

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

