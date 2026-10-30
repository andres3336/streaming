package com.andres3336.streaming.data.media

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {

    @Test
    fun liveChannelHasNoSeekOrSpeed() {
        val item = Channel(name = "TV", url = "https://example.com/live.m3u8").toMediaItem()

        assertTrue(item.kind == MediaKind.LIVE)
        assertTrue(item.capabilities.isLive)
        assertFalse(item.capabilities.canSeek)
        assertFalse(item.capabilities.canChangeSpeed)
        assertFalse(item.capabilities.canOpenExternally)
        assertTrue(item.capabilities.hasEpg)
    }

    @Test
    fun movieCanSeekAndOpenExternally() {
        val item = Channel(
            name = "Peli",
            url = "https://example.com/movie.mp4",
            isVod = true,
        ).toMediaItem()

        assertTrue(item.kind == MediaKind.MOVIE)
        assertTrue(item.capabilities.canSeek)
        assertTrue(item.capabilities.canChangeSpeed)
        assertTrue(item.capabilities.canOpenExternally)
    }

    @Test
    fun episodeHasEpisodeCapability() {
        val item = Channel(
            name = "Serie T1E2",
            url = "https://example.com/episode.mp4",
            isVod = true,
            season = 1,
            episode = 2,
            seriesTitle = "Serie",
        ).toMediaItem()

        assertTrue(item.kind == MediaKind.SERIES_EPISODE)
        assertTrue(item.capabilities.hasEpisodes)
        assertTrue(item.capabilities.canSeek)
    }
}
