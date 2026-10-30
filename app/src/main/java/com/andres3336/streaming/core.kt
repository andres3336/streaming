package com.andres3336.streaming

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.M3uFetch
import com.andres3336.streaming.data.cache.CacheIdentity
import com.andres3336.streaming.data.cache.ChannelCache
import com.andres3336.streaming.data.cache.XtreamCatalogCache
import com.andres3336.streaming.data.M3uParser
import com.andres3336.streaming.data.M3uRepository
import com.andres3336.streaming.data.config.ConfigFetch
import com.andres3336.streaming.data.config.ConfigRepository
import com.andres3336.streaming.data.config.ConfigCache
import com.andres3336.streaming.data.config.ConfigResult
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.ConfigMerger
import com.andres3336.streaming.data.config.ConfigParser
import com.andres3336.streaming.data.config.LiveMode
import com.andres3336.streaming.data.config.SourceHealth
import com.andres3336.streaming.data.config.SourceHealthChecker
import com.andres3336.streaming.data.config.SourcesConfig
import com.andres3336.streaming.data.epg.EpgCache
import com.andres3336.streaming.data.epg.EpgGuide
import com.andres3336.streaming.data.epg.EpgRepository
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.live.LiveStateStore
import com.andres3336.streaming.data.live.liveKey
import com.andres3336.streaming.data.media.LanguageTag
import com.andres3336.streaming.data.playback.LastPlayback
import com.andres3336.streaming.data.playback.LastPlaybackStore
import com.andres3336.streaming.data.playback.PlaybackEntry
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.playback.PlaybackStore
import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.data.settings.DeviceSettingsStore
import com.andres3336.streaming.data.settings.HomeLauncher
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.player.VideoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Locale

internal val LocalEpgGuide = compositionLocalOf<EpgGuide?> { null }

internal sealed interface AppState {
    data object Loading : AppState
    data class Error(val message: String) : AppState
    data class Ready(
        val channels: List<Channel>,
        val config: AppConfig,
        val sourceLabel: String,
        val etag: String?,
        val gistError: String? = null,
    ) : AppState
}

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? android.content.ContextWrapper)?.baseContext
    }
    return null
}

@Composable
internal fun StreamingApp() {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val configRepository = remember(client) { ConfigRepository(BuildConfig.CONFIG_URL, client) }
    val m3uRepository = remember(client) { M3uRepository(client) }
    val sourceHealthChecker = remember(client, m3uRepository) {
        SourceHealthChecker(client, m3uRepository)
    }
    val configCache = remember { ConfigCache(context) }
    val channelCache = remember { ChannelCache(context) }
    val xtreamCatalogCache = remember { XtreamCatalogCache(context) }
    val epgCache = remember { EpgCache(context) }
    val epgRepository = remember(client) { EpgRepository(client) }
    val settingsStore = remember { DeviceSettingsStore(context) }
    val playbackStore = remember { PlaybackStore(context) }
    val lastPlaybackStore = remember { LastPlaybackStore(context) }
    val liveStateStore = remember { LiveStateStore(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf<AppState>(AppState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    var etag by remember { mutableStateOf<String?>(null) }
    var lastReady by remember { mutableStateOf<AppState.Ready?>(null) }
    var centralConfig by remember { mutableStateOf<AppConfig?>(null) }
    var deviceSettings by remember { mutableStateOf(DeviceSettings()) }
    var playbackEntries by remember { mutableStateOf<List<PlaybackEntry>>(emptyList()) }
    var lastPlayback by remember { mutableStateOf<LastPlayback?>(null) }
    var lastPlaybackLoaded by remember { mutableStateOf(false) }
    var liveState by remember { mutableStateOf(LiveState()) }
    var epgGuide by remember { mutableStateOf<EpgGuide?>(null) }
    var playing by remember { mutableStateOf<PlaybackRequest?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var refreshVersion by remember { mutableIntStateOf(0) }
    var forceConfigRefresh by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var pendingRefresh by remember { mutableStateOf(false) }
    var hasStopped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceSettings = withContext(Dispatchers.IO) { settingsStore.load() }
        playbackEntries = withContext(Dispatchers.IO) { playbackStore.load() }
        lastPlayback = withContext(Dispatchers.IO) { lastPlaybackStore.load() }
        lastPlaybackLoaded = true
        liveState = withContext(Dispatchers.IO) { liveStateStore.load() }
    }

    val activity = context.findActivity()
    val autoStartRequested = remember {
        activity?.intent?.getBooleanExtra(MainActivity.EXTRA_AUTO_START, false) == true
    }
    var autoStartHandled by remember { mutableStateOf(false) }

    LaunchedEffect(deviceSettings.useAsLauncher) {
        deviceSettings.useAsLauncher?.let { enabled ->
            withContext(Dispatchers.IO) { HomeLauncher.setEnabled(context, enabled) }
        }
    }

    LaunchedEffect(retry) {
        state = AppState.Loading
        val loaded = loadApp(
            context = context,
            configRepository = configRepository,
            m3uRepository = m3uRepository,
            configCache = configCache,
            channelCache = channelCache,
            etag = if (forceConfigRefresh) null else etag,
            lastReady = lastReady,
        )
        forceConfigRefresh = false
        state = loaded
        if (loaded is AppState.Ready) {
            lastReady = loaded
            etag = loaded.etag
            centralConfig = loaded.config
        }
    }

    LaunchedEffect(refreshTick) {
        if (refreshTick == 0) return@LaunchedEffect
        if (playing != null) {
            pendingRefresh = true
            return@LaunchedEffect
        }

        val current = state as? AppState.Ready ?: return@LaunchedEffect
        refreshing = true
        val refreshed = refreshApp(
            configRepository = configRepository,
            m3uRepository = m3uRepository,
            configCache = configCache,
            channelCache = channelCache,
            etag = if (forceConfigRefresh) null else current.etag,
        )
        forceConfigRefresh = false
        refreshing = false

        if (refreshed != null) {
            if (playing != null) {
                pendingRefresh = true
            } else {
                state = refreshed
                lastReady = refreshed
                etag = refreshed.etag
                centralConfig = refreshed.config
                refreshVersion++
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> hasStopped = true
                Lifecycle.Event.ON_RESUME -> {
                    if (hasStopped) {
                        hasStopped = false
                        if (playing != null) pendingRefresh = true else refreshTick++
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onSelect: (PlaybackRequest) -> Unit = { request ->
        val last = LastPlayback.fromChannel(request.channel)
        lastPlayback = last
        scope.launch(Dispatchers.IO) { lastPlaybackStore.save(last) }

        if (!request.channel.isVod) {
            val updated = liveState.remember(request.channel.liveKey())
            if (updated != liveState) {
                liveState = updated
                scope.launch(Dispatchers.IO) { liveStateStore.save(updated) }
            }
        }

        val savedPosition = playbackEntries
            .firstOrNull { it.url == request.channel.url && it.isResumable }
            ?.positionMs
            ?: 0L

        playing = if (request.startPositionMs > 0L) {
            request
        } else {
            request.copy(startPositionMs = savedPosition)
        }
    }

    val onToggleFavoriteKey: (String) -> Unit = { key ->
        val updated = liveState.toggleFavorite(key)
        if (updated != liveState) {
            liveState = updated
            scope.launch(Dispatchers.IO) { liveStateStore.save(updated) }
        }
    }

    val onToggleFavorite: (Channel) -> Unit = { channel ->
        onToggleFavoriteKey(channel.liveKey())
    }

    val onProgress: (Channel, Long, Long) -> Unit = { channel, position, duration ->
        PlaybackEntry.fromProgress(channel, position, duration)?.let { entry ->
            val updated = (playbackEntries.filterNot { it.url == channel.url } + entry)
                .filter(PlaybackEntry::isResumable)
                .sortedByDescending(PlaybackEntry::updatedAt)
                .take(50)

            playbackEntries = updated
            scope.launch(Dispatchers.IO) { playbackStore.save(updated) }
        }
    }

    val onFinished: (Channel) -> Unit = { channel ->
        val updated = playbackEntries.filterNot { it.url == channel.url }
        if (updated.size != playbackEntries.size) {
            playbackEntries = updated
            scope.launch(Dispatchers.IO) { playbackStore.save(updated) }
        }
        lastPlayback = null
        scope.launch(Dispatchers.IO) { lastPlaybackStore.clear() }
    }

    val closePlayback = {
        playing = null
        if (pendingRefresh) {
            pendingRefresh = false
            refreshTick++
        }
    }

    val recoverSource = {
        playing = null
        forceConfigRefresh = true
        refreshTick += 1
    }

    LaunchedEffect(
        state,
        deviceSettings.autoResumeOnWake,
        autoStartRequested,
        lastPlaybackLoaded,
        lastPlayback,
    ) {
        if (!autoStartRequested || autoStartHandled) return@LaunchedEffect
        if (state !is AppState.Ready || !lastPlaybackLoaded) return@LaunchedEffect

        autoStartHandled = true
        activity?.intent?.removeExtra(MainActivity.EXTRA_AUTO_START)

        if (deviceSettings.autoResumeOnWake == false) return@LaunchedEffect
        val last = lastPlayback ?: return@LaunchedEffect
        if (playing != null) return@LaunchedEffect

        val position = if (last.isLive) {
            0L
        } else {
            playbackEntries.firstOrNull { it.url == last.url }?.positionMs ?: 0L
        }
        playing = PlaybackRequest(last.toChannel(), startPositionMs = position)
    }

    val effectiveConfig = centralConfig?.let { ConfigMerger.apply(it, deviceSettings) }
    LaunchedEffect(
        effectiveConfig?.epg?.url,
        effectiveConfig?.epg?.refreshHours,
        refreshTick,
    ) {
        val config = effectiveConfig ?: return@LaunchedEffect
        val sourceKey = CacheIdentity.epg(config)
        if (sourceKey == null) {
            epgGuide = null
            return@LaunchedEffect
        }
        epgGuide = withContext(Dispatchers.IO) {
            epgRepository.load(config.epg, sourceKey, epgCache)
        }
    }

    var startCacheCleared by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveConfig?.cache?.clearOnStart) {
        if (!startCacheCleared && effectiveConfig?.cache?.clearOnStart == true) {
            startCacheCleared = true
            withContext(Dispatchers.IO) { VideoCache.clear(context) }
        }
    }
    val checkSources: suspend () -> SourceHealth = {
        val config = effectiveConfig ?: (state as? AppState.Ready)?.config
        if (config == null) {
            SourceHealth()
        } else {
            withContext(Dispatchers.IO) { sourceHealthChecker.check(config) }
        }
    }

    when (val current = state) {
        is AppState.Loading -> LoadingScreen()
        is AppState.Error -> ErrorScreen(
            message = current.message,
            onRetry = { retry++ },
            onUpdateConfig = {
                forceConfigRefresh = true
                retry++
            },
        )

        is AppState.Ready -> {
            val activeConfig = effectiveConfig ?: current.config
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalEpgGuide provides epgGuide.takeIf { activeConfig.ui.showNowNext },
                // The app owns its text scale: the TV's system font scale (1.4 on
                // the test device) would otherwise inflate every screen and clip
                // titles. `ui.fontScale` stays the single source of truth.
                LocalDensity provides Density(
                    density = density.density * activeConfig.ui.uiScale,
                    fontScale = activeConfig.ui.fontScale,
                ),
            ) {
                ReadyScreen(
                    ready = current,
                    config = activeConfig,
                    settings = deviceSettings,
                    selected = playing,
                    onSelect = onSelect,
                    onClose = closePlayback,
                    playbackEntries = playbackEntries,
                    onProgress = onProgress,
                    onFinished = onFinished,
                    onRecoverSource = recoverSource,
                    liveState = liveState,
                    onToggleFavorite = onToggleFavorite,
                    onToggleFavoriteKey = onToggleFavoriteKey,
                    refreshVersion = refreshVersion,
                    xtreamCatalogCache = xtreamCatalogCache,
                    onCheckSources = checkSources,
                    refreshing = refreshing,
                    onRefreshConfig = {
                        if (playing == null) {
                            forceConfigRefresh = true
                            refreshTick++
                        }
                    },
                    onSettingsChange = { updated ->
                        deviceSettings = updated
                        scope.launch(Dispatchers.IO) { settingsStore.save(updated) }
                    },
                )
            }
        }
    }
}

internal suspend fun loadApp(
    context: Context,
    configRepository: ConfigRepository,
    m3uRepository: M3uRepository,
    configCache: ConfigCache,
    channelCache: ChannelCache,
    etag: String?,
    lastReady: AppState.Ready?,
): AppState {
    if (BuildConfig.CONFIG_URL.isBlank()) {
        val channels = withContext(Dispatchers.IO) {
            context.assets.open("demo.m3u").bufferedReader().use { reader ->
                M3uParser.parse(reader.readText())
            }
        }
        return AppState.Ready(
            channels = channels,
            config = AppConfig(
                version = 1,
                sources = SourcesConfig(LiveMode.GIST),
            ),
            sourceLabel = "Demo",
            etag = null,
        )
    }

    return when (val fetched = configRepository.fetch(etag)) {
        is ConfigFetch.MissingUrl -> AppState.Error("Falta la URL de configuración")

        is ConfigFetch.NotModified -> lastReady
            ?: loadCached(configCache, channelCache, m3uRepository)
            ?: AppState.Error("La configuración no cambió y no hay copia local")

        is ConfigFetch.Failure -> loadCached(configCache, channelCache, m3uRepository)
            ?: lastReady?.let {
                loadFromConfig(
                    config = it.config,
                    etag = it.etag,
                    sourceLabel = it.sourceLabel,
                    m3uRepository = m3uRepository,
                    channelCache = channelCache,
                )
            }
            ?: AppState.Error(fetched.message)

        is ConfigFetch.Success -> {
            configCache.save(fetched.raw)
            loadFromConfig(
                config = fetched.config,
                etag = fetched.etag,
                sourceLabel = liveSourceLabel(fetched.config),
                m3uRepository = m3uRepository,
                channelCache = channelCache,
            )
        }
    }
}

internal fun liveSourceLabel(config: AppConfig): String = when {
    config.sources.m3uUrl != null && config.sources.xtream != null -> "Gist + Xtream"
    config.sources.xtream != null -> "Xtream"
    else -> "Gist"
}

internal suspend fun loadCached(
    configCache: ConfigCache,
    channelCache: ChannelCache,
    m3uRepository: M3uRepository,
): AppState? {
    val cached = configCache.load() ?: return null
    val parsed = ConfigParser.parse(cached)
    if (parsed !is ConfigResult.Valid) return null

    val config = parsed.config
    return loadFromConfig(
        config = config,
        etag = null,
        sourceLabel = "${liveSourceLabel(config)} · caché",
        m3uRepository = m3uRepository,
        channelCache = channelCache,
    )
}

internal suspend fun loadFromConfig(
    config: AppConfig,
    etag: String?,
    sourceLabel: String,
    m3uRepository: M3uRepository,
    channelCache: ChannelCache,
): AppState {
    val m3uUrl = config.sources.m3uUrl
    if (m3uUrl == null) {
        return if (config.sources.xtream == null) {
            AppState.Error("La configuración no incluye ninguna fuente")
        } else {
            AppState.Ready(
                channels = emptyList(),
                config = config,
                sourceLabel = "Canales · Xtream",
                etag = etag,
            )
        }
    }

    val sourceKey = CacheIdentity.m3u(config)
    return when (val playlist = m3uRepository.load(
        url = m3uUrl,
        userAgent = config.player.userAgent,
        headers = config.player.headers,
    )) {
        is M3uFetch.Success -> {
            sourceKey?.let { channelCache.save(it, playlist.channels) }
            AppState.Ready(
                channels = playlist.channels,
                config = config,
                sourceLabel = sourceLabel,
                etag = etag,
            )
        }

        is M3uFetch.Failure -> {
            val cached = sourceKey?.let { channelCache.load(it) }
            when {
                cached != null -> AppState.Ready(
                    channels = cached,
                    config = config,
                    sourceLabel = "Lista en caché",
                    etag = etag,
                    gistError = playlist.message,
                )

                config.sources.xtream != null -> AppState.Ready(
                    channels = emptyList(),
                    config = config,
                    sourceLabel = "Gist no disponible",
                    etag = etag,
                    gistError = playlist.message,
                )

                else -> AppState.Error(playlist.message)
            }
        }
    }
}

internal suspend fun refreshApp(
    configRepository: ConfigRepository,
    m3uRepository: M3uRepository,
    configCache: ConfigCache,
    channelCache: ChannelCache,
    etag: String?,
): AppState.Ready? = when (val fetched = configRepository.fetch(etag)) {
    is ConfigFetch.Success -> {
        configCache.save(fetched.raw)
        when (val loaded = loadFromConfig(
            config = fetched.config,
            etag = fetched.etag,
            sourceLabel = liveSourceLabel(fetched.config),
            m3uRepository = m3uRepository,
            channelCache = channelCache,
        )) {
            is AppState.Ready -> loaded
            else -> null
        }
    }

    else -> null
}

internal const val FAVORITES_CATEGORY = "__favorites__"

internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

internal fun yearOnly(value: String?): String? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return text.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: text
}

internal fun XtreamMovie.toChannel(client: XtreamClient): Channel = Channel(
    name = name,
    url = client.movieUrl(this),
    group = "Películas",
    logo = logo,
    tvgId = id,
    language = LanguageTag.normalize(language),
    isVod = true,
    year = year,
    imdbId = imdbId,
    tmdbId = tmdbId,
)
