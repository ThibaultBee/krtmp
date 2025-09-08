# krtmp: A RTMP, FLV and AMF library for Kotlin Multiplatform and Android

krtmp is a Kotlin Multiplatform implementation of RTMP, FLV and AMF protocols from legacy RTMP to
enhanced RTMP v2.

# Notice

krtmp is still in development and is not ready for production use. There is no guarantee that the
API will not change in the future.

To finalize the release, I need to add more tests, fixes and feedbacks. If you want to help, feel
free to give feedbacks, report issues or contribute to the project.

TODO:

- Fixes:
    - See https://youtrack.jetbrains.com/issue/KTOR-8642#focus=Comments-27-12303807.0-0

# RTMP

A RTMP client and server library for Kotlin Multiplatform.

Features:

- [x] RTMP client
- [x] RTMP server
- [ ] Statistics
- [x] Support for legacy RTMP
- [x] Support for enhanced RTMP v2 (partial)

Supported protocols:

- [x] RTMP
- [x] RTMPS
- [x] RTMPT (not tested)

## Installation

Adds the following dependency to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:rtmp:0.9.0")
```

## Usage

### Client

Use `RtmpConnectionBuilder` to create a RTMP client:

```kotlin
val client = RtmpConnectionBuilder.connect(
    "rtmp://my.server.com/app/streamkey" // Your RTMP server URL (incl app name and stream key)
)
```

Then prepare your live by sending these messages to the server:

```kotlin
client.createStream() // Send createStream message
client.publish(StreamPublishType.LIVE) // Send publish message
```

If you already have FLV data, write your video/audio data:

```kotlin
try {
    // Write metadata
    val metadata = OnMetadata.Metadata(...)
    client.writeSetDataFrame(metadata)

    while (true) {
        // Write audio data. `audioData` are in `AudioTagHeader` format. See FLV specification for more details.
        client.writeAudio(audioTimestamp, audioData)
        // Write video data. `videoData` are in `VideoTagHeader` format. See FLV specification for more details.
        client.writeVideo(videoTimestamp, videoData)
    }
} catch (e: Exception) {
    // Handle exception
}
```

See [FLV](#flv) for more details to write audio and video frames..

### Server

Use `RtmpConnectionBuilder` to create a RTMP server:

```kotlin
val server = RtmpConnectionBuilder.bind("0.0.0.0:1935") // Listening on port 1935
```

Then start the server:

```kotlin
server.listen()
```

# FLV

A muxer/demuxer for FLV.

Features:

- [x] Muxer for FLV
- [x] Demuxer for FLV
- [x] AMF0 metadata
- [ ] AMF3 metadata
- [x] Support for legacy RTMP
- [x] Support for enhanced RTMP v1: AV1, HEVC, VP8, VP9
- [x] Support for enhanced RTMP v2: Multitrack, Opus,...

## Installation

Adds the following dependencies to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:flv:0.9.0")
```

## Usage

Creates a FLV muxer and add audio/video data:

```kotlin
val muxer = FLVMuxer(path = "/path/to/file.flv")

// Write FLV header
flvMuxer.encodeFlvHeader(hasAudio, hasVideo)

// Register audio configurations (if any)
val audioConfig = FLVAudioConfig(
    FlvAudioConfig.SoundFormat.AAC,
    FlvAudioConfig.SoundRate.KHZ44,
    FlvAudioConfig.SoundSize.SND8BIT,
    FlvAudioConfig.SoundType.STEREO
)
// Register video configurations (if any)
val videoConfig = FLVVideoConfig(
)

// Write onMetadata
muxer.encode(0, OnMetadata(audioConfig, videoConfig))

// Write audio/video data
muxer.encode(audioFrame)
muxer.encode(videoFrame)
muxer.encode(audioFrame)
muxer.encode(videoFrame)

// Till you're done, then
muxer.flush()

// Close the output
muxer.close()
```

# AMF

A serializer/deserializer for AMF0 and AMF3.

Features:

- [x] Serializer for AMF0
- [ ] Serializer for AMF3
- [x] Deserializer for AMF0
- [ ] Deserializer for AMF3

## Installation

It requires `kotlinx.serialization` library.
See [Setup](https://github.com/Kotlin/kotlinx.serialization?tab=readme-ov-file#setup) for more
details.
Then, adds the following dependencies to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:amf:0.9.0")
```

## Usage

Creates a class and make it serializable with `@Serializable` annotation:

```kotlin
@Serializable
class MyData(val a: Int, val b: String)
```

Then you can serialize it to AMF0:

```kotlin
val data = MyData(1, "Hello")
val array = Amf.encodeToByteArray(MyData.serializer(), data)
```

# TODO

- [ ] More tests (missing tests samples)

# Licence

    Copyright 2023 Thibault B.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.