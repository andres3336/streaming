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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
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
import com.andres3336.streaming.data.search.SearchIndex
import com.andres3336.streaming.data.search.SearchItem
import com.andres3336.streaming.data.search.SearchKind
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.ui.QuickChip
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
) {
    val xtream = config.sources.xtream
    val client = remember(xtream, config.player.userAgent) {
        xtream?.let { XtreamClient(it, userAgent = config.player.userAgent) }
    }
    var index by remember { mutableStateOf(SearchIndex(emptyList())) }
    var loading by remember { mutableStateOf(xtream != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var debounced by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf<SearchKind?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var selectedMovie by remember { mutableStateOf<XtreamMovie?>(null) }
    var selectedSeries by remember { mutableStateOf<XtreamSeries?>(null) }
    val focusManager = LocalFocusManager.current
    val cacheKey = remember(channels, config.sources, refreshVersion) {
        CacheIdentity.sha256(
            "search|${config.sources.m3uUrl}|${config.sources.xtream?.baseUrl}|" +
                "${config.sources.xtream?.username}|$refreshVersion|${channels.size}",
        )
    }

    LaunchedEffect(cacheKey, retry) {
        val cached = SearchCatalogCache.get(cacheKey)
        if (cached != null) {
            index = withContext(Dispatchers.Default) { SearchIndex(cached) }
            loading = false
            if (SearchCatalogCache.isComplete(cacheKey)) return@LaunchedEffect
        }

        val items = mutableListOf<SearchItem>()
        channels.forEach { channel ->
            items += SearchItem(
                id = "live:${channel.liveKey()}",
                title = channel.name,
                subtitle = channel.group.orEmpty(),
                kind = SearchKind.LIVE,
                channel = channel,
            )
        }

        if (client != null) {
            loading = true
            error = null
            var complete = true
            index = withContext(Dispatchers.Default) { SearchIndex(items) }

            val categoryNames = when (val result = client.liveCategories()) {
                is XtreamResult.Success -> result.value.associate { it.id to it.name }
                is XtreamResult.Failure -> emptyMap()
            }

            when (val result = client.allLiveStreams()) {
                is XtreamResult.Success -> {
                    result.value
                        .filter(XtreamCategoryFilter::isUsable)
                        .forEach { stream ->
                            val channel = client.liveChannel(
                                stream = stream,
                                categoryName = categoryNames[stream.categoryId] ?: "Xtream",
                                providerLabel = "Xtream",
                            )
                            items += SearchItem(
                                id = "xtream:${stream.id}",
                                title = channel.name,
                                subtitle = channel.group.orEmpty(),
                                kind = SearchKind.CHANNELS,
                                channel = channel,
                            )
                        }
                    index = withContext(Dispatchers.Default) { SearchIndex(items) }
                }

                is XtreamResult.Failure -> {
                    complete = false
                    if (items.isEmpty()) error = result.message
                }
            }

            when (val result = client.allVodStreams()) {
                is XtreamResult.Success -> result.value.forEach { movie ->
                    items += SearchItem(
                        id = "movie:${movie.id}",
                        title = movie.name,
                        subtitle = listOfNotNull(movie.genre, movie.year).joinToString(" · "),
                        kind = SearchKind.MOVIE,
                        movie = movie,
                    )
                }

                is XtreamResult.Failure -> complete = false
            }

            when (val result = client.allSeries()) {
                is XtreamResult.Success -> result.value.forEach { series ->
                    items += SearchItem(
                        id = "series:${series.id}",
                        title = series.name,
                        subtitle = listOfNotNull(series.genre, series.year).joinToString(" · "),
                        kind = SearchKind.SERIES,
                        series = series,
                    )
                }

                is XtreamResult.Failure -> complete = false
            }

            index = withContext(Dispatchers.Default) { SearchIndex(items) }
            SearchCatalogCache.put(cacheKey, items, complete = complete)
            loading = false
        } else {
            index = withContext(Dispatchers.Default) { SearchIndex(items) }
            loading = false
        }
    }

    LaunchedEffect(query) {
        delay(250)
        debounced = query
    }

    val results = remember(index, debounced, kind) {
        index.search(debounced, kind)
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
        )
        return
    }
    if (series != null && client != null) {
        SeriesDetail(
            client = client,
            series = series,
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
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(StreamColors.Accent),
                modifier = Modifier
                    .weight(1f)
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
                                text = "Buscar en vivo, canales, películas y series…",
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
                Button(onClick = { query = "" }) {
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

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchFilterButton(
                label = "Todo",
                selected = kind == null,
                onClick = { kind = null },
            )
            SearchKind.entries.forEach { value ->
                SearchFilterButton(
                    label = value.label,
                    selected = kind == value,
                    onClick = { kind = value },
                )
            }
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
                "Escribe para buscar en vivo, canales, películas y series.",
            )

            results.isEmpty() -> CenteredMessage("Sin resultados para \"$query\"")

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(results, key = { it.item.id }) { result ->
                    Surface(
                        onClick = {
                            val item = result.item
                            when {
                                item.channel != null -> onPlay(PlaybackRequest(item.channel))
                                item.movie != null -> selectedMovie = item.movie
                                item.series != null -> selectedSeries = item.series
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ClickableSurfaceDefaults.shape(
                            shape = RoundedCornerShape(10.dp),
                        ),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = StreamColors.TextPrimary,
                            focusedContainerColor = StreamColors.CardHover,
                            focusedContentColor = StreamColors.TextPrimary,
                        ),
                        scale = ClickableSurfaceDefaults.scale(
                            focusedScale = 1.01f,
                            pressedScale = 0.99f,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.item.title,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                )
                                if (result.item.subtitle.isNotBlank()) {
                                    Text(
                                        text = result.item.subtitle,
                                        color = StreamColors.TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Text(
                                text = result.item.kind.label,
                                color = StreamColors.AccentBright,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    QuickChip(label = label, selected = selected, onClick = onClick)
}
