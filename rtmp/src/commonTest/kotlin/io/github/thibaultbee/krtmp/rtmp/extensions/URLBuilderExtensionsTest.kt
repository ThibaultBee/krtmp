package io.github.thibaultbee.krtmp.rtmp.extensions

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.fail

class URLBuilderExtensionsTest {
    @Test
    fun `test valid RTMP URL`() {
        try {
            URLBuilder("rtmp://host:1234/app/stream").validateRtmp()
            URLBuilder("rtmp://host/app/stream").validateRtmp()
            URLBuilder("rtmp://192.168.1.12:1234/app/stream").validateRtmp()
            URLBuilder("rtmp://192.168.1.12/app/stream").validateRtmp()
            URLBuilder("rtmp://192.168.1.12/app/app2/stream").validateRtmp()
        } catch (e: Exception) {
            fail("Exception thrown: ${e.message}", e)
        }
    }

    @Test
    fun `test invalid RTMP URL`() {
        try {
            URLBuilder("rtmp://host:1234/app").validateRtmp()
            fail("Exception must be thrown for missing stream key")
        } catch (_: Exception) {
        }
        try {
            URLBuilder("rtmp://host:1234/app/stream/").validateRtmp()
            fail("Exception must be thrown for trailing slash")
        } catch (_: Exception) {
        }
        try {
            URLBuilder("rtmpz://host:1234/app/stream").validateRtmp()
            fail("Exception must be thrown for invalid protocol")
        } catch (_: Exception) {
        }
        try {
            URLBuilder("rtmp://host:port/app/stream").validateRtmp()
            fail("Exception must be thrown for invalid port")
        } catch (_: Exception) {
        }
    }
}