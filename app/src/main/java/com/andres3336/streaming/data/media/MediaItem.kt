package com.andres3336.streaming.data.media

import com.andres3336.streaming.data.Channel

enum class MediaKind {
    LIVE,
    MOVIE,
    SERIES_EPISODE,
}

data class MediaItem(
    val id: String,
    val kind: MediaKind,
    val title: String,
    val streamUrl: String,
    val group: String? = null,
    val artworkUrl: String? = null,
    val provider: String? = null,
    val durationMs: Long? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val year: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
) {
    val capabilities: MediaCapabilities
        get() = MediaCapabilities(
            isLive = kind == MediaKind.LIVE,
            canSeek = kind != MediaKind.LIVE,
            canPause = true,
            hasEpisodes = kind == MediaKind.SERIES_EPISODE,
            canChangeQuality = true,
            canChangeSpeed = kind != MediaKind.LIVE,
            canOpenExternally = kind != MediaKind.LIVE,
        )
}

data class MediaCapabilities(
    val isLive: Boolean,
    val canSeek: Boolean,
    val canPause: Boolean,
    val hasEpisodes: Boolean,
    val canChangeQuality: Boolean,
    val canChangeSpeed: Boolean,
    val canOpenExternally: Boolean,
)

fun Channel.toMediaItem(): MediaItem {
    val kind = when {
        !isVod -> MediaKind.LIVE
        season != null || seriesTitle != null -> MediaKind.SERIES_EPISODE
        else -> MediaKind.MOVIE
    }
    return MediaItem(
        id = tvgId?.takeIf(String::isNotBlank) ?: url,
        kind = kind,
        title = name,
        streamUrl = url,
        group = group,
        artworkUrl = logo,
        provider = provider,
        season = season,
        episode = episode,
        year = year,
        imdbId = imdbId,
        tmdbId = tmdbId,
    )
}
