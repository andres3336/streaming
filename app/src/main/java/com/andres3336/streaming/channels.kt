package com.andres3336.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.cache.CacheIdentity
import com.andres3336.streaming.data.cache.XtreamCatalog
import com.andres3336.streaming.data.cache.XtreamCatalogCache
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.NetworkConfig
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.epg.EpgGuide
import com.andres3336.streaming.data.epg.EpgNowNext
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.live.liveKey
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.ui.LivePreviewPane
import com.andres3336.streaming.ui.LocalLiveFocus
import com.andres3336.streaming.ui.ProgressLine
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class LiveRailEntry(
    val id: String,
    val label: String,
)

@Composable
internal fun GistLiveBrowse(
    channels: List<Channel>,
    liveState: LiveState,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
) {
    var selected by remember { mutableStateOf("all") }
    var focused by remember { mutableStateOf<Channel?>(null) }
    val epgGuide = LocalEpgGuide.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            nowMs = System.currentTimeMillis()
        }
    }
    LaunchedEffect(channels) {
        if (focused == null) {
            focused = channels.firstOrNull { it.liveKey() == liveState.lastChannel }
                ?: channels.firstOrNull()
        }
    }
    val groups = remember(channels) {
        channels.mapNotNull { it.group?.takeIf(String::isNotBlank) }.distinct()
    }
    val entries = remember(groups) {
        listOf(
            LiveRailEntry("all", "Todos"),
            LiveRailEntry("favorites", "Favoritos"),
            LiveRailEntry("recents", "Recientes"),
        ) + groups.map { LiveRailEntry("group:$it", it) }
    }

    CompositionLocalProvider(LocalLiveFocus provides { focused = it }) {
        BrowseShell(
            rail = {
                BrowseRail(
                    items = entries,
                    selected = entries.firstOrNull { it.id == selected },
                    label = LiveRailEntry::label,
                    onSelect = { selected = it.id },
                )
            },
            preview = {
                LivePreviewPane(
                    channel = focused,
                    nowNext = focused?.let { epgGuide?.nowNext(it, nowMs) },
                    nowMs = nowMs,
                    playerConfig = playerConfig,
                    networkConfig = networkConfig,
                )
            },
            content = {
            when {
                selected == "favorites" -> ChannelList(
                    channels = channels,
                    favorites = liveState.favorites,
                    recents = liveState.recents,
                    lastChannel = liveState.lastChannel,
                    startLastChannel = true,
                    onToggleFavorite = onToggleFavorite,
                    onSelect = onSelect,
                    section = LiveListSection.Favorites,
                    title = "Favoritos",
                )

                selected == "recents" -> ChannelList(
                    channels = channels,
                    favorites = liveState.favorites,
                    recents = liveState.recents,
                    lastChannel = liveState.lastChannel,
                    startLastChannel = true,
                    onToggleFavorite = onToggleFavorite,
                    onSelect = onSelect,
                    section = LiveListSection.Recents,
                    title = "Recientes",
                )

                selected.startsWith("group:") -> {
                    val group = selected.removePrefix("group:")
                    val list = remember(channels, group) {
                        channels.filter { (it.group ?: "Sin grupo") == group }
                    }
                    ChannelList(
                        channels = list,
                        favorites = liveState.favorites,
                        recents = liveState.recents,
                        lastChannel = liveState.lastChannel,
                        startLastChannel = true,
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onSelect,
                        section = LiveListSection.Flat,
                        title = group,
                    )
                }

                else -> ChannelList(
                    channels = channels,
                    favorites = liveState.favorites,
                    recents = liveState.recents,
                    lastChannel = liveState.lastChannel,
                    startLastChannel = true,
                    onToggleFavorite = onToggleFavorite,
                    onSelect = onSelect,
                    section = LiveListSection.All,
                )
                }
            },
        )
    }
}

@Composable
internal fun XtreamLiveBrowse(
    config: AppConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    liveState: LiveState,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    xtreamCatalogCache: XtreamCatalogCache,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
) {
    var focused by remember { mutableStateOf<Channel?>(null) }
    val epgGuide = LocalEpgGuide.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val xtream = config.sources.xtream
    val sourceKey = CacheIdentity.xtream(config)
    if (xtream == null || sourceKey == null) {
        SourceFailurePane(
            message = "La configuración no incluye Xtream.",
            onRetry = {},
            onUpdateConfig = onUpdateConfig,
        )
        return
    }

    val client = remember(xtream, config.player.userAgent) {
        XtreamClient(xtream, userAgent = config.player.userAgent)
    }
    var catalog by remember(sourceKey) { mutableStateOf(XtreamCatalog()) }
    var selectedKey by remember(sourceKey) { mutableStateOf("") }
    var hiddenCategoryIds by remember(sourceKey) { mutableStateOf<Set<String>>(emptySet()) }
    var showHidden by remember(sourceKey) { mutableStateOf(false) }
    var loadingCategories by remember(sourceKey) { mutableStateOf(true) }
    var loadingStreams by remember(sourceKey) { mutableStateOf(false) }
    var error by remember(sourceKey) { mutableStateOf<String?>(null) }
    var usingCache by remember(sourceKey) { mutableStateOf(false) }
    var retry by remember(sourceKey) { mutableIntStateOf(0) }

    val minItems = config.sources.xtream?.minCategoryItems ?: 2
    val hideEmpty = config.sources.xtream?.hideEmptyCategories ?: true

    LaunchedEffect(sourceKey, refreshVersion, retry) {
        val cached = withContext(Dispatchers.IO) { xtreamCatalogCache.load(sourceKey) }
        if (cached != null) {
            catalog = cached
            usingCache = true
            if (selectedKey.isBlank()) {
                selectedKey = cached.categories.firstOrNull()?.id.orEmpty()
            }
        }

        loadingCategories = catalog.categories.isEmpty()
        error = null
        when (val result = client.liveCategories()) {
            is XtreamResult.Success -> {
                val updated = catalog.copy(categories = result.value)
                catalog = updated
                usingCache = false
                loadingCategories = false
                if (selectedKey.isBlank()) {
                    selectedKey = result.value.firstOrNull()?.id.orEmpty()
                }
                withContext(Dispatchers.IO) { xtreamCatalogCache.save(sourceKey, updated) }
            }

            is XtreamResult.Failure -> {
                loadingCategories = false
                if (catalog.categories.isEmpty()) error = result.message
            }
        }
    }

    LaunchedEffect(selectedKey, refreshVersion, retry, sourceKey) {
        val categoryId = selectedKey.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        val cached = catalog.streams[categoryId]
        loadingStreams = cached == null
        if (cached != null) usingCache = true

        when (val result = client.liveStreams(categoryId)) {
            is XtreamResult.Success -> {
                val updated = catalog.copy(streams = catalog.streams + (categoryId to result.value))
                catalog = updated
                usingCache = false
                loadingStreams = false
                val usable = XtreamCategoryFilter.usableStreams(result.value)
                if (hideEmpty && minItems > 0 && usable.size < minItems) {
                    hiddenCategoryIds = hiddenCategoryIds + categoryId
                }
                withContext(Dispatchers.IO) { xtreamCatalogCache.save(sourceKey, updated) }
            }

            is XtreamResult.Failure -> {
                loadingStreams = false
                if (cached == null) error = result.message
            }
        }
    }

    val visibleCategories = remember(
        catalog.categories,
        hiddenCategoryIds,
        showHidden,
    ) {
        if (showHidden) {
            catalog.categories
        } else {
            catalog.categories.filter {
                XtreamCategoryFilter.isVisibleCategory(it, hiddenCategoryIds)
            }
        }
    }
    val railEntries = remember(visibleCategories) {
        visibleCategories.map { LiveRailEntry(it.id, it.name) }
    }

    LaunchedEffect(railEntries, selectedKey, showHidden) {
        if (railEntries.none { it.id == selectedKey }) {
            selectedKey = railEntries.firstOrNull()?.id.orEmpty()
        }
    }

    val fallbackChannel = remember(catalog.streams, selectedKey, visibleCategories) {
        catalog.streams[selectedKey]?.firstOrNull()?.let { stream ->
            client.liveChannel(
                stream = stream,
                categoryName = visibleCategories.firstOrNull { it.id == selectedKey }?.name
                    ?: "Xtream",
                providerLabel = null,
            )
        }
    }

    CompositionLocalProvider(LocalLiveFocus provides { focused = it }) {
    when {
        loadingCategories && catalog.categories.isEmpty() -> BrowseShell(
            rail = {},
            content = { CenteredMessage("Cargando canales…") },
        )

        error != null && catalog.categories.isEmpty() -> SourceFailurePane(
            message = error!!,
            onRetry = { retry++ },
            onUpdateConfig = onUpdateConfig,
        )

        else -> BrowseShell(
            rail = {
                BrowseRail(
                    items = railEntries,
                    selected = railEntries.firstOrNull { it.id == selectedKey },
                    label = LiveRailEntry::label,
                    onSelect = { selectedKey = it.id },
                    footer = if (hiddenCategoryIds.isNotEmpty() || showHidden) {
                        {
                            QuickChip(
                                label = if (showHidden) {
                                    "Ocultar vacías"
                                } else {
                                    "Mostrar ocultas (${hiddenCategoryIds.size})"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showHidden = !showHidden },
                            )
                        }
                    } else {
                        null
                    },
                )
            },
            preview = {
                LivePreviewPane(
                    channel = focused ?: fallbackChannel,
                    nowNext = (focused ?: fallbackChannel)?.let { epgGuide?.nowNext(it, nowMs) },
                    nowMs = nowMs,
                    playerConfig = playerConfig,
                    networkConfig = networkConfig,
                )
            },
            content = {
                val category = catalog.categories.firstOrNull { it.id == selectedKey }
                val streams = catalog.streams[selectedKey]
                when {
                    selectedKey.isBlank() -> CenteredMessage("No hay categorías")
                    loadingStreams && streams == null -> CenteredMessage("Cargando canales…")
                    error != null && streams == null -> SourceFailurePane(
                        message = error!!,
                        onRetry = { retry++ },
                        onUpdateConfig = onUpdateConfig,
                    )

                    else -> {
                        val xtreamChannels = remember(streams, category?.name) {
                            XtreamCategoryFilter.usableStreams(streams.orEmpty()).map { stream ->
                                client.liveChannel(
                                    stream = stream,
                                    categoryName = category?.name ?: "Xtream",
                                    providerLabel = null,
                                )
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (usingCache) {
                                Text(
                                    text = "Lista en caché",
                                    color = StreamColors.Favorite,
                                    fontSize = 14.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ChannelList(
                                    channels = xtreamChannels,
                                    favorites = liveState.favorites,
                                    recents = liveState.recents,
                                    lastChannel = liveState.lastChannel,
                                    startLastChannel = true,
                                    onToggleFavorite = onToggleFavorite,
                                    onSelect = onSelect,
                                    section = LiveListSection.Flat,
                                    title = category?.name ?: "Canales",
                                    useBaseNumbers = false,
                                )
                            }
                        }
                    }
                }
            },
        )
    }
    }
}

internal enum class LiveListSection { All, Favorites, Recents, Flat }

internal sealed interface LiveListRow {
    val key: String

    data class Header(
        override val key: String,
        val title: String,
    ) : LiveListRow

    data class Item(
        override val key: String,
        val channel: Channel,
        val favorite: Boolean,
        val showGroup: Boolean,
        val number: Int,
    ) : LiveListRow
}

@Composable
internal fun ChannelList(
    channels: List<Channel>,
    favorites: List<String>,
    recents: List<String>,
    lastChannel: String?,
    startLastChannel: Boolean,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    section: LiveListSection = LiveListSection.All,
    title: String? = null,
    showChannelNumbers: Boolean = true,
    useBaseNumbers: Boolean = true,
) {
    val visible = remember(channels, section, favorites, recents) {
        when (section) {
            LiveListSection.Favorites -> LiveState.resolve(favorites, channels)
            LiveListSection.Recents -> LiveState.resolve(recents, channels)
            else -> channels
        }
    }
    val numberByKey = remember(channels) {
        channels.mapIndexed { index, channel -> channel.liveKey() to index + 1 }.toMap()
    }
    val favoriteKeys = remember(favorites) { favorites.toSet() }
    val rows = remember(visible, section, title, numberByKey, favoriteKeys, useBaseNumbers) {
        buildList {
            fun addItems(list: List<Channel>, prefix: String, showGroup: Boolean) {
                list.forEachIndexed { index, channel ->
                    add(
                        LiveListRow.Item(
                            key = "$prefix-$index-${channel.liveKey()}",
                            channel = channel,
                            favorite = channel.liveKey() in favoriteKeys,
                            showGroup = showGroup,
                            number = if (useBaseNumbers) {
                                numberByKey[channel.liveKey()] ?: (index + 1)
                            } else {
                                index + 1
                            },
                        ),
                    )
                }
            }

            when (section) {
                LiveListSection.All -> {
                    val favoriteChannels = LiveState.resolve(favorites, visible)
                    if (favoriteChannels.isNotEmpty()) {
                        add(LiveListRow.Header("h-favorites", "Favoritos"))
                        addItems(favoriteChannels, "favorites", showGroup = true)
                    }

                    val recentChannels = LiveState.resolve(recents, visible)
                        .filterNot { it.liveKey() in favoriteKeys }
                    if (recentChannels.isNotEmpty()) {
                        add(LiveListRow.Header("h-recents", "Recientes"))
                        addItems(recentChannels, "recents", showGroup = true)
                    }

                    visible.groupBy { it.group ?: "Sin grupo" }.forEach { (group, groupChannels) ->
                        add(LiveListRow.Header("h-group-$group", group))
                        addItems(groupChannels, "group-$group", showGroup = false)
                    }
                }

                LiveListSection.Favorites,
                LiveListSection.Recents,
                LiveListSection.Flat,
                -> {
                    title?.let { add(LiveListRow.Header("h-title", it)) }
                    addItems(visible, section.name.lowercase(), showGroup = false)
                }
            }
        }
    }
    val showLogos = remember(channels) {
        channels.mapNotNull { it.logo?.takeIf(String::isNotBlank) }.distinct().size > 1
    }
    val reportFocus = LocalLiveFocus.current
    val epgGuide = LocalEpgGuide.current
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(epgGuide) {
        if (epgGuide == null) return@LaunchedEffect
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }
    val focusKey = remember(lastChannel, startLastChannel, visible) {
        if (startLastChannel) {
            lastChannel?.takeIf { key -> visible.any { it.liveKey() == key } }
        } else {
            null
        }
    }
    val focusIndex = remember(rows, focusKey) {
        rows.indexOfFirst { it is LiveListRow.Item && it.channel.liveKey() == focusKey }
            .takeIf { it >= 0 }
            ?: rows.indexOfFirst { it is LiveListRow.Item }
    }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var focusRequested by remember { mutableStateOf(false) }

    // Focus the last channel only once per visit.
    LaunchedEffect(focusIndex) {
        if (!focusRequested && focusIndex >= 0) {
            focusRequested = true
            listState.scrollToItem(focusIndex)
            delay(150)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "${visible.size} canales",
                color = StreamColors.TextMuted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty()) {
            CenteredMessage("No hay canales")
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    when (row) {
                        is LiveListRow.Header -> Text(
                            text = row.title.uppercase(),
                            color = StreamColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
                        )

                        is LiveListRow.Item -> LiveChannelRow(
                            channel = row.channel,
                            favorite = row.favorite,
                            showGroup = row.showGroup,
                            showLogo = showLogos,
                            number = row.number,
                            showNumber = showChannelNumbers,
                            epgNowNext = epgGuide?.nowNext(row.channel, nowMs),
                            nowMs = nowMs,
                            focusModifier = if (index == focusIndex) {
                                Modifier.focusRequester(focusRequester)
                            } else {
                                Modifier
                            },
                            onFocused = { reportFocus?.invoke(row.channel) },
                            onSelect = {
                                val channelIndex = visible.indexOfFirst {
                                    it.url == row.channel.url && it.name == row.channel.name
                                }
                                onSelect(
                                    PlaybackRequest(
                                        channel = row.channel,
                                        queue = visible,
                                        index = channelIndex.coerceAtLeast(0),
                                    ),
                                )
                            },
                            onToggleFavorite = { onToggleFavorite(row.channel) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LiveChannelRow(
    channel: Channel,
    favorite: Boolean,
    showGroup: Boolean,
    showLogo: Boolean,
    number: Int,
    showNumber: Boolean,
    epgNowNext: EpgNowNext?,
    nowMs: Long,
    focusModifier: Modifier,
    onFocused: () -> Unit,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) StreamColors.AccentBright else Color.Transparent

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onSelect,
            modifier = Modifier
                .weight(1f)
                .then(focusModifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                }
                .border(1.dp, borderColor, shape),
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = StreamColors.TextPrimary,
                focusedContainerColor = StreamColors.AccentDeep,
                focusedContentColor = StreamColors.TextPrimary,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showNumber) {
                    Text(
                        text = number.toString(),
                        color = StreamColors.TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(30.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 32.dp)
                        .background(StreamColors.SurfaceSunken, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (showLogo) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            placeholder = painterResource(R.drawable.ic_channel_placeholder),
                            error = painterResource(R.drawable.ic_channel_placeholder),
                            fallback = painterResource(R.drawable.ic_channel_placeholder),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(3.dp),
                        )
                    } else {
                        Text(
                            text = channel.name.trim().take(1).uppercase(),
                            color = StreamColors.AccentBright,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(0.42f)) {
                    Text(
                        text = channel.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val details = listOfNotNull(
                        channel.group?.takeIf { showGroup && it.isNotBlank() },
                        channel.provider?.takeIf(String::isNotBlank),
                    ).joinToString(" \u00b7 ")
                    if (details.isNotEmpty()) {
                        Text(
                            text = details,
                            color = StreamColors.TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(0.58f)) {
                    val current = epgNowNext?.current
                    if (current != null) {
                        val left = EpgGuide.formatTimeLeft(current.stopMs - nowMs)
                        Text(
                            text = current.title + if (left.isNotEmpty()) "  \u00b7  $left" else "",
                            color = StreamColors.TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(4.dp))
                        ProgressLine(
                            fraction = (
                                (nowMs - current.startMs).toFloat() /
                                    (current.stopMs - current.startMs).coerceAtLeast(1L)
                                ).coerceIn(0f, 1f),
                            height = 2,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        StarButton(favorite = favorite, onClick = onToggleFavorite)
    }
}

@Composable
internal fun StarButton(favorite: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(36.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (favorite) {
                    "Quitar de favoritos"
                } else {
                    "Añadir a favoritos"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (favorite) "\u2605" else "\u2606",
            color = when {
                focused -> StreamColors.TextPrimary
                favorite -> StreamColors.Favorite
                else -> StreamColors.TextMuted
            },
            fontSize = 18.sp,
        )
    }
}
