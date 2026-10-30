package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LastPlaybackTest {

    @Test
    fun roundTripsThroughJson() {
        val channel = Channel(
            name = "Canal",
            url = "https://example.com/live.m3u8",
            group = "TV",
            tvgId = "canal-1",
            isVod = false,
        )
        val last = LastPlayback.fromChannel(channel)
        val restored = LastPlayback.fromJson(last.toJson())

        assertEquals(true, restored?.isLive)
        assertEquals("canal-1", restored?.key)
        assertEquals("Canal", restored?.toChannel()?.name)
        assertFalse(restored!!.toChannel().isVod)
    }

    @Test
    fun keepsVodPositionMarker() {
        val channel = Channel(
            name = "Peli",
            url = "https://example.com/movie.mp4",
            isVod = true,
        )
        val last = LastPlayback.fromChannel(channel)

        assertFalse(last.isLive)
        assertTrue(last.toChannel().isVod)
    }
}
