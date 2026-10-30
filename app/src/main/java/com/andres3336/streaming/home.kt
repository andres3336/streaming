package com.andres3336.streaming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.epg.EpgGuide
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.playback.PlaybackEntry
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.ui.HeroBlock
import com.andres3336.streaming.ui.MediaCardModel
import com.andres3336.streaming.ui.MediaRow
import com.andres3336.streaming.ui.PosterGrid
import kotlinx.coroutines.delay

@Composable
internal fun HomeScreen(
    channels: List<Channel>,
    liveState: LiveState,
    playbackEntries: List<PlaybackEntry>,
    config: AppConfig,
    refreshVersion: Int,
    onPlay: (PlaybackRequest) -> Unit,
    onOpenSection: (String) -> Unit,
) {
    val xtream = config.sources.xtream
    val client = remember(xtream, config.player.userAgent) {
        xtream?.let { XtreamClient(it, userAgent = config.player.userAgent) }
    }
    var movies by remember(client) { mutableStateOf<List<XtreamMovie>>(emptyList()) }
    var series by remember(client) { mutableStateOf<List<XtreamSeries>>(emptyList()) }

    LaunchedEffect(client, refreshVersion) {
        val source = client ?: return@LaunchedEffect
        val movieCategories = source.vodCategories()
        if (movieCategories is XtreamResult.Success) {
            movieCategories.value.firstOrNull { it.id != FAVORITES_CATEGORY }?.let { category ->
                (source.vodStreams(category.id) as? XtreamResult.Success)?.value?.let {
                    movies = it
                }
            }
        }
        val seriesCategories = source.seriesCategories()
        if (seriesCategories is XtreamResult.Success) {
            seriesCategories.value.firstOrNull()?.let { category ->
                (source.series(category.id) as? XtreamResult.Success)?.value?.let {
                    series = it
                }
            }
        }
    }

    val epgGuide = LocalEpgGuide.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val favorites = remember(liveState.favorites, channels) {
        LiveState.resolve(liveState.favorites, channels)
    }

    val heroEntry = playbackEntries.firstOrNull()
    val heroMovieBase = movies.firstOrNull()
    val heroChannel = channels.firstOrNull {
        epgGuide?.nowNext(it, nowMs)?.current != null
    } ?: channels.firstOrNull()

    var heroInfo by remember(heroMovieBase?.id) { mutableStateOf<XtreamMovie?>(null) }
    LaunchedEffect(heroMovieBase?.id) {
        val source = client ?: return@LaunchedEffect
        val base = heroMovieBase ?: return@LaunchedEffect
        if (heroInfo?.id == base.id) return@LaunchedEffect
        delay(200)
        val result = source.vodInfo(base.id)
        if (result is XtreamResult.Success) heroInfo = result.value
    }
    val heroMovie = heroMovieBase?.let { base ->
        val detail = heroInfo?.takeIf { it.id == base.id } ?: return@let base
        base.copy(
            plot = detail.plot ?: base.plot,
            genre = detail.genre ?: base.genre,
            year = detail.year ?: base.year,
            rating = detail.rating ?: base.rating,
            backdrop = detail.backdrop ?: base.backdrop,
        )
    }

    val hero: HomeHero = when {
        heroEntry != null -> HomeHero(
            title = heroEntry.name,
            badge = "Continuar viendo",
            meta = listOf(
                "${formatDuration(heroEntry.positionMs)} de ${formatDuration(heroEntry.durationMs)}",
                heroEntry.group.orEmpty(),
            ),
            synopsis = null,
            image = heroEntry.logo,
            primary = { onPlay(PlaybackRequest(heroEntry.toChannel(), startPositionMs = heroEntry.positionMs)) },
            secondaryLabel = "Películas",
            secondary = { onOpenSection("movies") },
        )

        heroMovie != null -> HomeHero(
            title = heroMovie.name,
            badge = "Película",
            meta = listOfNotNull(
                yearOnly(heroMovie.year),
                heroMovie.genre,
                heroMovie.rating?.let { "\u2605 $it" },
            ),
            synopsis = heroMovie.plot,
            image = heroMovie.logo,
            primary = { onPlay(PlaybackRequest(heroMovie.toChannel(client!!))) },
            secondaryLabel = "Ver todas",
            secondary = { onOpenSection("movies") },
        )

        heroChannel != null -> {
            val now = epgGuide?.nowNext(heroChannel, nowMs)?.current
            HomeHero(
                title = heroChannel.name,
                badge = "En vivo",
                meta = listOfNotNull(
                    heroChannel.group,
                    now?.let { "Ahora: ${it.title}" },
                    now?.let { EpgGuide.formatTimeLeft(it.stopMs - nowMs) },
                ),
                synopsis = null,
                image = heroChannel.logo,
                primary = {
                    onPlay(
                        PlaybackRequest(
                            channel = heroChannel,
                            queue = channels,
                            index = channels.indexOf(heroChannel).coerceAtLeast(0),
                        ),
                    )
                },
                secondaryLabel = "Canales",
                secondary = { onOpenSection("channels") },
            )
        }

        else -> HomeHero(
            title = "Streaming",
            badge = null,
            meta = emptyList(),
            synopsis = null,
            image = null,
            primary = { onOpenSection("settings") },
            secondaryLabel = null,
            secondary = null,
        )
    }

    val continueCards = remember(playbackEntries) {
        playbackEntries.take(12).mapIndexed { index, entry ->
            MediaCardModel(
                key = "continue:$index",
                title = entry.name,
                subtitle = formatDuration(entry.positionMs),
                image = entry.logo,
                progress = (entry.positionMs.toFloat() / entry.durationMs.toFloat()).coerceIn(0f, 1f),
                badge = "CONTINUAR",
            )
        }
    }
    val favoriteCards = remember(favorites) {
        favorites.take(12).mapIndexed { index, channel ->
            MediaCardModel(
                key = "favorite:$index",
                title = channel.name,
                subtitle = channel.group,
                image = channel.logo,
            )
        }
    }
    val liveCards = remember(channels) {
        channels.take(12).mapIndexed { index, channel ->
            MediaCardModel(
                key = "live:$index",
                title = channel.name,
                subtitle = channel.group,
                image = channel.logo,
            )
        }
    }
    val movieCards = remember(movies) {
        movies.take(12).mapIndexed { index, movie ->
            MediaCardModel(
                key = "movie:$index",
                title = movie.name,
                subtitle = listOfNotNull(yearOnly(movie.year), movie.genre).joinToString(" \u00b7 "),
                image = movie.logo,
                badge = movie.rating?.let { "\u2605 $it" },
            )
        }
    }
    val seriesCards = remember(series) {
        series.take(12).mapIndexed { index, item ->
            MediaCardModel(
                key = "series:$index",
                title = item.name,
                subtitle = listOfNotNull(yearOnly(item.year), item.genre).joinToString(" \u00b7 "),
                image = item.cover,
                badge = item.rating?.let { "\u2605 $it" },
            )
        }
    }

    val onCard: (String) -> Unit = { key ->
        val kind = key.substringBefore(':')
        val index = key.substringAfter(':').toIntOrNull() ?: -1
        when (kind) {
            "continue" -> playbackEntries.getOrNull(index)?.let { entry ->
                onPlay(PlaybackRequest(entry.toChannel(), startPositionMs = entry.positionMs))
            }

            "favorite" -> favorites.getOrNull(index)?.let { channel ->
                onPlay(
                    PlaybackRequest(
                        channel = channel,
                        queue = favorites,
                        index = index,
                    ),
                )
            }

            "live" -> channels.getOrNull(index)?.let { channel ->
                onPlay(PlaybackRequest(channel = channel, queue = channels, index = index))
            }

            "movie" -> {
                val source = client
                if (source != null) {
                    movies.getOrNull(index)?.let { movie ->
                        onPlay(PlaybackRequest(movie.toChannel(source)))
                    }
                }
            }

            "series" -> onOpenSection("series")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item {
            HeroBlock(
                title = hero.title,
                badge = hero.badge,
                meta = hero.meta,
                synopsis = hero.synopsis,
                image = hero.image,
                primaryLabel = if (hero.badge == "Continuar viendo") "Reanudar" else "Reproducir",
                onPrimary = hero.primary,
                secondaryLabel = hero.secondaryLabel,
                onSecondary = hero.secondary,
                height = 250,
            )
        }
        if (continueCards.isNotEmpty()) {
            item { MediaRow(title = "Continuar viendo", items = continueCards, onSelect = onCard) }
        }
        if (favoriteCards.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Favoritos",
                    items = favoriteCards,
                    onSelect = onCard,
                    count = "${favorites.size} canales",
                )
            }
        }
        if (liveCards.isNotEmpty()) {
            item {
                MediaRow(
                    title = "En vivo ahora",
                    items = liveCards,
                    onSelect = onCard,
                    count = "${channels.size} canales",
                )
            }
        }
        if (movieCards.isNotEmpty()) {
            item {
                MediaRow(
                    title = "Películas",
                    items = movieCards,
                    onSelect = onCard,
                )
            }
        }
        if (seriesCards.isNotEmpty()) {
            item { MediaRow(title = "Series", items = seriesCards, onSelect = onCard) }
        }
    }
}

internal data class HomeHero(
    val title: String,
    val badge: String?,
    val meta: List<String>,
    val synopsis: String?,
    val image: String?,
    val primary: () -> Unit,
    val secondaryLabel: String?,
    val secondary: (() -> Unit)?,
)

@Composable
internal fun ContinueWatchingScreen(
    entries: List<PlaybackEntry>,
    onSelect: (PlaybackRequest) -> Unit,
) {
    PosterGrid(
        items = entries,
        key = { it.url },
        title = { it.name },
        subtitle = {
            "${formatDuration(it.positionMs)} / ${formatDuration(it.durationMs)}"
        },
        image = { it.logo },
        onClick = {
            onSelect(PlaybackRequest(it.toChannel(), startPositionMs = it.positionMs))
        },
        emptyMessage = "No hay contenido en curso",
    )
}
