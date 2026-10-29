package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackEntryTest {

    @Test
    fun roundTripsJson() {
        val entry = PlaybackEntry(
            url = "https://example.com/movie.mp4",
            name = "Movie",
            logo = "https://example.com/poster.jpg",
            group = "Películas",
            positionMs = 120_000,
            durationMs = 7_200_000,
            updatedAt = 1_000,
        )

        assertEquals(entry, PlaybackEntry.fromJson(entry.toJson()))
    }

    @Test
    fun resumableOnlyAfterTenSecondsAndBeforeTheEnd() {
        assertFalse(entry(position = 5_000, duration = 100_000).isResumable)
        assertTrue(entry(position = 20_000, duration = 100_000).isResumable)
        assertFalse(entry(position = 90_000, duration = 100_000).isResumable)
    }

    @Test
    fun fromProgressRejectsLiveContent() {
        val live = Channel(name = "Live", url = "https://example.com/live.m3u8", isVod = false)

        assertNull(PlaybackEntry.fromProgress(live, 30_000, 60_000))
    }

    @Test
    fun fromProgressKeepsVodChannelFields() {
        val channel = Channel(
            name = "Movie",
            url = "https://example.com/movie.mp4",
            group = "Películas",
            logo = "https://example.com/poster.jpg",
            isVod = true,
        )

        val entry = PlaybackEntry.fromProgress(channel, 30_000, 60_000)!!

        assertEquals(channel.url, entry.url)
        assertEquals(channel.name, entry.name)
        assertEquals(channel.logo, entry.logo)
        assertEquals(channel.group, entry.group)
        assertEquals(30_000, entry.positionMs)
    }

    @Test
    fun rejectsIncompleteJson() {
        assertNull(PlaybackEntry.fromJson(org.json.JSONObject("{}")))
        assertNull(
            PlaybackEntry.fromJson(
                org.json.JSONObject("""{"url":"u","name":"n","positionMs":0,"durationMs":10}"""),
            ),
        )
    }

    private fun entry(position: Long, duration: Long) = PlaybackEntry(
        url = "u",
        name = "n",
        positionMs = position,
        durationMs = duration,
    )
}
