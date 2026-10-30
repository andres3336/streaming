package com.andres3336.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.XtreamConfig
import com.andres3336.streaming.data.media.FavoriteKeys
import com.andres3336.streaming.data.media.LanguageTag
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.data.xtream.XtreamSeriesInfo
import com.andres3336.streaming.ui.CategoryEntry
import com.andres3336.streaming.ui.CategoryPanel
import com.andres3336.streaming.ui.HeroBlock
import com.andres3336.streaming.ui.PosterGrid
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.ui.TextSizes

@Composable
internal fun XtreamSeriesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    favorites: List<String>,
    onToggleFavoriteKey: (String) -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
) {
    val client = remember(xtream, playerConfig.userAgent) {
        XtreamClient(xtream, userAgent = playerConfig.userAgent)
    }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var series by remember { mutableStateOf<List<XtreamSeries>>(emptyList()) }
    var favoriteCatalog by remember(xtream, refreshVersion) {
        mutableStateOf<List<XtreamSeries>?>(null)
    }
    var loadingCategories by remember { mutableStateOf(true) }
    var loadingSeries by remember { mutableStateOf(false) }
    var loadingFavorites by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var favoritesError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<XtreamSeries?>(null) }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val favoriteKeys = remember(favorites) { favorites.toSet() }
    val favoritesMode = selectedId == FAVORITES_CATEGORY

    LaunchedEffect(retry, refreshVersion, xtream) {
        loadingCategories = true
        error = null
        when (val result = client.seriesCategories()) {
            is XtreamResult.Success -> {
                categories = result.value
                if (selectedId.isNullOrBlank() ||
                    (selectedId != FAVORITES_CATEGORY &&
                        result.value.none { it.id == selectedId })
                ) {
                    selectedId = result.value.firstOrNull()?.id
                }
                loadingCategories = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loadingCategories = false
            }
        }
    }

    LaunchedEffect(selectedId, retry, refreshVersion, xtream) {
        val id = selectedId ?: return@LaunchedEffect
        if (id == FAVORITES_CATEGORY) {
            if (favoriteCatalog != null) return@LaunchedEffect
            loadingFavorites = true
            favoritesError = null
            when (val result = client.allSeries()) {
                is XtreamResult.Success -> favoriteCatalog = result.value
                is XtreamResult.Failure -> favoritesError = result.message
            }
            loadingFavorites = false
            return@LaunchedEffect
        }

        loadingSeries = true
        error = null
        when (val result = client.series(id)) {
            is XtreamResult.Success -> {
                series = result.value
                loadingSeries = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loadingSeries = false
            }
        }
    }

    val selectedSeries = detail
    if (selectedSeries != null) {
        SeriesDetail(
            client = client,
            series = selectedSeries,
            refreshVersion = refreshVersion,
            favorite = FavoriteKeys.series(selectedSeries.id) in favoriteKeys,
            onToggleFavorite = {
                onToggleFavoriteKey(FavoriteKeys.series(selectedSeries.id))
            },
            onUpdateConfig = onUpdateConfig,
            onBack = { detail = null },
            onPlay = onPlay,
        )
        return
    }

    if (error != null && categories.isEmpty()) {
        SourceFailurePane(
            message = error!!,
            onRetry = { retry++ },
            onUpdateConfig = onUpdateConfig,
        )
        return
    }

    if (loadingCategories && categories.isEmpty()) {
        CenteredMessage("Cargando series…")
        return
    }

    var focusedSeries by remember(selectedId) { mutableStateOf<XtreamSeries?>(null) }
    val visibleSeries = remember(favoritesMode, favoriteCatalog, favoriteKeys, series) {
        if (favoritesMode) {
            favoriteCatalog.orEmpty().filter { FavoriteKeys.series(it.id) in favoriteKeys }
        } else {
            series
        }
    }

    BrowseShell(
        rail = {
            CategoryPanel(
                entries = buildList {
                    add(CategoryEntry(FAVORITES_CATEGORY, "Favoritos", favorite = true))
                    categories.forEach { add(CategoryEntry(it.id, it.name)) }
                },
                selectedId = selectedId,
                onSelect = { selectedId = it },
                modifier = Modifier.fillMaxSize(),
                selectedFocusRequester = categoryFocus,
                rightFocusRequester = gridFocus,
                footer = {
                    Text(
                        text = "Mantén OK: ★ favorito",
                        color = StreamColors.TextMuted,
                        fontSize = 10.sp,
                        maxLines = 2,
                    )
                },
            )
        },
        content = {
            when {
                favoritesMode && loadingFavorites -> CenteredMessage("Cargando favoritos…")
                favoritesMode && favoritesError != null -> SourceFailurePane(
                    message = favoritesError!!,
                    onRetry = { retry++ },
                    onUpdateConfig = onUpdateConfig,
                )

                !favoritesMode && loadingSeries -> CenteredMessage("Cargando…")
                !favoritesMode && error != null -> SourceFailurePane(
                    message = error!!,
                    onRetry = { retry++ },
                    onUpdateConfig = onUpdateConfig,
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    val hero = focusedSeries ?: visibleSeries.firstOrNull()
                    if (hero != null) {
                        HeroBlock(
                            title = hero.name,
                            badge = "Serie",
                            meta = listOfNotNull(
                                yearOnly(hero.year),
                                hero.genre,
                                hero.rating?.let { "\u2605 $it" },
                                if (FavoriteKeys.series(hero.id) in favoriteKeys) "Favorita" else null,
                            ),
                            synopsis = hero.plot,
                            image = hero.cover,
                            primaryLabel = "Ver serie",
                            onPrimary = { detail = hero },
                            secondaryLabel = "Favorito",
                            onSecondary = { onToggleFavoriteKey(FavoriteKeys.series(hero.id)) },
                            height = 206,
                            titleSize = TextSizes.ScreenTitle,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    PosterGrid(
                        items = visibleSeries,
                        key = { it.id },
                        title = { it.name },
                        subtitle = {
                            listOfNotNull(yearOnly(it.year), it.genre)
                                .joinToString(" · ")
                                .ifBlank { null }
                        },
                        image = { it.cover },
                        rating = { it.rating },
                        language = { LanguageTag.normalize(it.language) },
                        favorite = { FavoriteKeys.series(it.id) in favoriteKeys },
                        onClick = { detail = it },
                        onToggleFavorite = {
                            onToggleFavoriteKey(FavoriteKeys.series(it.id))
                        },
                        onFocusItem = { focusedSeries = it },
                        gridFocusRequester = gridFocus,
                        leftFocusRequester = categoryFocus,
                        modifier = Modifier.weight(1f),
                        emptyMessage = if (favoritesMode) {
                            "Aún no hay series favoritas"
                        } else {
                            "No hay series en esta categoría"
                        },
                    )
                }
            }
        },
    )
}

@Composable
internal fun SeriesDetail(
    client: XtreamClient,
    series: XtreamSeries,
    refreshVersion: Int,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onUpdateConfig: () -> Unit,
    onBack: () -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
) {
    var info by remember(series.id) { mutableStateOf<XtreamSeriesInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    BackHandler { onBack() }

    LaunchedEffect(series.id, retry, refreshVersion) {
        loading = true
        error = null
        when (val result = client.seriesInfo(series.id)) {
            is XtreamResult.Success -> {
                info = result.value
                loading = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuickChip(label = "Volver", fontSize = 13.sp, onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                text = series.name,
                color = StreamColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            QuickChip(
                label = if (favorite) "\u2605 Favorito" else "\u2606 Favorito",
                fontSize = 13.sp,
                onClick = onToggleFavorite,
            )
        }

        if (!series.plot.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = series.plot,
                color = StreamColors.TextSecondary,
                fontSize = 15.sp,
                maxLines = 3,
            )
        }

        val metaSource = info
        val meta = listOfNotNull(
            yearOnly(metaSource?.year ?: series.year),
            (metaSource?.rating ?: series.rating)?.let { "★ $it" },
            metaSource?.genre ?: series.genre,
        ).joinToString(" · ")
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(text = meta, color = StreamColors.AccentBright, fontSize = 16.sp)
        }
        val plot = metaSource?.plot ?: series.plot
        if (!plot.isNullOrBlank() && plot != series.plot) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = plot,
                color = StreamColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 3,
            )
        }

        Spacer(Modifier.height(16.dp))

        val seriesInfo = info
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                loading -> CenteredMessage("Cargando episodios…")
                error != null -> SourceFailurePane(
                    message = error!!,
                    onRetry = { retry++ },
                    onUpdateConfig = onUpdateConfig,
                )
                seriesInfo == null || seriesInfo.seasons.isEmpty() -> {
                    CenteredMessage("No hay episodios")
                }

                else -> {
                    val allEpisodes = seriesInfo.seasons
                        .flatMap { it.episodes }
                        .sortedWith(compareBy({ it.season }, { it.episode }))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        seriesInfo.seasons.forEach { season ->
                            item(key = "season-${season.number}") {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(14.dp)
                                            .background(
                                                StreamColors.Accent,
                                                RoundedCornerShape(2.dp),
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Temporada ${season.number}",
                                        color = StreamColors.TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${season.episodes.size} episodios",
                                        color = StreamColors.TextMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            items(season.episodes) { episode ->
                                EpisodeRow(
                                    label = "T${episode.season} E${episode.episode}",
                                    title = episode.title,
                                    onClick = {
                                        val queue = allEpisodes.map { item ->
                                            Channel(
                                                name = "${series.name} T${item.season}E${item.episode} · ${item.title}",
                                                url = client.episodeUrl(item),
                                                group = "Series",
                                                logo = series.cover,
                                                tvgId = item.id,
                                                isVod = true,
                                                year = series.year,
                                                season = item.season,
                                                episode = item.episode,
                                                seriesTitle = series.name,
                                                imdbId = series.imdbId,
                                                tmdbId = series.tmdbId,
                                            )
                                        }
                                        val index = allEpisodes.indexOf(episode).coerceAtLeast(0)
                                        onPlay(
                                            PlaybackRequest(
                                                channel = queue[index],
                                                queue = queue,
                                                index = index,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun EpisodeRow(
    label: String,
    title: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (focused) StreamColors.AccentBright else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = StreamColors.Surface,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.AccentDeep,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = StreamColors.AccentBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp),
            )
            Text(
                text = title,
                color = StreamColors.TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
