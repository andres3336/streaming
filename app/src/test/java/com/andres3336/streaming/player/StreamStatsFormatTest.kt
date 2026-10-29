package com.andres3336.streaming.player

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamStatsFormatTest {

    @Test
    fun formatsBitrateAtDifferentScales() {
        assertEquals("—", StreamStatsFormat.bitrate(0))
        assertEquals("800 bps", StreamStatsFormat.bitrate(800))
        assertEquals("500 kbps", StreamStatsFormat.bitrate(500_000))
        assertEquals("1.5 Mbps", StreamStatsFormat.bitrate(1_500_000))
    }

    @Test
    fun formatsResolutionAndFrameRate() {
        assertEquals("1920 × 1080", StreamStatsFormat.resolution(1920, 1080))
        assertEquals("—", StreamStatsFormat.resolution(0, 0))
        assertEquals("50.0 fps", StreamStatsFormat.frameRate(50f))
        assertEquals("—", StreamStatsFormat.frameRate(0f))
    }

    @Test
    fun formatsBufferAndDroppedFrames() {
        assertEquals("3.5 s", StreamStatsFormat.buffer(3_500))
        assertEquals("—", StreamStatsFormat.buffer(0))
        assertEquals("12", StreamStatsFormat.droppedFrames(12))
        assertEquals("—", StreamStatsFormat.droppedFrames(-1))
    }

    @Test
    fun masksEverythingExceptTheHost() {
        val url = "https://user:pass@example.com:8443/live/secret-token/index.m3u8?token=abc"

        assertEquals("example.com", StreamStatsFormat.maskedSource(url))
        assertEquals("oculta", StreamStatsFormat.maskedSource("not a url"))
        assertEquals("oculta", StreamStatsFormat.maskedSource(""))
    }

    @Test
    fun labelsKnownVideoCodecs() {
        assertEquals("H.264 · avc1.64001f", StreamStatsFormat.codec("video/avc", "avc1.64001f"))
        assertEquals("H.265", StreamStatsFormat.codec("video/hevc", null))
        assertEquals("av01.0.08M.08", StreamStatsFormat.codec("video/custom", "av01.0.08M.08"))
        assertEquals("—", StreamStatsFormat.codec(null, null))
    }
}
