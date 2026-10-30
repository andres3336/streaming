package com.andres3336.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
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
import com.andres3336.streaming.data.media.TitleVariants
import com.andres3336.streaming.data.playback.PlaybackEntry
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamEpisode
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.data.xtream.XtreamSeriesInfo
import com.andres3336.streaming.ui.CategoryEntry
import com.andres3336.streaming.ui.CategoryPanel
import com.andres3336.streaming.ui.CinematicDetails
import com.andres3336.streaming.ui.PosterGrid
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.ui.VideoPreview
import kotlinx.coroutines.delay

@Composable
internal fun XtreamSeriesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    favorites: List<String>,
    onToggleFavoriteKey: (String) -> Unit,
    initialCategoryId: String? = null,
    onPlay: (PlaybackRequest) -> Unit,
    playbackEntries: List<PlaybackEntry> = emptyList(),
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
    var detail by remember { mutableStateOf<SeriesGroup?>(null) }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
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

    LaunchedEffect(initialCategoryId) {
        if (!initialCategoryId.isNullOrBlank()) selectedId = initialCategoryId
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

    var focusedGroup by remember(selectedId) { mutableStateOf<SeriesGroup?>(null) }

    val selectedGroup = detail
    if (selectedGroup != null) {
        SeriesDetail(
            client = client,
            series = selectedGroup.primary,
            variants = selectedGroup.variants,
            playerConfig = playerConfig,
            refreshVersion = refreshVersion,
            favorite = FavoriteKeys.series(selectedGroup.primary.id) in favoriteKeys,
            onToggleFavorite = {
                onToggleFavoriteKey(FavoriteKeys.series(selectedGroup.primary.id))
            },
            onUpdateConfig = onUpdateConfig,
            onBack = { detail = null },
            onPlay = onPlay,
            playbackEntries = playbackEntries,
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

    val visibleSeries = remember(favoritesMode, favoriteCatalog, favoriteKeys, series) {
        if (favoritesMode) {
            favoriteCatalog.orEmpty().filter { FavoriteKeys.series(it.id) in favoriteKeys }
        } else {
            series
        }
    }
    val groups = remember(visibleSeries) {
        visibleSeries
            .groupBy { TitleVariants.groupKey(it.name, it.year) }
            .map { (key, variants) ->
                SeriesGroup(
                    key = key,
                    title = TitleVariants.parse(variants.first().name).baseTitle,
                    variants = variants,
                )
            }
    }

    val hero = focusedGroup?.primary ?: groups.firstOrNull()?.primary

    CinematicBrowse(
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
            )
        },
        details = {
            if (hero != null) {
                CinematicDetails(
                    title = hero.name,
                    meta = listOfNotNull(
                        yearOnly(hero.year),
                        hero.genre,
                        hero.rating?.let { "\u2605 $it" },
                        if (FavoriteKeys.series(hero.id) in favoriteKeys) "Favorita" else null,
                    ),
                    synopsis = hero.plot,
                )
            }
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

                else -> PosterGrid(
                    items = groups,
                    key = { it.key },
                    title = { it.title },
                    image = { it.primary.cover },
                    rating = { it.primary.rating },
                    favorite = { FavoriteKeys.series(it.primary.id) in favoriteKeys },
                    onClick = { detail = it },
                    onToggleFavorite = { group ->
                        onToggleFavoriteKey(FavoriteKeys.series(group.primary.id))
                    },
                    onFocusItem = { focusedGroup = it },
                    gridFocusRequester = gridFocus,
                    leftFocusRequester = categoryFocus,
                    gridState = gridState,
                    initialFocusKey = focusedGroup?.key,
                    modifier = Modifier.weight(1f),
                    emptyMessage = if (favoritesMode) {
                        "Aún no hay series favoritas"
                    } else {
                        "No hay series en esta categoría"
                    },
                )
            }
        },
    )

}

@Composable
internal fun SeriesDetail(
    client: XtreamClient,
    series: XtreamSeries,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onUpdateConfig: () -> Unit,
    onBack: () -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
    variants: List<XtreamSeries> = listOf(series),
    playbackEntries: List<PlaybackEntry> = emptyList(),
) {
    var selected by remember(series.id, variants) {
        mutableStateOf(variants.firstOrNull { it.id == series.id } ?: variants.firstOrNull() ?: series)
    }
    var info by remember(selected.id) { mutableStateOf<XtreamSeriesInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var focusedEpisode by remember(selected.id) { mutableStateOf<XtreamEpisode?>(null) }
    val episodeFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler { onBack() }

    LaunchedEffect(selected.id, retry, refreshVersion) {
        loading = true
        error = null
        when (val result = client.seriesInfo(selected.id)) {
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

    val seriesInfo = info
    val episodes = remember(seriesInfo) {
        seriesInfo?.seasons.orEmpty()
            .flatMap { it.episodes }
            .sortedWith(compareBy({ it.season }, { it.episode }))
    }
    val toChannel: (XtreamEpisode) -> Channel = { episode ->
        Channel(
            name = "${TitleVariants.parse(selected.name).baseTitle} T${episode.season}E${episode.episode} · ${episode.title}",
            url = client.episodeUrl(episode),
            group = "Series",
            logo = selected.cover,
            tvgId = episode.id,
            isVod = true,
            year = selected.year,
            season = episode.season,
            episode = episode.episode,
            seriesTitle = selected.name,
            imdbId = selected.imdbId,
            tmdbId = selected.tmdbId,
        )
    }
    val queue = remember(episodes, selected.id) { episodes.map(toChannel) }
    val indexById = remember(episodes) {
        episodes.mapIndexed { index, episode -> episode.id to index }.toMap()
    }
    val resumeByUrl = remember(playbackEntries) { playbackEntries.associateBy { it.url } }
    val indexByUrl = remember(queue) {
        queue.mapIndexed { index, channel -> channel.url to index }.toMap()
    }
    val lastPlayedId = remember(playbackEntries, indexByUrl, episodes) {
        playbackEntries.firstOrNull { it.url in indexByUrl }?.url
            ?.let(indexByUrl::get)
            ?.let(episodes::getOrNull)
            ?.id
    }

    LaunchedEffect(episodes, lastPlayedId) {
        if (episodes.isEmpty()) return@LaunchedEffect
        if (focusedEpisode == null) {
            focusedEpisode = episodes.firstOrNull { it.id == lastPlayedId } ?: episodes.first()
        }
        delay(150)
        runCatching { episodeFocus.requestFocus() }
    }

    val selectedEpisode = focusedEpisode ?: episodes.firstOrNull()
    val playEpisode: (XtreamEpisode) -> Unit = { episode ->
        val index = indexById[episode.id] ?: 0
        val channel = queue.getOrNull(index) ?: toChannel(episode)
        onPlay(
            PlaybackRequest(
                channel = channel,
                queue = queue,
                index = index,
                startPositionMs = resumeByUrl[channel.url]?.positionMs ?: 0L,
            ),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuickChip(label = "Volver", fontSize = 13.sp, onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                text = TitleVariants.parse(selected.name).baseTitle,
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

        if (variants.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                variants.forEach { variant ->
                    QuickChip(
                        label = TitleVariants.parse(variant.name).tag,
                        selected = variant.id == selected.id,
                        onClick = { selected = variant },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                val metaSource = info
                val meta = listOfNotNull(
                    yearOnly(metaSource?.year ?: selected.year),
                    (metaSource?.rating ?: selected.rating)?.let { "\u2605 $it" },
                    metaSource?.genre ?: selected.genre,
                ).joinToString("  \u00b7  ")
                if (meta.isNotBlank()) {
                    Text(text = meta, color = StreamColors.AccentBright, fontSize = 13.sp)
                }
                val plot = metaSource?.plot ?: selected.plot
                if (!plot.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = plot,
                        color = StreamColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_CHANNEL_UP -> {
                                    focusManager.moveFocus(FocusDirection.Down)
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                                    focusManager.moveFocus(FocusDirection.Up)
                                    true
                                }

                                else -> false
                            }
                        },
                ) {
                    when {
                        loading -> CenteredMessage("Cargando episodios\u2026")
                        error != null -> SourceFailurePane(
                            message = error!!,
                            onRetry = { retry++ },
                            onUpdateConfig = onUpdateConfig,
                        )

                        seriesInfo == null || episodes.isEmpty() -> {
                            CenteredMessage("No hay episodios")
                        }

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            seriesInfo.seasons.forEach { season ->
                                item(key = "season-${season.number}") {
                                    Row(
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
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
                                items(season.episodes, key = { it.id }) { episode ->
                                    val channel = queue.getOrNull(indexById[episode.id] ?: 0)
                                    val resume = channel?.let { resumeByUrl[it.url] }
                                    EpisodeRow(
                                        label = "T${episode.season} E${episode.episode}",
                                        title = episode.title,
                                        progress = resume?.let { entry ->
                                            if (entry.durationMs > 0) {
                                                (entry.positionMs.toFloat() / entry.durationMs.toFloat())
                                                    .coerceIn(0f, 1f)
                                            } else {
                                                null
                                            }
                                        },
                                        focusModifier = if (episode.id == selectedEpisode?.id) {
                                            Modifier.focusRequester(episodeFocus)
                                        } else {
                                            Modifier
                                        },
                                        onFocus = { focusedEpisode = episode },
                                        onClick = { playEpisode(episode) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(0.4f)) {
                val episode = selectedEpisode
                val channel = episode?.let { queue.getOrNull(indexById[it.id] ?: 0) }
                if (episode != null && channel != null) {
                    VideoPreview(
                        channel = channel,
                        playerConfig = playerConfig,
                        onPlay = { playEpisode(episode) },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "T${episode.season} E${episode.episode} \u00b7 ${episode.title}",
                        color = StreamColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val resume = resumeByUrl[channel.url]
                    if (resume != null && resume.positionMs > 0) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Visto hasta ${formatDuration(resume.positionMs)}",
                            color = StreamColors.AccentBright,
                            fontSize = 11.sp,
                        )
                    }
                } else if (!loading) {
                    Text(
                        text = "Selecciona un episodio",
                        color = StreamColors.TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun EpisodeRow(
    label: String,
    title: String,
    progress: Float? = null,
    focusModifier: Modifier = Modifier,
    onFocus: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(focusModifier)
            .border(1.dp, if (focused) StreamColors.AccentBright else Color.Transparent, shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus?.invoke()
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = StreamColors.Surface,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.AccentDeep,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
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
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(StreamColors.Border),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(StreamColors.Accent),
                    )
                }
            }
        }
    }
}

/** One poster per title; the language variants are chosen in the detail. */
internal data class SeriesGroup(
    val key: String,
    val title: String,
    val variants: List<XtreamSeries>,
) {
    val primary: XtreamSeries get() = variants.first()
}
