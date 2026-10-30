package com.andres3336.streaming.data

data class Channel(
    val name: String,
    val url: String,
    val group: String? = null,
    val logo: String? = null,
    val tvgId: String? = null,
    val language: String? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val isVod: Boolean = false,
    val fallbackUrl: String? = null,
    val provider: String? = null,
    val year: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val seriesTitle: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
)
