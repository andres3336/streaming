package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackRequestTest {

    private val first = Channel(name = "E1", url = "https://example.com/e1.mp4", isVod = true)
    private val second = Channel(name = "E2", url = "https://example.com/e2.mp4", isVod = true)
    private val third = Channel(name = "E3", url = "https://example.com/e3.mp4", isVod = true)

    @Test
    fun returnsNextEpisode() {
        val request = PlaybackRequest(first, listOf(first, second, third), index = 0)

        assertEquals(second, request.next)
        assertEquals(1, request.nextRequest()?.index)
        assertEquals(second, request.nextRequest()?.channel)
    }

    @Test
    fun lastEpisodeHasNoNext() {
        val request = PlaybackRequest(third, listOf(first, second, third), index = 2)

        assertNull(request.next)
        assertNull(request.nextRequest())
    }

    @Test
    fun movieWithoutQueueHasNoNext() {
        val request = PlaybackRequest(first)

        assertNull(request.next)
        assertNull(request.nextRequest())
    }

    @Test
    fun nextRequestStartsFromTheBeginning() {
        val request = PlaybackRequest(first, listOf(first, second), index = 0)

        assertEquals(0L, request.nextRequest()?.startPositionMs)
    }
}
