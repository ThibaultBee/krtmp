# krtmp: A RTMP/FLV/AMF library for Kotlin Multiplatform and Android

# RTMP

A RTMP client and server (soon) library for Kotlin Multiplatform.

Features:

- [x] RTMP publish client
- [ ] RTMP play client
- [ ] RTMP play2 client
- [ ] RTMP server
- [ ] Statistics

Supported protocols:

- [x] RTMP
- [x] RTMPS
- [x] RTMPT (partially)

## Installation

Adds the following dependency to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:rtmp:1.0.0")
```

## Usage

Creates a RTMP publish client with the Factory `RtmpClientConnectionFactory`:

```kotlin
val client = RtmpPublishClientConnectionFactory().create(
    "rtmp://my.server.com/app/streamkey" // Your RTMP server URL (incl app name and stream key)
)
```

Then prepare your live by sending these messages to the server:

```kotlin
client.connect()
client.createStream()
client.publish(Command.Publish.Type.LIVE)
```

If you have raw audio and video frames, you need to mux them into FLV tag headers. You can use
the `FlvMuxer` class for that.

```kotlin
val flvMuxer = client.flvMuxer
```

See [FLV](#flv) for more details to write audio and video frames..

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

For advanced configuration, see `RtmpClientSettings`.

# FLV

A muxer/demuxer for FLV.

Features:

- [x] Muxer for FLV
- [x] Demuxer for FLV
- [x] AMF0 metadata
- [ ] AMF3 metadata
- [x] Supported audio codec: AAC
- [x] Supported video codec: AVC/H.264 and enhanced RTMP codecs: HEVC/H.265, VP9, AV1

## Installation

Adds the following dependencies to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:flv:1.0.0")
```

## Usage

Creates a FLV muxer and add audio/video data:

```kotlin
val muxer = FlvMuxer()

// Register audio configurations (if any)
val audioConfig = FlvAudioConfig(
    FlvAudioConfig.SoundFormat.AAC,
    FlvAudioConfig.SoundRate.KHZ44,
    FlvAudioConfig.SoundSize.SND8BIT,
    FlvAudioConfig.SoundType.STEREO
)
val audioId = muxer.addStream(audioConfig)

// Register audio configurations (if any)
val videoConfig = FlvVideoConfig(

)
val videoId = muxer.addStream(videoConfig)

// Start the muxer (write FlvTag (if needed) and onMetaData)
muxer.startStream()

// Write audio/video data
muxer.write(audioFrame)
muxer.write(videoFrame)
muxer.write(audioFrame)
muxer.write(videoFrame)
// till you're done

// Stop the muxer
muxer.stopStream()

```

# AMF

A serializer/deserializer for AMF0 and AMF3.

Features:

- [x] Serializer for AMF0
- [ ] Serializer for AMF3
- [ ] Deserializer for AMF0
- [ ] Deserializer for AMF3

## Installation

It requires `kotlinx.serialization` library.
See [Setup](https://github.com/Kotlin/kotlinx.serialization?tab=readme-ov-file#setup) for more
details.
Then, adds the following dependencies to your project:

```kotlin
implementation("io.github.thibaultbee.krtmp:amf:1.0.0")
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

- [x] A FLV/RTMP parameter for supported level: (legacy, enhanced v1, enhanced v2,...)

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