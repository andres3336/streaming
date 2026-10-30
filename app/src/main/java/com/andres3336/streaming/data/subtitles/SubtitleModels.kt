package com.andres3336.streaming.data.subtitles

import com.andres3336.streaming.data.Channel

data class MediaDescriptor(
    val key: String,
    val title: String,
    val year: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
) {
    val isEpisode: Boolean
        get() = season != null && episode != null

    companion object {
        fun fromChannel(channel: Channel): MediaDescriptor = MediaDescriptor(
            key = channel.tvgId?.takeIf(String::isNotBlank) ?: channel.url,
            title = channel.seriesTitle ?: channel.name,
            year = channel.year,
            imdbId = channel.imdbId,
            tmdbId = channel.tmdbId,
            season = channel.season,
            episode = channel.episode,
        )
    }
}

data class SubtitleCandidate(
    val fileId: String,
    val title: String,
    val language: String,
    val downloads: Int,
    val rating: Double,
    val release: String?,
    val hearingImpaired: Boolean,
)

data class SubtitleFile(
    val language: String,
    val content: String,
    val candidate: SubtitleCandidate,
)
