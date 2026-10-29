package com.andres3336.streaming.data.xtream

data class XtreamAccount(
    val authenticated: Boolean,
    val status: String,
)

data class XtreamCategory(
    val id: String,
    val name: String,
)

data class XtreamMovie(
    val id: String,
    val name: String,
    val logo: String?,
    val categoryId: String,
    val extension: String,
)

data class XtreamSeries(
    val id: String,
    val name: String,
    val cover: String?,
    val categoryId: String,
    val plot: String?,
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
)
