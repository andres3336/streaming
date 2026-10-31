package com.andres3336.streaming

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.cache.CacheIdentity
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.live.liveKey
import com.andres3336.streaming.data.media.FavoriteKeys
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.search.SearchCatalogCache
import com.andres3336.streaming.data.search.SearchCatalogStore
import com.andres3336.streaming.data.search.SearchIndex
import com.andres3336.streaming.data.search.SearchItem
import com.andres3336.streaming.data.search.SearchKind
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.ui.StreamColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun UniversalSearchScreen(
    channels: List<Channel>,
    config: AppConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    favorites: List<String>,
    onToggleFavoriteKey: (String) -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
    onOpenCategory: (String, String) -> Unit,
) {
    val xtream = config.sources.xtream
    val client = remember(xtream, config.player.userAgent) {
        xtream?.let { XtreamClient(it, userAgent = config.player.userAgent) }
    }
    var index by remember { mutableStateOf(SearchIndex(emptyList())) }
    var loading by remember { mutableStateOf(xtream != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    val query = field.text
    val searchFocus = remember { FocusRequester() }
    var debounced by remember { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    var selectedMovie by remember { mutableStateOf<XtreamMovie?>(null) }
    var selectedSeries by remember { mutableStateOf<XtreamSeries?>(null) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val catalogStore = remember { SearchCatalogStore(context) }
    val cacheKey = remember(channels, config.sources, refreshVersion) {
        CacheIdentity.sha256(
            "search-v4|${config.sources.m3uUrl}|${config.sources.xtream?.baseUrl}|" +
                "${config.sources.xtream?.username}|$refreshVersion|${channels.size}|" +
                config.sources.xtream?.hideCategories.orEmpty().joinToString(","),
        )
    }

    LaunchedEffect(cacheKey, retry) {
        val cached = SearchCatalogCache.get(cacheKey)
        if (cached != null) {
            index = withContext(Dispatchers.Default) { SearchIndex(cached) }
            loading = false
            if (SearchCatalogCache.isComplete(cacheKey)) return@LaunchedEffect
        }

        // Encrypted, gzipped catalogue from a previous run: no network on a cold start.
        val persisted = withContext(Dispatchers.IO) { catalogStore.load(cacheKey) }
        if (persisted != null) {
            index = withContext(Dispatchers.Default) { SearchIndex(persisted) }
            SearchCatalogCache.put(cacheKey, persisted, complete = true)
            loading = false
            return@LaunchedEffect
        }

        val items = mutableListOf<SearchItem>()
        channels.forEach { channel ->
            items += SearchItem(
                id = "live:${channel.liveKey()}",
                title = channel.name,
                subtitle = "",
                kind = SearchKind.TV,
                image = channel.logo,
                streamUrl = channel.url,
                streamTvgId = channel.tvgId,
                streamLanguage = channel.language,
            )
        }

        if (client != null) {
            loading = true
            error = null
            var complete = true
            index = withContext(Dispatchers.Default) { SearchIndex(items) }

            val liveCategories = when (val result = client.liveCategories()) {
                is XtreamResult.Success -> result.value
                is XtreamResult.Failure -> {
                    complete = false
                    emptyList()
                }
            }
            val hideCategories = config.sources.xtream?.hideCategories.orEmpty()
            val hiddenCategoryIds = liveCategories
                .filterNot {
                    XtreamCategoryFilter.isVisibleCategory(it, emptySet(), hideCategories)
                }
                .map { it.id }
                .toSet()
            liveCategories
                .filterNot { it.id in hiddenCategoryIds }
                .forEach { category ->
                    items += categoryItem(category.id, category.name, "tv")
                }

            when (val result = client.allLiveStreams()) {
                is XtreamResult.Success -> {
                    result.value
                        .filter(XtreamCategoryFilter::isUsable)
                        .filterNot { it.categoryId in hiddenCategoryIds }
                        .forEach { stream ->
                            items += SearchItem(
                                id = "xtream:${stream.id}",
                                title = stream.name,
                                subtitle = "",
                                kind = SearchKind.TV,
                                image = stream.logo,
                                streamUrl = client.liveUrl(stream),
                                streamTvgId = "xtream:${stream.id}",
                                streamLanguage = stream.language,
                            )
                        }
                    index = withContext(Dispatchers.Default) { SearchIndex(items) }
                }

                is XtreamResult.Failure -> {
                    complete = false
                    if (items.isEmpty()) error = result.message
                }
            }

            val movieCategories = when (val result = client.vodCategories()) {
                is XtreamResult.Success -> result.value
                is XtreamResult.Failure -> {
                    complete = false
                    emptyList()
                }
            }
            val hiddenMovieCategoryIds = movieCategories
                .filterNot {
                    XtreamCategoryFilter.isVisibleCategory(it, emptySet(), hideCategories)
                }
                .map { it.id }
                .toSet()

            when (val result = client.allVodStreams()) {
                is XtreamResult.Success -> result.value
                    .filterNot { it.categoryId in hiddenMovieCategoryIds }
                    .forEach { movie ->
                        items += SearchItem(
                            id = "movie:${movie.id}",
                            title = movie.name,
                            subtitle = listOfNotNull(movie.genre, movie.year).joinToString(" · "),
                            kind = SearchKind.MOVIE,
                            image = movie.logo,
                            movieId = movie.id,
                            movieExtension = movie.extension,
                            movieCategoryId = movie.categoryId,
                        )
                    }

                is XtreamResult.Failure -> complete = false
            }
            movieCategories
                .filterNot { it.id in hiddenMovieCategoryIds }
                .forEach { category -> items += categoryItem(category.id, category.name, "movies") }

            val seriesCategories = when (val result = client.seriesCategories()) {
                is XtreamResult.Success -> result.value
                is XtreamResult.Failure -> {
                    complete = false
                    emptyList()
                }
            }
            val hiddenSeriesCategoryIds = seriesCategories
                .filterNot {
                    XtreamCategoryFilter.isVisibleCategory(it, emptySet(), hideCategories)
                }
                .map { it.id }
                .toSet()

            when (val result = client.allSeries()) {
                is XtreamResult.Success -> result.value
                    .filterNot { it.categoryId in hiddenSeriesCategoryIds }
                    .forEach { series ->
                        items += SearchItem(
                            id = "series:${series.id}",
                            title = series.name,
                            subtitle = listOfNotNull(series.genre, series.year).joinToString(" · "),
                            kind = SearchKind.SERIES,
                            image = series.cover,
                            seriesId = series.id,
                            seriesCategoryId = series.categoryId,
                        )
                    }

                is XtreamResult.Failure -> complete = false
            }
            seriesCategories
                .filterNot { it.id in hiddenSeriesCategoryIds }
                .forEach { category -> items += categoryItem(category.id, category.name, "series") }

            index = withContext(Dispatchers.Default) { SearchIndex(items) }
            SearchCatalogCache.put(cacheKey, items, complete = complete)
            if (complete) {
                withContext(Dispatchers.IO) {
                    catalogStore.save(cacheKey, items.distinctBy { it.id })
                }
            }
            loading = false
        } else {
            index = withContext(Dispatchers.Default) { SearchIndex(items) }
            loading = false
        }
    }

    // The field takes focus when Buscar opens so typing and the TV microphone target
    // it directly instead of whatever was focused before.
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { searchFocus.requestFocus() }
    }

    LaunchedEffect(query) {
        delay(250)
        debounced = query
    }

    val results = remember(index, debounced) {
        index.search(debounced, limit = 150)
    }
    val groups = remember(results) {
        SearchKind.entries
            .mapNotNull { kind ->
                val groupItems = results.filter { it.item.kind == kind }
                if (groupItems.isEmpty()) null else kind to groupItems
            }
            // Best match first. Ties keep the enum order (En vivo, Canales,
            // Películas, Series, Categorías), which keeps the familiar layout.
            .sortedByDescending { (_, groupItems) -> groupItems.first().score }
    }
    val favoriteKeys = remember(favorites) { favorites.toSet() }

    val movie = selectedMovie
    val series = selectedSeries
    if (movie != null && client != null) {
        MovieDetail(
            client = client,
            movie = movie,
            playerConfig = config.player,
            refreshVersion = refreshVersion,
            onBack = { selectedMovie = null },
            onPlay = { channel -> onPlay(PlaybackRequest(channel)) },
            favorite = FavoriteKeys.movie(movie.id) in favorites,
            onToggleFavorite = { onToggleFavoriteKey(FavoriteKeys.movie(movie.id)) },
        )
        return
    }
    if (series != null && client != null) {
        SeriesDetail(
            client = client,
            series = series,
            playerConfig = config.player,
            refreshVersion = refreshVersion,
            favorite = FavoriteKeys.series(series.id) in favoriteKeys,
            onToggleFavorite = {
                onToggleFavoriteKey(FavoriteKeys.series(series.id))
            },
            onUpdateConfig = onUpdateConfig,
            onBack = { selectedSeries = null },
            onPlay = onPlay,
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = field,
                onValueChange = { field = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(StreamColors.Accent),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocus)
                    .background(StreamColors.SurfaceRaised, RoundedCornerShape(8.dp))
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }
                        if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN) {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar en vivo, canales, películas, series y categorías…",
                                color = StreamColors.TextMuted,
                                fontSize = 18.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = { field = TextFieldValue("") }) {
                    Text("Limpiar", fontSize = 16.sp)
                }
            }
            Text(
                text = when {
                    loading && index.size() == 0 -> "Cargando catálogo…"
                    loading -> "${index.size()} · cargando más…"
                    query.isBlank() -> "${index.size()} elementos"
                    else -> "${results.size} resultados"
                },
                color = StreamColors.TextMuted,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading && index.size() == 0 -> CenteredMessage("Cargando catálogo de Xtream…")
            error != null && index.size() == 0 -> SourceFailurePane(
                message = error!!,
                onRetry = { retry++ },
                onUpdateConfig = onUpdateConfig,
            )

            query.isBlank() -> CenteredMessage(
                "Escribe para buscar en vivo, canales, películas, series y categorías.",
            )

            results.isEmpty() -> CenteredMessage("Sin resultados para \"$query\"")

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                groups.forEach { (kind, groupItems) ->
                    item(key = "group:${kind.name}") {
                        SearchGroupHeader(label = kind.label, count = groupItems.size)
                    }
                    items(groupItems.take(GROUP_LIMIT), key = { it.item.id }) { result ->
                        val item = result.item
                        SearchResultRow(
                            title = item.title,
                            subtitle = item.subtitle,
                            trailing = item.categorySection?.let(::sectionLabel),
                            onClick = {
                                val streamUrl = item.streamUrl
                                val movieId = item.movieId
                                val seriesId = item.seriesId
                                when {
                                    streamUrl != null -> onPlay(
                                        PlaybackRequest(
                                            Channel(
                                                name = item.title,
                                                url = streamUrl,
                                                logo = item.image,
                                                tvgId = item.streamTvgId,
                                                language = item.streamLanguage,
                                                isVod = false,
                                            ),
                                        ),
                                    )

                                    movieId != null -> selectedMovie = XtreamMovie(
                                        id = movieId,
                                        name = item.title,
                                        logo = item.image,
                                        categoryId = item.movieCategoryId.orEmpty(),
                                        extension = item.movieExtension ?: "mp4",
                                    )

                                    seriesId != null -> selectedSeries = XtreamSeries(
                                        id = seriesId,
                                        name = item.title,
                                        cover = item.image,
                                        categoryId = item.seriesCategoryId.orEmpty(),
                                        plot = null,
                                    )

                                    item.categoryId != null ->
                                        onOpenCategory(item.categorySection ?: "tv", item.categoryId)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchGroupHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(StreamColors.Accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            color = StreamColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$count",
            color = StreamColors.TextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    trailing: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.CardHover,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.01f, pressedScale = 0.99f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, maxLines = 1)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = StreamColors.TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                }
            }
            if (trailing != null) {
                Text(
                    text = trailing,
                    color = StreamColors.AccentBright,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** Rows shown per group; the header still reports the full count. */
private const val GROUP_LIMIT = 8

private fun sectionLabel(section: String): String = when (section) {
    "tv" -> "TV"
    "movies" -> "Películas"
    "series" -> "Series"
    else -> section
}
private fun categoryItem(id: String, name: String, section: String): SearchItem = SearchItem(
    id = "category:$section:$id",
    title = name,
    subtitle = "",
    kind = SearchKind.CATEGORY,
    categoryId = id,
    categorySection = section,
)
