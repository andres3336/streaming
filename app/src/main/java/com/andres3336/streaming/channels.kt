package com.andres3336.streaming

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
import androidx.compose.ui.res.painterResource
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
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.live.liveKey
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.ui.LivePreviewPane
import com.andres3336.streaming.ui.LocalLiveFocus
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

/** Pinned category for the curated gist list; it sorts on top of Xtream. */
private const val GIST_CATEGORY = "__gist"
private const val GIST_LABEL = "_Mis canales"

/**
 * One TV browser for both live sources. The curated M3U list is the `_Mis canales`
 * category pinned on top of the Xtream categories, so there are no source sub-tabs.
 */
@Composable
internal fun TvBrowseScreen(
    config: AppConfig,
    gistChannels: List<Channel>,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    liveState: LiveState,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    xtreamCatalogCache: XtreamCatalogCache,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
    initialCategoryId: String? = null,
) {
    val xtream = config.sources.xtream
    val sourceKey = remember(config) { CacheIdentity.xtream(config) }
    val client = remember(xtream, config.player.userAgent) {
        xtream?.let { XtreamClient(it, userAgent = config.player.userAgent) }
    }
    var catalog by remember(sourceKey) { mutableStateOf(XtreamCatalog()) }
    var selectedKey by remember(sourceKey) {
        mutableStateOf(if (gistChannels.isNotEmpty()) GIST_CATEGORY else "")
    }
    var focused by remember { mutableStateOf<Channel?>(null) }
    var hiddenCategoryIds by remember(sourceKey) { mutableStateOf<Set<String>>(emptySet()) }
    var showHidden by remember(sourceKey) { mutableStateOf(false) }
    var loadingCategories by remember(sourceKey) { mutableStateOf(client != null) }
    var loadingStreams by remember(sourceKey) { mutableStateOf(false) }
    var error by remember(sourceKey) { mutableStateOf<String?>(null) }
    var usingCache by remember(sourceKey) { mutableStateOf(false) }
    var retry by remember(sourceKey) { mutableIntStateOf(0) }

    val minItems = config.sources.xtream?.minCategoryItems ?: 2
    val hideEmpty = config.sources.xtream?.hideEmptyCategories ?: true
    val hideCategories = config.sources.xtream?.hideCategories.orEmpty()

    LaunchedEffect(initialCategoryId) {
        if (!initialCategoryId.isNullOrBlank()) selectedKey = initialCategoryId
    }

    LaunchedEffect(sourceKey, refreshVersion, retry) {
        val client = client ?: return@LaunchedEffect
        val key = sourceKey ?: return@LaunchedEffect

        val cached = withContext(Dispatchers.IO) { xtreamCatalogCache.load(key) }
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
                withContext(Dispatchers.IO) { xtreamCatalogCache.save(key, updated) }
            }

            is XtreamResult.Failure -> {
                loadingCategories = false
                if (catalog.categories.isEmpty()) error = result.message
            }
        }
    }

    LaunchedEffect(selectedKey, refreshVersion, retry, sourceKey) {
        val client = client ?: return@LaunchedEffect
        val key = sourceKey ?: return@LaunchedEffect
        val categoryId = selectedKey.takeIf { it.isNotBlank() && it != GIST_CATEGORY }
            ?: return@LaunchedEffect
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
                withContext(Dispatchers.IO) { xtreamCatalogCache.save(key, updated) }
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
        hideCategories,
        showHidden,
    ) {
        catalog.categories.filter { category ->
            if (XtreamCategoryFilter.isHiddenByConfig(category.name, hideCategories)) {
                false
            } else if (showHidden) {
                true
            } else {
                XtreamCategoryFilter.isVisibleCategory(category, hiddenCategoryIds)
            }
        }
    }

    val railEntries = remember(gistChannels, visibleCategories) {
        buildList {
            if (gistChannels.isNotEmpty()) add(LiveRailEntry(GIST_CATEGORY, GIST_LABEL))
            visibleCategories.forEach { add(LiveRailEntry(it.id, it.name)) }
        }
    }

    LaunchedEffect(railEntries, selectedKey, loadingCategories) {
        if (loadingCategories) return@LaunchedEffect
        if (railEntries.none { it.id == selectedKey }) {
            selectedKey = railEntries.firstOrNull()?.id.orEmpty()
        }
    }

    // The preview belongs to the selected category, not to the previous one.
    LaunchedEffect(selectedKey) { focused = null }

    val isGist = selectedKey == GIST_CATEGORY
    val streams = catalog.streams[selectedKey]
    val fallbackChannel = remember(selectedKey, streams, client) {
        if (isGist) {
            null
        } else {
            streams?.firstOrNull()?.let { stream ->
                client?.liveChannel(
                    stream = stream,
                    providerLabel = null,
                )
            }
        }
    }
    val previewChannel = focused ?: fallbackChannel
    val previewSource = remember(previewChannel, gistChannels) {
        when {
            previewChannel == null -> null
            gistChannels.any { it.url == previewChannel.url } -> "Gist"
            else -> "Xtream"
        }
    }

    CompositionLocalProvider(LocalLiveFocus provides { focused = it }) {
        BrowseShell(
            rail = {
                BrowseRail(
                    items = railEntries,
                    selected = railEntries.firstOrNull { it.id == selectedKey },
                    label = LiveRailEntry::label,
                    onSelect = { selectedKey = it.id },
                    footer = if (hasHiddenCategories(hiddenCategoryIds, showHidden)) {
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
                    channel = previewChannel,
                    playerConfig = playerConfig,
                    networkConfig = networkConfig,
                    source = previewSource,
                )
            },
            content = {
                when {
                    selectedKey.isBlank() && loadingCategories ->
                        CenteredMessage("Cargando canales…")

                    selectedKey.isBlank() && error != null -> SourceFailurePane(
                        message = error!!,
                        onRetry = { retry++ },
                        onUpdateConfig = onUpdateConfig,
                    )

                    selectedKey.isBlank() -> CenteredMessage("No hay canales")

                    isGist -> ChannelList(
                        channels = gistChannels,
                        favorites = liveState.favorites,
                        lastChannel = liveState.lastChannel,
                        startLastChannel = true,
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onSelect,
                        section = LiveListSection.All,
                        showChannelNumbers = config.ui.showChannelNumbers,
                    )

                    loadingStreams && streams == null -> CenteredMessage("Cargando canales…")

                    error != null && streams == null -> SourceFailurePane(
                        message = error!!,
                        onRetry = { retry++ },
                        onUpdateConfig = onUpdateConfig,
                    )

                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        if (usingCache) {
                            Text(
                                text = "Lista en caché",
                                color = StreamColors.Favorite,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        ChannelList(
                            channels = remember(streams, client) {
                                (streams.orEmpty())
                                    .let(XtreamCategoryFilter::usableStreams)
                                    .map { stream ->
                                        client!!.liveChannel(
                                            stream = stream,
                                            providerLabel = null,
                                        )
                                    }
                            },
                            favorites = liveState.favorites,
                            lastChannel = liveState.lastChannel,
                            startLastChannel = true,
                            onToggleFavorite = onToggleFavorite,
                            onSelect = onSelect,
                            section = LiveListSection.Flat,
                            showChannelNumbers = config.ui.showChannelNumbers,
                            useBaseNumbers = false,
                        )
                    }
                }
            },
        )
    }
}

private fun hasHiddenCategories(hidden: Set<String>, showHidden: Boolean): Boolean =
    hidden.isNotEmpty() || showHidden

internal enum class LiveListSection { All, Flat }

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
        val number: Int,
    ) : LiveListRow
}

@Composable
internal fun ChannelList(
    channels: List<Channel>,
    favorites: List<String>,
    lastChannel: String?,
    startLastChannel: Boolean,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    section: LiveListSection = LiveListSection.All,
    showChannelNumbers: Boolean = true,
    useBaseNumbers: Boolean = true,
) {
    val visible = channels
    val numberByKey = remember(channels) {
        channels.mapIndexed { index, channel -> channel.liveKey() to index + 1 }.toMap()
    }
    val favoriteKeys = remember(favorites) { favorites.toSet() }
    val rows = remember(visible, section, numberByKey, favoriteKeys, useBaseNumbers) {
        buildList {
            fun addItems(list: List<Channel>, prefix: String) {
                list.forEachIndexed { index, channel ->
                    add(
                        LiveListRow.Item(
                            key = "$prefix-$index-${channel.liveKey()}",
                            channel = channel,
                            favorite = channel.liveKey() in favoriteKeys,
                            number = if (useBaseNumbers) {
                                numberByKey[channel.liveKey()] ?: (index + 1)
                            } else {
                                index + 1
                            },
                        ),
                    )
                }
            }

            if (section == LiveListSection.All) {
                val favoriteChannels = LiveState.resolve(favorites, visible)
                if (favoriteChannels.isNotEmpty()) {
                    add(LiveListRow.Header("h-favorites", "Favoritos"))
                    addItems(favoriteChannels, "favorites")
                }
            }

            addItems(visible, section.name.lowercase())
        }
    }
    val showLogos = remember(channels) {
        channels.mapNotNull { it.logo?.takeIf(String::isNotBlank) }.distinct().size > 1
    }
    val reportFocus = LocalLiveFocus.current
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
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(6.dp))

        if (visible.isEmpty()) {
            CenteredMessage("No hay canales")
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    when (row) {
                        is LiveListRow.Header -> Text(
                            text = row.title.uppercase(),
                            color = StreamColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp),
                        )

                        is LiveListRow.Item -> LiveChannelRow(
                            channel = row.channel,
                            favorite = row.favorite,
                            showLogo = showLogos,
                            number = row.number,
                            showNumber = showChannelNumbers,
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
                                        number = row.number,
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
    showLogo: Boolean,
    number: Int,
    showNumber: Boolean,
    focusModifier: Modifier,
    onFocused: () -> Unit,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) StreamColors.AccentBright else Color.Transparent

    Surface(
        onClick = onSelect,
        onLongClick = onToggleFavorite,
        modifier = Modifier
            .fillMaxWidth()
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
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Favourite marker: painted only when set, no focus stop and no column.
            Text(
                text = "\u2605",
                color = if (favorite) StreamColors.Favorite else Color.Transparent,
                fontSize = 13.sp,
                modifier = Modifier.width(14.dp),
            )
            if (showNumber) {
                Text(
                    text = number.toString(),
                    color = StreamColors.TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(24.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 26.dp)
                    .background(StreamColors.SurfaceSunken, RoundedCornerShape(5.dp)),
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
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                    )
                } else {
                    Text(
                        text = channel.name.trim().take(1).uppercase(),
                        color = StreamColors.AccentBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                val details = channel.provider?.takeIf(String::isNotBlank).orEmpty()
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
        }
    }
}
