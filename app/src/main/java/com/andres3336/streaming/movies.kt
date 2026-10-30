package com.andres3336.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.XtreamConfig
import com.andres3336.streaming.data.media.FavoriteKeys
import com.andres3336.streaming.data.media.LanguageTag
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.ui.CategoryEntry
import com.andres3336.streaming.ui.CategoryPanel
import com.andres3336.streaming.ui.HeroBlock
import com.andres3336.streaming.ui.PosterGrid
import com.andres3336.streaming.ui.VideoPreview
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.ui.TextSizes
import kotlinx.coroutines.delay

@Composable
internal fun XtreamMoviesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    favorites: List<String>,
    onToggleFavoriteKey: (String) -> Unit,
    onPlay: (Channel) -> Unit,
) {
    val client = remember(xtream, playerConfig.userAgent) {
        XtreamClient(xtream, userAgent = playerConfig.userAgent)
    }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var movies by remember { mutableStateOf<List<XtreamMovie>>(emptyList()) }
    var favoriteCatalog by remember(xtream, refreshVersion) {
        mutableStateOf<List<XtreamMovie>?>(null)
    }
    var loadingCategories by remember { mutableStateOf(true) }
    var loadingMovies by remember { mutableStateOf(false) }
    var loadingFavorites by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var favoritesError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<XtreamMovie?>(null) }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val favoriteKeys = remember(favorites) { favorites.toSet() }
    val favoritesMode = selectedId == FAVORITES_CATEGORY

    LaunchedEffect(retry, refreshVersion, xtream) {
        loadingCategories = true
        error = null
        when (val result = client.vodCategories()) {
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
            when (val result = client.allVodStreams()) {
                is XtreamResult.Success -> favoriteCatalog = result.value
                is XtreamResult.Failure -> favoritesError = result.message
            }
            loadingFavorites = false
            return@LaunchedEffect
        }

        loadingMovies = true
        error = null
        when (val result = client.vodStreams(id)) {
            is XtreamResult.Success -> {
                movies = result.value
                loadingMovies = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loadingMovies = false
            }
        }
    }

    val selectedMovie = detail
    if (selectedMovie != null) {
        MovieDetail(
            client = client,
            movie = selectedMovie,
            playerConfig = playerConfig,
            refreshVersion = refreshVersion,
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
        CenteredMessage("Cargando películas…")
        return
    }

    val visibleMovies = remember(favoritesMode, favoriteCatalog, favoriteKeys, movies) {
        if (favoritesMode) {
            favoriteCatalog.orEmpty().filter { FavoriteKeys.movie(it.id) in favoriteKeys }
        } else {
            movies
        }
    }
    var focusedMovie by remember(selectedId) { mutableStateOf<XtreamMovie?>(null) }
    var infoCache by remember(selectedId) {
        mutableStateOf<Map<String, XtreamMovie>>(emptyMap())
    }
    LaunchedEffect(focusedMovie?.id) {
        val item = focusedMovie ?: return@LaunchedEffect
        if (infoCache.containsKey(item.id)) return@LaunchedEffect
        delay(260)
        val result = client.vodInfo(item.id)
        if (result is XtreamResult.Success) {
            infoCache = infoCache + (item.id to result.value)
        }
    }
    val heroMovie = focusedMovie ?: visibleMovies.firstOrNull()

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

                !favoritesMode && loadingMovies -> CenteredMessage("Cargando…")
                !favoritesMode && error != null -> SourceFailurePane(
                    message = error!!,
                    onRetry = { retry++ },
                    onUpdateConfig = onUpdateConfig,
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    val heroBase = heroMovie
                    val heroInfo = heroBase?.let { infoCache[it.id] }
                    val hero = heroBase?.let { item ->
                        if (heroInfo == null) {
                            item
                        } else {
                            item.copy(
                                plot = heroInfo.plot ?: item.plot,
                                backdrop = heroInfo.backdrop ?: item.backdrop,
                                genre = heroInfo.genre ?: item.genre,
                                year = heroInfo.year ?: item.year,
                                rating = heroInfo.rating ?: item.rating,
                                duration = heroInfo.duration ?: item.duration,
                            )
                        }
                    }
                    if (hero != null) {
                        HeroBlock(
                            title = hero.name,
                            badge = "Película",
                            meta = listOfNotNull(
                                yearOnly(hero.year),
                                hero.genre,
                                hero.rating?.let { "\u2605 $it" },
                                if (FavoriteKeys.movie(hero.id) in favoriteKeys) "Favorito" else null,
                            ),
                            synopsis = hero.plot,
                            image = hero.logo,
                            primaryLabel = "Reproducir",
                            onPrimary = { onPlay(hero.toChannel(client)) },
                            secondaryLabel = "Ficha",
                            onSecondary = { detail = hero },
                            height = 206,
                            titleSize = TextSizes.ScreenTitle,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    PosterGrid(
                        items = visibleMovies,
                        key = { it.id },
                        title = { it.name },
                        subtitle = {
                            listOfNotNull(yearOnly(it.year), it.genre)
                                .joinToString(" · ")
                                .ifBlank { null }
                        },
                        image = { it.logo },
                        rating = { it.rating },
                        language = { LanguageTag.normalize(it.language) },
                        favorite = { FavoriteKeys.movie(it.id) in favoriteKeys },
                        onClick = { detail = it },
                        onToggleFavorite = {
                            onToggleFavoriteKey(FavoriteKeys.movie(it.id))
                        },
                        onFocusItem = { focusedMovie = it },
                        gridFocusRequester = gridFocus,
                        leftFocusRequester = categoryFocus,
                        modifier = Modifier.weight(1f),
                        emptyMessage = if (favoritesMode) {
                            "Aún no hay películas favoritas"
                        } else {
                            "No hay películas en esta categoría"
                        },
                    )
                }
            }
        },
    )
}

@Composable
internal fun MovieDetail(
    client: XtreamClient,
    movie: XtreamMovie,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onBack: () -> Unit,
    onPlay: (Channel) -> Unit,
) {
    var info by remember(movie.id) { mutableStateOf<XtreamMovie?>(null) }
    var loading by remember(movie.id) { mutableStateOf(true) }
    var retry by remember(movie.id) { mutableIntStateOf(0) }
    val detailFocus = remember { FocusRequester() }

    BackHandler { onBack() }

    LaunchedEffect(movie.id, retry, refreshVersion) {
        loading = true
        when (val result = client.vodInfo(movie.id)) {
            is XtreamResult.Success -> info = result.value
            is XtreamResult.Failure -> Unit
        }
        loading = false
    }

    LaunchedEffect(loading) {
        if (!loading) {
            delay(120)
            runCatching { detailFocus.requestFocus() }
        }
    }

    val current = info ?: movie
    val detailChannel = remember(current, movie.id) {
        Channel(
            name = current.name,
            url = client.movieUrl(current.copy(extension = movie.extension)),
            group = "Películas",
            logo = current.logo ?: movie.logo,
            tvgId = current.id,
            language = LanguageTag.normalize(current.language ?: movie.language),
            isVod = true,
            year = current.year ?: movie.year,
            imdbId = current.imdbId ?: movie.imdbId,
            tmdbId = current.tmdbId ?: movie.tmdbId,
        )
    }
    val playMovie = { onPlay(detailChannel) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuickChip(label = "Volver", fontSize = 13.sp, onClick = onBack)
        }
        Spacer(Modifier.height(10.dp))

        if (loading && info == null) {
            CenteredMessage("Cargando ficha\u2026")
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(0.55f)) {
                    VideoPreview(
                        channel = detailChannel,
                        playerConfig = playerConfig,
                        onPlay = playMovie,
                        modifier = Modifier.focusRequester(detailFocus),
                    )
                }
                Spacer(Modifier.width(18.dp))
                Column(modifier = Modifier.weight(0.45f)) {
                    Text(
                        text = current.name,
                        color = StreamColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = listOfNotNull(
                        yearOnly(current.year),
                        current.rating?.let { "\u2605 $it" },
                        current.duration,
                    ).joinToString("  \u00b7  ")
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(text = meta, color = StreamColors.AccentBright, fontSize = 13.sp)
                    }
                    current.genre?.let {
                        Spacer(Modifier.height(3.dp))
                        Text(text = it, color = StreamColors.TextSecondary, fontSize = 12.sp)
                    }
                    current.plot?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = it,
                            color = StreamColors.TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            maxLines = 9,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    current.cast?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Reparto: $it",
                            color = StreamColors.TextMuted,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
