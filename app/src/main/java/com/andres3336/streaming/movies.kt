package com.andres3336.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.andres3336.streaming.data.media.TitleVariants
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.ui.CategoryEntry
import com.andres3336.streaming.ui.CategoryPanel
import com.andres3336.streaming.ui.CinematicDetails
import com.andres3336.streaming.ui.PosterGrid
import com.andres3336.streaming.ui.VideoPreview
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import kotlinx.coroutines.delay

@Composable
internal fun XtreamMoviesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    favorites: List<String>,
    onToggleFavoriteKey: (String) -> Unit,
    initialCategoryId: String? = null,
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
    var detail by remember { mutableStateOf<MovieGroup?>(null) }
    val categoryFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val gridState = rememberLazyGridState()
    val favoriteKeys = remember(favorites) { favorites.toSet() }
    val favoritesMode = selectedId == FAVORITES_CATEGORY

    LaunchedEffect(retry, refreshVersion, xtream) {
        loadingCategories = true
        error = null
        when (val result = client.vodCategories()) {
            is XtreamResult.Success -> {
                categories = result.value.filter {
                    XtreamCategoryFilter.isVisibleCategory(it, emptySet(), xtream.hideCategories)
                }
                if (selectedId.isNullOrBlank() ||
                    (selectedId != FAVORITES_CATEGORY &&
                        categories.none { it.id == selectedId })
                ) {
                    selectedId = categories.firstOrNull()?.id
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

    var focusedGroup by remember(selectedId) { mutableStateOf<MovieGroup?>(null) }

    val selectedGroup = detail
    if (selectedGroup != null) {
        MovieDetail(
            client = client,
            movie = selectedGroup.primary,
            variants = selectedGroup.variants,
            playerConfig = playerConfig,
            refreshVersion = refreshVersion,
            onBack = { detail = null },
            onPlay = onPlay,
            favorite = FavoriteKeys.movie(selectedGroup.primary.id) in favoriteKeys,
            onToggleFavorite = {
                onToggleFavoriteKey(FavoriteKeys.movie(selectedGroup.primary.id))
            },
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
    val groups = remember(visibleMovies) {
        visibleMovies
            .groupBy { TitleVariants.groupKey(it.name, it.year) }
            .map { (key, variants) ->
                MovieGroup(
                    key = key,
                    title = TitleVariants.parse(variants.first().name).baseTitle,
                    variants = variants,
                )
            }
    }
    var infoCache by remember(selectedId) {
        mutableStateOf<Map<String, XtreamMovie>>(emptyMap())
    }
    LaunchedEffect(focusedGroup?.primary?.id) {
        val item = focusedGroup?.primary ?: return@LaunchedEffect
        if (infoCache.containsKey(item.id)) return@LaunchedEffect
        delay(260)
        val result = client.vodInfo(item.id)
        if (result is XtreamResult.Success) {
            infoCache = infoCache + (item.id to result.value)
        }
    }
    val heroMovie = focusedGroup?.primary ?: groups.firstOrNull()?.primary

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
                    title = TitleVariants.parse(hero.name).baseTitle,
                    meta = listOfNotNull(
                        yearOnly(hero.year),
                        hero.genre,
                        hero.rating?.let { "\u2605 $it" },
                        if (FavoriteKeys.movie(hero.id) in favoriteKeys) "Favorito" else null,
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

                !favoritesMode && loadingMovies -> CenteredMessage("Cargando…")
                !favoritesMode && error != null -> SourceFailurePane(
                    message = error!!,
                    onRetry = { retry++ },
                    onUpdateConfig = onUpdateConfig,
                )

                else -> PosterGrid(
                    items = groups,
                    key = { it.key },
                    title = { it.title },
                    image = { it.primary.logo },
                    rating = { it.primary.rating },
                    favorite = { FavoriteKeys.movie(it.primary.id) in favoriteKeys },
                    onClick = { detail = it },
                    onToggleFavorite = { group ->
                        onToggleFavoriteKey(FavoriteKeys.movie(group.primary.id))
                    },
                    onFocusItem = { focusedGroup = it },
                    gridFocusRequester = gridFocus,
                    leftFocusRequester = categoryFocus,
                    gridState = gridState,
                    initialFocusKey = focusedGroup?.key,
                    modifier = Modifier.weight(1f),
                    emptyMessage = if (favoritesMode) {
                        "Aún no hay películas favoritas"
                    } else {
                        "No hay películas en esta categoría"
                    },
                )
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
    favorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    variants: List<XtreamMovie> = listOf(movie),
) {
    val orderedVariants = remember(variants) {
        variants.sortedBy { TitleVariants.priority(TitleVariants.parse(it.name).tag) }
    }
    var selected by remember(movie.id, orderedVariants) {
        mutableStateOf(orderedVariants.firstOrNull() ?: movie)
    }
    var info by remember(selected.id) { mutableStateOf<XtreamMovie?>(null) }
    var loading by remember(selected.id) { mutableStateOf(true) }
    var retry by remember(selected.id) { mutableIntStateOf(0) }
    val detailFocus = remember { FocusRequester() }

    BackHandler { onBack() }

    LaunchedEffect(selected.id, retry, refreshVersion) {
        loading = true
        info = null
        when (val result = client.vodInfo(selected.id)) {
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

    val current = info ?: selected
    val detailChannel = remember(current, selected.id) {
        Channel(
            name = TitleVariants.parse(current.name).baseTitle,
            url = client.movieUrl(current.copy(extension = selected.extension)),
            logo = current.logo ?: selected.logo,
            tvgId = current.id,
            language = LanguageTag.normalize(current.language ?: selected.language),
            isVod = true,
            year = current.year ?: selected.year,
            imdbId = current.imdbId ?: selected.imdbId,
            tmdbId = current.tmdbId ?: selected.tmdbId,
        )
    }
    val playMovie = { onPlay(detailChannel) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuickChip(label = "Volver", fontSize = 13.sp, onClick = onBack)
            Spacer(Modifier.weight(1f))
            QuickChip(
                label = if (favorite) "\u2605 Favorito" else "\u2606 Favorito",
                fontSize = 13.sp,
                onClick = onToggleFavorite,
            )
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
                    if (variants.size > 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            orderedVariants.forEach { variant ->
                                QuickChip(
                                    label = TitleVariants.parse(variant.name).tag,
                                    selected = variant.id == selected.id,
                                    onClick = { selected = variant },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = TitleVariants.parse(current.name).baseTitle,
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

/** One poster per title; the language variants are chosen in the detail. */
internal data class MovieGroup(
    val key: String,
    val title: String,
    val variants: List<XtreamMovie>,
) {
    val primary: XtreamMovie get() = variants.first()
}
