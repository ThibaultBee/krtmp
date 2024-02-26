package io.github.thibaultbee.krtmp.flv

import kotlinx.io.files.Path
import org.jcodec.api.FrameGrab
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.model.Picture
import org.jcodec.containers.mp4.MP4Util
import java.io.File
import java.nio.file.Files
import kotlin.test.Test


class FlvMuxerTest {
    /*
    private fun transmuxToFlv(source: Path, destination: Path): FlvMuxer {
        val moov = MP4Util.parseMovie(File(source.toString()))
        moov.tracks[0].stsd
        moov.videoTrack.stsd
        MP4Util.findFirstAtom("stsd", File(source.toString())
        moov.videoTrack.trackHeader
        val grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(File(source.toString())))
        var picture: Picture

        val muxer = FlvMuxer(destination)

        // muxer.addStream(grab.format)

        while (grab.getNativeFrame().also { picture = it } != null) {
            //        muxer.write()
        }

        return muxer
    }

    @Test
    fun `test write video file`() {
        val source = Resource("video/558k.mp4").toPath()
        val destination = Path(Files.createTempFile("", ".tmp").toString())
        val muxer = transmuxToFlv(source, destination)

    }*/
}