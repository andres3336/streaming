package com.andres3336.streaming.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HlsFallbackTest {

    @Test
    fun rewritesM3u8AndKeepsTheQuery() {
        assertEquals(
            "https://example.com/live/channel.ts",
            HlsFallback.tsUrl("https://example.com/live/channel.m3u8"),
        )
        assertEquals(
            "https://example.com/live/channel.ts?token=abc",
            HlsFallback.tsUrl("https://example.com/live/channel.m3u8?token=abc"),
        )
    }

    @Test
    fun leavesNonHlsUrlsAlone() {
        assertNull(HlsFallback.tsUrl("https://example.com/live/channel.ts"))
        assertNull(HlsFallback.tsUrl("https://example.com/movie.mp4"))
        assertNull(HlsFallback.tsUrl(""))
    }
}
