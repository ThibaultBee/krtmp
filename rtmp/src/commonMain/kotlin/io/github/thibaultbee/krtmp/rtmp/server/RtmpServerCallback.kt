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
package io.github.thibaultbee.krtmp.rtmp.server

import io.github.thibaultbee.krtmp.rtmp.messages.Audio
import io.github.thibaultbee.krtmp.rtmp.messages.Command
import io.github.thibaultbee.krtmp.rtmp.messages.DataAmf
import io.github.thibaultbee.krtmp.rtmp.messages.Message
import io.github.thibaultbee.krtmp.rtmp.messages.Video


/**
 * Callback interface for RTMP server events.
 */
interface RtmpServerCallback {
    /**
     * Called when a new client connects to the server.
     */
    fun onConnect(connect: Command) = Unit

    /**
     * Called when a new stream is created.
     *
     * @param createStream the createStream command received from the client
     */
    fun onCreateStream(createStream: Command) = Unit

    /**
     * Called when a stream is released.
     *
     * @param releaseStream the releaseStream command received from the client
     */
    fun onReleaseStream(releaseStream: Command) = Unit

    /**
     * Called when a stream is deleted.
     *
     * @param deleteStream the deleteStream command received from the client
     */
    fun onDeleteStream(deleteStream: Command) = Unit

    /**
     * Called when a stream is published.
     *
     * @param publish the publish command received from the client
     */
    fun onPublish(publish: Command) = Unit

    /**
     * Called when a stream is played.
     *
     * @param play the play command received from the client
     */
    fun onPlay(play: Command) = Unit

    /**
     * Called when a stream is FCPublished.
     *
     * @param fcPublish the FCPublish command received from the client
     */
    fun onFCPublish(fcPublish: Command) = Unit

    /**
     * Called when a stream is FCUnpublished.
     *
     * @param fcUnpublish the FCUnpublish command received from the client
     */
    fun onFCUnpublish(fcUnpublish: Command) = Unit

    /**
     * Called when a stream is closed.
     *
     * @param closeStream the closeStream command received from the client
     */
    fun onCloseStream(closeStream: Command) = Unit

    /**
     * Called when a set data frame is received.
     *
     * @param setDataFrame the setDataFrame message received from the client
     */
    fun onSetDataFrame(setDataFrame: DataAmf) = Unit

    /**
     * Called when an audio message is received.
     *
     * @param audio the audio message received from the client
     */
    fun onAudio(audio: Audio) = Unit

    /**
     * Called when a video message is received.
     *
     * @param video the video message received from the client
     */
    fun onVideo(video: Video) = Unit

    /**
     * Called when an unknown message is received.
     *
     * @param message the unknown message received from the client
     */
    fun onUnknownMessage(message: Message) = Unit

    /**
     * Called when an unknown command message is received.
     *
     * @param command the unknown command message received from the client
     */
    fun onUnknownCommandMessage(command: Command) = Unit

    /**
     * Called when an unknown data message is received.
     *
     * @param data the unknown data message received from the client
     */
    fun onUnknownDataMessage(data: DataAmf) = Unit
}

/**
 * Builder class for creating [RtmpServerCallback] instances with lambda functions.
 */
class RtmpServerCallbackBuilder : RtmpServerCallback {
    private var connectBlock: Command.() -> Unit = {}
    private var createStreamBlock: Command.() -> Unit = {}
    private var releaseStreamBlock: Command.() -> Unit = {}
    private var deleteStreamBlock: Command.() -> Unit = {}
    private var publishBlock: Command.() -> Unit = {}
    private var playBlock: Command.() -> Unit = {}
    private var fcPublishBlock: Command.() -> Unit = {}
    private var fcUnpublishBlock: Command.() -> Unit = {}
    private var closeStreamBlock: Command.() -> Unit = {}
    private var setDataFrameBlock: DataAmf.() -> Unit = {}
    private var audioBlock: Audio.() -> Unit = {}
    private var videoBlock: Video.() -> Unit = {}
    private var unknownMessageBlock: Message.() -> Unit = {}
    private var unknownCommandMessageBlock: Command.() -> Unit = {}
    private var unknownDataMessageBlock: DataAmf.() -> Unit = {}

    fun connect(block: Command.() -> Unit) {
        connectBlock = block
    }

    fun createStream(block: Command.() -> Unit) {
        createStreamBlock = block
    }

    fun releaseStream(block: Command.() -> Unit) {
        releaseStreamBlock = block
    }

    fun deleteStream(block: Command.() -> Unit) {
        deleteStreamBlock = block
    }

    fun publish(block: Command.() -> Unit) {
        publishBlock = block
    }

    fun play(block: Command.() -> Unit) {
        playBlock = block
    }

    fun fcPublish(block: Command.() -> Unit) {
        fcPublishBlock = block
    }

    fun fcUnpublish(block: Command.() -> Unit) {
        fcUnpublishBlock = block
    }

    fun closeStream(block: Command.() -> Unit) {
        closeStreamBlock = block
    }

    fun setDataFrame(block: DataAmf.() -> Unit) {
        setDataFrameBlock = block
    }

    fun audio(block: Audio.() -> Unit) {
        audioBlock = block
    }

    fun video(block: Video.() -> Unit) {
        videoBlock = block
    }

    fun unknownMessage(block: Message.() -> Unit) {
        unknownMessageBlock = block
    }

    fun unknownCommandMessage(block: Command.() -> Unit) {
        unknownCommandMessageBlock = block
    }

    fun unknownDataMessage(block: DataAmf.() -> Unit) {
        unknownDataMessageBlock = block
    }

    override fun onConnect(connect: Command) = connect.connectBlock()
    override fun onCreateStream(createStream: Command) = createStream.createStreamBlock()
    override fun onReleaseStream(releaseStream: Command) = releaseStream.releaseStreamBlock()
    override fun onDeleteStream(deleteStream: Command) = deleteStream.deleteStreamBlock()
    override fun onPublish(publish: Command) = publish.publishBlock()
    override fun onPlay(play: Command) = play.playBlock()
    override fun onFCPublish(fcPublish: Command) = fcPublish.fcPublishBlock()
    override fun onFCUnpublish(fcUnpublish: Command) = fcUnpublish.fcUnpublishBlock()
    override fun onCloseStream(closeStream: Command) = closeStream.closeStreamBlock()
    override fun onSetDataFrame(setDataFrame: DataAmf) = setDataFrame.setDataFrameBlock()
    override fun onAudio(audio: Audio) = audio.audioBlock()
    override fun onVideo(video: Video) = video.videoBlock()
    override fun onUnknownMessage(message: Message) = message.unknownMessageBlock()
    override fun onUnknownCommandMessage(command: Command) =
        command.unknownCommandMessageBlock()

    override fun onUnknownDataMessage(data: DataAmf) = data.unknownDataMessageBlock()
}