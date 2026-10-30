package com.andres3336.streaming.data.xtream

data class XtreamAccount(
    val authenticated: Boolean,
    val status: String,
)

data class XtreamCategory(
    val id: String,
    val name: String,
)

data class XtreamLiveStream(
    val id: String,
    val name: String,
    val logo: String?,
    val categoryId: String,
    val epgChannelId: String?,
    val language: String? = null,
)

data class XtreamMovie(
    val id: String,
    val name: String,
    val logo: String?,
    val categoryId: String,
    val extension: String,
    val plot: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val duration: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val backdrop: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
)

data class XtreamSeries(
    val id: String,
    val name: String,
    val cover: String?,
    val categoryId: String,
    val plot: String?,
    val year: String? = null,
    val rating: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val backdrop: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
)

data class XtreamEpisode(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val extension: String,
)

data class XtreamSeason(
    val number: Int,
    val episodes: List<XtreamEpisode>,
)

data class XtreamSeriesInfo(
    val seasons: List<XtreamSeason>,
    val plot: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val genre: String? = null,
    val language: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val backdrop: String? = null,
    val imdbId: String? = null,
    val tmdbId: String? = null,
)
