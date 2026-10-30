package com.andres3336.streaming

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.view.KeyEvent as AndroidKeyEvent
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.M3uFetch
import com.andres3336.streaming.data.cache.CacheIdentity
import com.andres3336.streaming.data.cache.ChannelCache
import com.andres3336.streaming.data.cache.XtreamCatalog
import com.andres3336.streaming.data.cache.XtreamCatalogCache
import com.andres3336.streaming.data.M3uParser
import com.andres3336.streaming.data.M3uRepository
import com.andres3336.streaming.data.config.ConfigFetch
import com.andres3336.streaming.data.config.ConfigRepository
import com.andres3336.streaming.data.config.ConfigCache
import com.andres3336.streaming.data.config.ConfigResult
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.CacheConfig
import com.andres3336.streaming.data.config.ConfigMerger
import com.andres3336.streaming.data.config.ConfigParser
import com.andres3336.streaming.data.config.LiveConfig
import com.andres3336.streaming.data.config.LiveMode
import com.andres3336.streaming.data.config.NetworkConfig
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.SourceHealth
import com.andres3336.streaming.data.config.SourceHealthChecker
import com.andres3336.streaming.data.config.SourcesConfig
import com.andres3336.streaming.data.config.SubtitlesConfig
import com.andres3336.streaming.data.config.UiConfig
import com.andres3336.streaming.data.config.XtreamConfig
import com.andres3336.streaming.data.diagnostics.ErrorLogEntry
import com.andres3336.streaming.data.diagnostics.ErrorLogStore
import com.andres3336.streaming.data.epg.EpgCache
import com.andres3336.streaming.data.epg.EpgGuide
import com.andres3336.streaming.data.epg.EpgNowNext
import com.andres3336.streaming.data.epg.EpgRepository
import com.andres3336.streaming.data.live.ChannelSearch
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.live.LiveStateStore
import com.andres3336.streaming.data.live.liveKey
import com.andres3336.streaming.data.media.toMediaItem
import com.andres3336.streaming.data.playback.LastPlayback
import com.andres3336.streaming.data.playback.LastPlaybackStore
import com.andres3336.streaming.data.playback.PlaybackEntry
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.playback.PlaybackStore
import com.andres3336.streaming.data.search.SearchCatalogCache
import com.andres3336.streaming.data.search.SearchIndex
import com.andres3336.streaming.data.search.SearchItem
import com.andres3336.streaming.data.search.SearchKind
import com.andres3336.streaming.data.subtitles.MediaDescriptor
import com.andres3336.streaming.data.subtitles.OpenSubtitlesClient
import com.andres3336.streaming.data.subtitles.SubtitleCandidate
import com.andres3336.streaming.data.subtitles.SubtitleCache
import com.andres3336.streaming.data.subtitles.SubtitleOffsetStore
import com.andres3336.streaming.data.subtitles.SubtitleRepository
import com.andres3336.streaming.data.subtitles.SubtitleResult
import com.andres3336.streaming.data.subtitles.SrtCodec
import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.data.settings.DeviceSettingsStore
import com.andres3336.streaming.data.settings.HomeLauncher
import com.andres3336.streaming.data.settings.SettingsOptions
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamCategoryFilter
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.data.xtream.XtreamSeriesInfo
import com.andres3336.streaming.player.ExternalPlayer
import com.andres3336.streaming.player.HlsFallback
import com.andres3336.streaming.player.NetworkPolicy
import com.andres3336.streaming.player.QualityOption
import com.andres3336.streaming.player.QualitySelection
import com.andres3336.streaming.player.StreamStatsFormat
import com.andres3336.streaming.player.TrackChoice
import com.andres3336.streaming.player.TrackChoices
import com.andres3336.streaming.player.VideoCache
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StreamingApp()
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_START = "auto_start"
    }
}

private val LocalEpgGuide = compositionLocalOf<EpgGuide?> { null }

private sealed interface AppState {
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

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) return current
        current = (current as? android.content.ContextWrapper)?.baseContext
    }
    return null
}

@Composable
private fun StreamingApp() {
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

    val onToggleFavorite: (Channel) -> Unit = { channel ->
        val updated = liveState.toggleFavorite(channel.liveKey())
        if (updated != liveState) {
            liveState = updated
            scope.launch(Dispatchers.IO) { liveStateStore.save(updated) }
        }
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
                LocalDensity provides Density(
                    density = density.density * activeConfig.ui.uiScale,
                    fontScale = density.fontScale * activeConfig.ui.fontScale,
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

private suspend fun loadApp(
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

private fun liveSourceLabel(config: AppConfig): String = when {
    config.sources.m3uUrl != null && config.sources.xtream != null -> "Gist + Xtream"
    config.sources.xtream != null -> "Xtream"
    else -> "Gist"
}

private suspend fun loadCached(
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

private suspend fun loadFromConfig(
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

private suspend fun refreshApp(
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

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Streaming",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Cargando configuración…",
                color = Color(0xFF94A3B8),
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onUpdateConfig: () -> Unit,
) {
    val updateFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { updateFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No se pudo iniciar",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onUpdateConfig,
                    modifier = Modifier.focusRequester(updateFocus),
                ) {
                    Text("Actualizar configuración", fontSize = 20.sp)
                }
                Button(onClick = onRetry) {
                    Text("Reintentar", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Actualizar configuración descarga de nuevo el gist y reintenta la fuente. " +
                    "Si falla, se conserva la última configuración válida.",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReadyScreen(
    ready: AppState.Ready,
    config: AppConfig,
    settings: DeviceSettings,
    selected: PlaybackRequest?,
    onSelect: (PlaybackRequest) -> Unit,
    onClose: () -> Unit,
    playbackEntries: List<PlaybackEntry>,
    onProgress: (Channel, Long, Long) -> Unit,
    onFinished: (Channel) -> Unit,
    onRecoverSource: () -> Unit,
    liveState: LiveState,
    onToggleFavorite: (Channel) -> Unit,
    refreshVersion: Int,
    xtreamCatalogCache: XtreamCatalogCache,
    onCheckSources: suspend () -> SourceHealth,
    refreshing: Boolean,
    onRefreshConfig: () -> Unit,
    onSettingsChange: (DeviceSettings) -> Unit,
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("live") }
    var liveQuery by remember { mutableStateOf("") }

    BackHandler(enabled = selected != null) { onClose() }

    val request = selected
    if (request != null) {
        PlaybackScreen(
            request = request,
            playerConfig = config.player,
            networkConfig = config.network,
            cacheConfig = config.cache,
            liveConfig = config.live,
            uiConfig = config.ui,
            subtitlesConfig = config.subtitles,
            onProgress = onProgress,
            onFinished = onFinished,
            onNext = onSelect,
            onAspectChange = { mode -> onSettingsChange(settings.copy(aspectMode = mode)) },
            onQualityChange = { quality ->
                onSettingsChange(settings.copy(qualityPreference = quality))
            },
            onRecoverSource = onRecoverSource,
            onClose = onClose,
        )
        return
    }

    if (config.forceUpdate) {
        ForceUpdateScreen(onUpdateConfig = onRefreshConfig)
        return
    }

    val xtream = config.sources.xtream
    val showLive = config.sources.m3uUrl != null && "live" in config.ui.sections
    val showChannels = xtream != null && "channels" in config.ui.sections
    val showMovies = xtream != null && "movies" in config.ui.sections
    val showSeries = xtream != null && "series" in config.ui.sections
    val showContinue = playbackEntries.isNotEmpty()
    val showSearch = showLive || showChannels || showMovies || showSeries
    val firstSection = when {
        showLive -> "live"
        showChannels -> "channels"
        showContinue -> "continue"
        showMovies -> "movies"
        showSeries -> "series"
        else -> "settings"
    }
    val activeSection = when {
        section == "live" && !showLive -> firstSection
        section == "channels" && !showChannels -> firstSection
        section == "movies" && !showMovies -> firstSection
        section == "series" && !showSeries -> firstSection
        section == "continue" && !showContinue -> firstSection
        section == "search" && !showSearch -> firstSection
        else -> section
    }

    val headerText = when (activeSection) {
        "live" -> if (ready.channels.isEmpty() && ready.gistError != null) {
            "Gist · no disponible"
        } else if (ready.sourceLabel == "Lista en caché") {
            "Gist · lista en caché · ${ready.channels.size} canales"
        } else {
            "Gist · ${ready.channels.size} canales"
        }
        "channels" -> "Xtream · Canales"
        "movies" -> "Xtream · Películas"
        "series" -> "Xtream · Series"
        "continue" -> "Continuar viendo"
        "search" -> "Buscar en todo"
        "settings" -> "Ajustes · solo este TV"
        else -> "Streaming"
    }

    val content: @Composable () -> Unit = {
        when (activeSection) {
            "movies" -> {
                if (xtream != null) {
                    XtreamMoviesScreen(
                        xtream = xtream,
                        playerConfig = config.player,
                        refreshVersion = refreshVersion,
                        onUpdateConfig = onRefreshConfig,
                        onPlay = { onSelect(PlaybackRequest(it)) },
                    )
                }
            }

            "series" -> {
                if (xtream != null) {
                    XtreamSeriesScreen(
                        xtream = xtream,
                        playerConfig = config.player,
                        refreshVersion = refreshVersion,
                        onUpdateConfig = onRefreshConfig,
                        onPlay = onSelect,
                    )
                }
            }

            "continue" -> ContinueWatchingScreen(
                entries = playbackEntries,
                onSelect = onSelect,
            )

            "settings" -> SettingsScreen(
                config = config,
                settings = settings,
                refreshing = refreshing,
                onRefreshConfig = onRefreshConfig,
                onCheckSources = onCheckSources,
                onSettingsChange = onSettingsChange,
                onClearCache = { VideoCache.clear(context) },
            )

            "live" -> {
                val gistError = ready.gistError
                if (gistError != null && ready.channels.isEmpty()) {
                    SourceFailurePane(
                        message = gistError,
                        onRetry = onRefreshConfig,
                        onUpdateConfig = onRefreshConfig,
                    )
                } else {
                    GistLiveBrowse(
                        channels = ready.channels,
                        liveState = liveState,
                        query = liveQuery,
                        onQueryChange = { liveQuery = it },
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onSelect,
                    )
                }
            }

            "channels" -> {
                if (xtream != null) {
                    XtreamLiveBrowse(
                        config = config,
                        refreshVersion = refreshVersion,
                        onUpdateConfig = onRefreshConfig,
                        liveState = liveState,
                        query = liveQuery,
                        onQueryChange = { liveQuery = it },
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onSelect,
                        xtreamCatalogCache = xtreamCatalogCache,
                    )
                }
            }

            "search" -> UniversalSearchScreen(
                channels = ready.channels,
                config = config,
                refreshVersion = refreshVersion,
                onUpdateConfig = onRefreshConfig,
                onPlay = onSelect,
            )

            else -> GistLiveBrowse(
                channels = ready.channels,
                liveState = liveState,
                query = liveQuery,
                onQueryChange = { liveQuery = it },
                onToggleFavorite = onToggleFavorite,
                onSelect = onSelect,
            )
        }
    }

    if (config.ui.navigation == "rail") {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220))
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            Column(modifier = Modifier.width(210.dp).fillMaxHeight()) {
                Text(
                    text = "Streaming",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                SectionBar(
                    section = activeSection,
                    showLive = showLive,
                    showChannels = showChannels,
                    showMovies = showMovies,
                    showSeries = showSeries,
                    showContinue = showContinue,
                    showSearch = showSearch,
                    showSettings = true,
                    vertical = true,
                    onSelect = { section = it },
                )
            }
            Spacer(Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text(
                    text = headerText,
                    color = Color(0xFF94A3B8),
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B1220))
                .padding(horizontal = 48.dp, vertical = 24.dp),
        ) {
            Text(
                text = "Streaming",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = headerText,
                color = Color(0xFF94A3B8),
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(12.dp))
            SectionBar(
                section = activeSection,
                showLive = showLive,
                showChannels = showChannels,
                showMovies = showMovies,
                showSeries = showSeries,
                showContinue = showContinue,
                showSearch = showSearch,
                showSettings = true,
                onSelect = { section = it },
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }

}

@Composable
private fun ForceUpdateScreen(onUpdateConfig: () -> Unit) {
    val updateFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { updateFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Actualización obligatoria",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Esta configuración requiere una actualización antes de continuar.",
                color = Color(0xFF94A3B8),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onUpdateConfig,
                modifier = Modifier.focusRequester(updateFocus),
            ) {
                Text("Actualizar configuración", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun SectionBar(
    section: String,
    showLive: Boolean,
    showChannels: Boolean,
    showMovies: Boolean,
    showSeries: Boolean,
    showContinue: Boolean,
    showSearch: Boolean,
    showSettings: Boolean,
    vertical: Boolean = false,
    onSelect: (String) -> Unit,
) {
    val items = buildList {
        if (showLive) add("live" to "En vivo")
        if (showChannels) add("channels" to "Canales")
        if (showContinue) add("continue" to "Continuar")
        if (showMovies) add("movies" to "Películas")
        if (showSeries) add("series" to "Series")
        if (showSearch) add("search" to "Buscar")
        if (showSettings) add("settings" to "Ajustes")
    }

    if (vertical) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { (id, label) ->
                SectionButton(
                    label = label,
                    selected = section == id,
                    modifier = Modifier.fillMaxWidth(),
                ) { onSelect(id) }
            }
        }
    } else {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { (id, label) ->
                SectionButton(label = label, selected = section == id) { onSelect(id) }
            }
        }
    }
}

@Composable
private fun SectionButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, modifier = modifier) {
        Text(
            text = label,
            color = if (selected) Color(0xFF3B82F6) else Color.White,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BrowseShell(
    rail: @Composable () -> Unit,
    content: @Composable () -> Unit,
    detail: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
        ) {
            rail()
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            content()
        }
        if (detail != null) {
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
            ) {
                detail()
            }
        }
    }
}

@Composable
private fun <T> BrowseRail(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items) { item ->
            Button(
                onClick = { onSelect(item) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label(item),
                    fontSize = 16.sp,
                    fontWeight = if (item == selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BrowsePosterRow(
    name: String,
    image: String?,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = image,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_channel_placeholder),
                error = painterResource(R.drawable.ic_channel_placeholder),
                fallback = painterResource(R.drawable.ic_channel_placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(40.dp)
                    .height(60.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = name,
                fontSize = 18.sp,
                maxLines = 1,
            )
        }
    }
}

private data class LiveRailEntry(
    val id: String,
    val label: String,
)

@Composable
private fun GistLiveBrowse(
    channels: List<Channel>,
    liveState: LiveState,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
) {
    var selected by remember { mutableStateOf("all") }
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

    BrowseShell(
        rail = {
            BrowseRail(
                items = entries,
                selected = entries.firstOrNull { it.id == selected },
                label = LiveRailEntry::label,
                onSelect = { selected = it.id },
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
                    query = query,
                    onQueryChange = onQueryChange,
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
                    query = query,
                    onQueryChange = onQueryChange,
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
                        query = query,
                        onQueryChange = onQueryChange,
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
                    query = query,
                    onQueryChange = onQueryChange,
                    onToggleFavorite = onToggleFavorite,
                    onSelect = onSelect,
                    section = LiveListSection.All,
                )
            }
        },
    )
}

@Composable
private fun XtreamLiveBrowse(
    config: AppConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    liveState: LiveState,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    xtreamCatalogCache: XtreamCatalogCache,
) {
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        BrowseRail(
                            items = railEntries,
                            selected = railEntries.firstOrNull { it.id == selectedKey },
                            label = LiveRailEntry::label,
                            onSelect = { selectedKey = it.id },
                        )
                    }
                    if (hiddenCategoryIds.isNotEmpty() || showHidden) {
                        Button(
                            onClick = { showHidden = !showHidden },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            Text(
                                text = if (showHidden) {
                                    "Ocultar categorías vacías"
                                } else {
                                    "Mostrar ocultas (${hiddenCategoryIds.size})"
                                },
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
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
                                    color = Color(0xFFFBBF24),
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
                                    query = query,
                                    onQueryChange = onQueryChange,
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

private enum class LiveListSection { All, Favorites, Recents, Flat }

private sealed interface LiveListRow {
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
private fun ChannelList(
    channels: List<Channel>,
    favorites: List<String>,
    recents: List<String>,
    lastChannel: String?,
    startLastChannel: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSelect: (PlaybackRequest) -> Unit,
    section: LiveListSection = LiveListSection.All,
    title: String? = null,
    showChannelNumbers: Boolean = true,
    useBaseNumbers: Boolean = true,
) {
    val filtered = remember(channels, query) { ChannelSearch.filter(channels, query) }
    val visible = remember(filtered, section, favorites, recents) {
        when (section) {
            LiveListSection.Favorites -> LiveState.resolve(favorites, filtered)
            LiveListSection.Recents -> LiveState.resolve(recents, filtered)
            else -> filtered
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

    // Focus the last channel only once per visit; filtering while typing must
    // never steal focus from the search field.
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
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                modifier = Modifier
                    .width(420.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        when (event.nativeKeyEvent.keyCode) {
                            AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                                focusManager.moveFocus(FocusDirection.Down)
                                true
                            }

                            AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                                focusManager.moveFocus(FocusDirection.Up)
                                true
                            }

                            else -> false
                        }
                    },
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar canal o grupo…",
                                color = Color(0xFF64748B),
                                fontSize = 18.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (query.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onQueryChange("") }) {
                    Text("Limpiar", fontSize = 16.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (query.isBlank()) {
                    "${visible.size} canales"
                } else {
                    "${visible.size} de ${channels.size} canales"
                },
                color = Color(0xFF64748B),
                fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty()) {
            CenteredMessage(
                if (query.isBlank()) {
                    "No hay canales"
                } else {
                    "No hay canales para \"$query\""
                },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    when (row) {
                        is LiveListRow.Header -> Text(
                            text = row.title,
                            color = Color(0xFF93C5FD),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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
private fun LiveChannelRow(
    channel: Channel,
    favorite: Boolean,
    showGroup: Boolean,
    showLogo: Boolean,
    number: Int,
    showNumber: Boolean,
    epgNowNext: EpgNowNext?,
    nowMs: Long,
    focusModifier: Modifier,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onSelect,
            modifier = Modifier
                .weight(1f)
                .then(focusModifier),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showNumber) {
                    Text(
                        text = number.toString(),
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(44.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                }
                if (showLogo) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        placeholder = painterResource(R.drawable.ic_channel_placeholder),
                        error = painterResource(R.drawable.ic_channel_placeholder),
                        fallback = painterResource(R.drawable.ic_channel_placeholder),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    ChannelInitial(channel.name)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.name,
                        fontSize = 18.sp,
                        maxLines = 1,
                    )
                    val details = listOfNotNull(
                        channel.group?.takeIf { showGroup && it.isNotBlank() },
                        channel.provider?.takeIf(String::isNotBlank),
                    ).joinToString(" · ")
                    if (details.isNotEmpty()) {
                        Text(
                            text = details,
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                    val current = epgNowNext?.current
                    if (current != null) {
                        val left = EpgGuide.formatTimeLeft(current.stopMs - nowMs)
                        Text(
                            text = "Ahora: ${current.title}" +
                                if (left.isNotEmpty()) " · $left" else "",
                            color = Color(0xFF93C5FD),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                    epgNowNext?.next?.let { next ->
                        Text(
                            text = "Después: ${next.title}",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onToggleFavorite,
            modifier = Modifier.width(56.dp),
        ) {
            Text(
                text = if (favorite) "★" else "☆",
                color = if (favorite) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                fontSize = 22.sp,
            )
        }
    }
}

@Composable
private fun XtreamMoviesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    onPlay: (Channel) -> Unit,
) {
    val client = remember(xtream, playerConfig.userAgent) {
        XtreamClient(xtream, userAgent = playerConfig.userAgent)
    }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<XtreamCategory?>(null) }
    var movies by remember { mutableStateOf<List<XtreamMovie>>(emptyList()) }
    var loadingCategories by remember { mutableStateOf(true) }
    var loadingMovies by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<XtreamMovie?>(null) }

    LaunchedEffect(retry, refreshVersion, xtream) {
        loadingCategories = true
        error = null
        when (val result = client.vodCategories()) {
            is XtreamResult.Success -> {
                categories = result.value
                selectedCategory = result.value.firstOrNull()
                loadingCategories = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loadingCategories = false
            }
        }
    }

    LaunchedEffect(selectedCategory, retry, refreshVersion, xtream) {
        val category = selectedCategory ?: return@LaunchedEffect
        loadingMovies = true
        error = null
        when (val result = client.vodStreams(category.id)) {
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
            refreshVersion = refreshVersion,
            onBack = { detail = null },
            onPlay = onPlay,
        )
        return
    }

    when {
        error != null -> SourceFailurePane(
            message = error!!,
            onRetry = { retry++ },
            onUpdateConfig = onUpdateConfig,
        )

        loadingCategories && categories.isEmpty() -> CenteredMessage("Cargando películas…")
        else -> BrowseShell(
            rail = {
                BrowseRail(
                    items = categories,
                    selected = selectedCategory,
                    label = XtreamCategory::name,
                    onSelect = { selectedCategory = it },
                )
            },
            content = {
                if (loadingMovies) {
                    CenteredMessage("Cargando…")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(movies) { movie ->
                            BrowsePosterRow(movie.name, movie.logo) {
                                detail = movie
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun MovieDetail(
    client: XtreamClient,
    movie: XtreamMovie,
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
    val playMovie = {
        onPlay(
            Channel(
                name = current.name,
                url = client.movieUrl(current.copy(extension = movie.extension)),
                group = "Películas",
                logo = current.logo ?: movie.logo,
                tvgId = current.id,
                isVod = true,
                year = current.year ?: movie.year,
                imdbId = current.imdbId ?: movie.imdbId,
                tmdbId = current.tmdbId ?: movie.tmdbId,
            ),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) {
                Text("Volver", fontSize = 16.sp)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = current.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = playMovie,
                modifier = Modifier.focusRequester(detailFocus),
            ) {
                Text("Reproducir", fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (loading) {
            CenteredMessage("Cargando ficha…")
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = current.logo ?: current.backdrop,
                    contentDescription = null,
                    placeholder = painterResource(R.drawable.ic_channel_placeholder),
                    error = painterResource(R.drawable.ic_channel_placeholder),
                    fallback = painterResource(R.drawable.ic_channel_placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(260.dp)
                        .height(390.dp),
                )
                Spacer(Modifier.width(24.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val meta = listOfNotNull(
                        yearOnly(current.year),
                        current.rating?.let { "★ $it" },
                        current.duration,
                    ).joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Text(text = meta, color = Color(0xFF93C5FD), fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    current.genre?.let {
                        Text(text = it, color = Color(0xFF94A3B8), fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    current.plot?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 10,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    current.cast?.let {
                        Text(
                            text = "Reparto: $it",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            maxLines = 3,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    Button(
                        onClick = playMovie,
                    ) {
                        Text("Reproducir", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun XtreamSeriesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
    onPlay: (PlaybackRequest) -> Unit,
) {
    val client = remember(xtream, playerConfig.userAgent) {
        XtreamClient(xtream, userAgent = playerConfig.userAgent)
    }
    var categories by remember { mutableStateOf<List<XtreamCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<XtreamCategory?>(null) }
    var series by remember { mutableStateOf<List<XtreamSeries>>(emptyList()) }
    var loadingCategories by remember { mutableStateOf(true) }
    var loadingSeries by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<XtreamSeries?>(null) }

    LaunchedEffect(retry, refreshVersion, xtream) {
        loadingCategories = true
        error = null
        when (val result = client.seriesCategories()) {
            is XtreamResult.Success -> {
                categories = result.value
                selectedCategory = result.value.firstOrNull()
                loadingCategories = false
            }

            is XtreamResult.Failure -> {
                error = result.message
                loadingCategories = false
            }
        }
    }

    LaunchedEffect(selectedCategory, retry, refreshVersion, xtream) {
        val category = selectedCategory ?: return@LaunchedEffect
        loadingSeries = true
        error = null
        when (val result = client.series(category.id)) {
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
            onUpdateConfig = onUpdateConfig,
            onBack = { detail = null },
            onPlay = onPlay,
        )
        return
    }

    when {
        error != null -> SourceFailurePane(
            message = error!!,
            onRetry = { retry++ },
            onUpdateConfig = onUpdateConfig,
        )

        loadingCategories && categories.isEmpty() -> CenteredMessage("Cargando series…")
        else -> BrowseShell(
            rail = {
                BrowseRail(
                    items = categories,
                    selected = selectedCategory,
                    label = XtreamCategory::name,
                    onSelect = { selectedCategory = it },
                )
            },
            content = {
                if (loadingSeries) {
                    CenteredMessage("Cargando…")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(series) { item ->
                            BrowsePosterRow(item.name, item.cover) { detail = item }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SeriesDetail(
    client: XtreamClient,
    series: XtreamSeries,
    refreshVersion: Int,
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
            Button(onClick = onBack) {
                Text("Volver", fontSize = 16.sp)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = series.name,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        if (!series.plot.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = series.plot,
                color = Color(0xFF94A3B8),
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
            Text(text = meta, color = Color(0xFF93C5FD), fontSize = 16.sp)
        }
        val plot = metaSource?.plot ?: series.plot
        if (!plot.isNullOrBlank() && plot != series.plot) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = plot,
                color = Color(0xFF94A3B8),
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
                                Text(
                                    text = "Temporada ${season.number}",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(season.episodes) { episode ->
                                Button(
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
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "T${episode.season} E${episode.episode} · ${episode.title}",
                                        fontSize = 17.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = Color(0xFF94A3B8),
            fontSize = 18.sp,
        )
    }
}

@Composable
private fun SourceFailurePane(
    message: String,
    onRetry: () -> Unit,
    onUpdateConfig: () -> Unit,
) {
    val updateFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { updateFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No se pudo cargar la fuente",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onUpdateConfig,
                    modifier = Modifier.focusRequester(updateFocus),
                ) {
                    Text("Actualizar configuración", fontSize = 18.sp)
                }
                Button(onClick = onRetry) {
                    Text("Reintentar", fontSize = 18.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Si la actualización falla, se conserva la configuración anterior.",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChannelInitial(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = Color(0xFF93C5FD),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UniversalSearchScreen(
    channels: List<Channel>,
    config: AppConfig,
    refreshVersion: Int,
    onUpdateConfig: () -> Unit,
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

    val movie = selectedMovie
    val series = selectedSeries
    if (movie != null && client != null) {
        MovieDetail(
            client = client,
            movie = movie,
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
                cursorBrush = SolidColor(Color(0xFF3B82F6)),
                modifier = Modifier
                    .width(460.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
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
                                color = Color(0xFF64748B),
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
            Spacer(Modifier.weight(1f))
            Text(
                text = when {
                    loading && index.size() == 0 -> "Cargando catálogo…"
                    loading -> "${index.size()} · cargando más…"
                    query.isBlank() -> "${index.size()} elementos"
                    else -> "${results.size} resultados"
                },
                color = Color(0xFF64748B),
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
                    Button(
                        onClick = {
                            val item = result.item
                            when {
                                item.channel != null -> onPlay(PlaybackRequest(item.channel))
                                item.movie != null -> selectedMovie = item.movie
                                item.series != null -> selectedSeries = item.series
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Text(
                                text = result.item.kind.label,
                                color = Color(0xFF93C5FD),
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
private fun SearchFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) Color(0xFF3B82F6) else Color.White,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ContinueWatchingScreen(
    entries: List<PlaybackEntry>,
    onSelect: (PlaybackRequest) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries, key = { it.url }) { entry ->
            Button(
                onClick = {
                    onSelect(PlaybackRequest(entry.toChannel(), startPositionMs = entry.positionMs))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = entry.logo,
                        contentDescription = null,
                        placeholder = painterResource(R.drawable.ic_channel_placeholder),
                        error = painterResource(R.drawable.ic_channel_placeholder),
                        fallback = painterResource(R.drawable.ic_channel_placeholder),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(48.dp)
                            .height(72.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                        )
                        Text(
                            text = "${formatDuration(entry.positionMs)} / ${formatDuration(entry.durationMs)}",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
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

@Composable
private fun SettingsScreen(
    config: AppConfig,
    settings: DeviceSettings,
    refreshing: Boolean,
    onRefreshConfig: () -> Unit,
    onCheckSources: suspend () -> SourceHealth,
    onSettingsChange: (DeviceSettings) -> Unit,
    onClearCache: () -> Unit,
) {
    val context = LocalContext.current
    var cacheCleared by remember { mutableStateOf(false) }
    var cacheSizeMb by remember { mutableStateOf<Long?>(null) }
    var checkingSources by remember { mutableStateOf(false) }
    var sourceHealth by remember { mutableStateOf<SourceHealth?>(null) }
    val errorLogStore = remember(context) { ErrorLogStore(context) }
    var errorLog by remember { mutableStateOf<List<ErrorLogEntry>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(cacheCleared, config.cache.sizeMb) {
        cacheSizeMb = withContext(Dispatchers.IO) { VideoCache.sizeMb(context) }
    }
    LaunchedEffect(Unit) {
        errorLog = withContext(Dispatchers.IO) { errorLogStore.load() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SettingsHeader("Inicio") }
        item {
            ChoiceRow(
                label = "Iniciar al encender",
                current = (settings.autoStartOnBoot ?: true).toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value ->
                onSettingsChange(settings.copy(autoStartOnBoot = value.toBoolean()))
            }
        }
        item {
            ChoiceRow(
                label = "Reanudar al despertar",
                current = (settings.autoResumeOnWake ?: true).toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value ->
                onSettingsChange(settings.copy(autoResumeOnWake = value.toBoolean()))
            }
        }
        item {
            Column {
                ChoiceRow(
                    label = "Usar Streaming como inicio",
                    current = (settings.useAsLauncher ?: false).toString(),
                    options = listOf("Sí" to "true", "No" to "false"),
                ) { value ->
                    val enabled = value.toBoolean()
                    HomeLauncher.setEnabled(context, enabled)
                    onSettingsChange(settings.copy(useAsLauncher = enabled))
                }
                Text(
                    text = "Si el TV bloquea el inicio automático, activa esto y elige " +
                        "Streaming al pulsar Home. Se puede desactivar cuando quieras.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item { SettingsHeader("Reproducción") }
        item {
            ChoiceRow(
                label = "Búfer",
                current = config.player.bufferMs.toString(),
                options = listOf(
                    "Bajo · 2 s" to "2000",
                    "Normal · 3 s" to "3000",
                    "Alto · 5 s" to "5000",
                ),
            ) { value -> onSettingsChange(settings.copy(bufferMs = value.toIntOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Prebúfer tras corte",
                current = config.player.preBufferMs.toString(),
                options = SettingsOptions.PRE_BUFFER_PRESETS.map {
                    "${it / 1000} s" to it.toString()
                },
            ) { value -> onSettingsChange(settings.copy(preBufferMs = value.toIntOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Latencia baja (en vivo)",
                current = config.player.liveLowLatency.toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value -> onSettingsChange(settings.copy(liveLowLatency = value.toBoolean())) }
        }
        item {
            ChoiceRow(
                label = "Calidad máxima",
                current = config.player.qualityCap,
                options = listOf(
                    "Auto" to "auto",
                    "1080p" to "1080p",
                    "720p" to "720p",
                    "480p" to "480p",
                    "360p" to "360p",
                ),
            ) { value -> onSettingsChange(settings.copy(qualityCap = value)) }
        }
        item {
            ChoiceRow(
                label = "Aspecto",
                current = config.player.aspectMode,
                options = listOf(
                    "Ajustar" to "fit",
                    "Rellenar" to "fill",
                    "Estirar" to "stretch",
                    "Zoom" to "zoom",
                    "Original" to "original",
                    "16:9" to "16:9",
                    "4:3" to "4:3",
                ),
            ) { value -> onSettingsChange(settings.copy(aspectMode = value)) }
        }

        item { SettingsHeader("Interfaz") }
        item {
            ChoiceRow(
                label = "Tamaño del texto",
                current = config.ui.fontScale.toString(),
                options = SettingsOptions.TEXT_SCALES.map {
                    scaleLabel(it) to it.toString()
                },
            ) { value -> onSettingsChange(settings.copy(textScale = value.toFloatOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Escala de la interfaz",
                current = config.ui.uiScale.toString(),
                options = SettingsOptions.UI_SCALES.map {
                    scaleLabel(it) to it.toString()
                },
            ) { value -> onSettingsChange(settings.copy(uiScale = value.toFloatOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Navegación",
                current = config.ui.navigation,
                options = listOf(
                    "Arriba" to "top",
                    "Lateral" to "rail",
                ),
            ) { value -> onSettingsChange(settings.copy(navigation = value)) }
        }
        item {
            ChoiceRow(
                label = "Barra del reproductor",
                current = config.ui.playerBar,
                options = listOf(
                    "Completa" to "full",
                    "Compacta" to "compact",
                ),
            ) { value -> onSettingsChange(settings.copy(playerBar = value)) }
        }
        item {
            Column {
                Text(
                    text = "Botones del reproductor",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "info" to "Info",
                        "aspect" to "Aspecto",
                        "tracks" to "Audio",
                        "quality" to "Calidad",
                        "speed" to "Velocidad",
                        "external" to "Externo",
                    ).forEach { (id, label) ->
                        val enabled = id in config.ui.playerButtons
                        Button(onClick = {
                            val updated = if (enabled) {
                                config.ui.playerButtons - id
                            } else {
                                config.ui.playerButtons + id
                            }
                            onSettingsChange(settings.copy(playerButtons = updated))
                        }) {
                            Text(
                                text = if (enabled) "✓ $label" else label,
                                color = if (enabled) Color(0xFF3B82F6) else Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }

        item { SettingsHeader("Subtítulos") }
        item {
            Column {
                Text(
                    text = if (config.subtitles.apiKey.isNotBlank()) {
                        "OpenSubtitles: configurado (${config.subtitles.languages.joinToString(", ")})"
                    } else {
                        "OpenSubtitles: falta apiKey en el gist"
                    },
                    color = if (config.subtitles.apiKey.isNotBlank()) {
                        Color(0xFF4ADE80)
                    } else {
                        Color(0xFFFBBF24)
                    },
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    label = "Descargar automáticamente",
                    current = (settings.autoDownloadSubtitles ?: config.subtitles.autoDownload).toString(),
                    options = listOf("Sí" to "true", "No" to "false"),
                ) { value ->
                    onSettingsChange(
                        settings.copy(autoDownloadSubtitles = value.toBoolean()),
                    )
                }
            }
        }

        item { SettingsHeader("Caché VOD") }
        item {
            ChoiceRow(
                label = "Tamaño",
                current = config.cache.sizeMb.toString(),
                options = SettingsOptions.CACHE_SIZES.map { "$it MB" to it.toString() },
            ) { value -> onSettingsChange(settings.copy(cacheSizeMb = value.toIntOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Borrar al iniciar",
                current = config.cache.clearOnStart.toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value -> onSettingsChange(settings.copy(clearOnStart = value.toBoolean())) }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = {
                    onClearCache()
                    cacheCleared = true
                    cacheSizeMb = 0L
                }) {
                    Text(
                        text = if (cacheCleared) "Caché borrada" else "Borrar caché",
                        fontSize = 17.sp,
                    )
                }
                Text(
                    text = "Uso actual: ${cacheSizeMb ?: "…"} MB · solo películas y series",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                )
            }
        }

        item { SettingsHeader("Red") }
        item {
            ChoiceRow(
                label = "Solo WiFi/Ethernet",
                current = config.network.wifiOnly.toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value -> onSettingsChange(settings.copy(wifiOnly = value.toBoolean())) }
        }
        item {
            ChoiceRow(
                label = "Ancho de banda máximo",
                current = config.network.maxBitrateKbps.toString(),
                options = SettingsOptions.BITRATES_KBPS.map { rate ->
                    if (rate == 0) "Sin límite" to "0" else "${rate / 1000} Mbps" to rate.toString()
                },
            ) { value -> onSettingsChange(settings.copy(maxBitrateKbps = value.toIntOrNull())) }
        }

        item { SettingsHeader("Fuentes") }
        item {
            ChoiceRow(
                label = "Ocultar categorías con menos de",
                current = (config.sources.xtream?.minCategoryItems ?: 2).toString(),
                options = SettingsOptions.MIN_CATEGORY_ITEMS.map {
                    if (it == 0) "Desactivado" to "0" else "$it canales" to it.toString()
                },
            ) { value -> onSettingsChange(settings.copy(minCategoryItems = value.toIntOrNull())) }
        }
        item {
            ChoiceRow(
                label = "Ocultar categorías vacías",
                current = (config.sources.xtream?.hideEmptyCategories ?: true).toString(),
                options = listOf("Sí" to "true", "No" to "false"),
            ) { value ->
                onSettingsChange(settings.copy(hideEmptyCategories = value.toBoolean()))
            }
        }
        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            if (!checkingSources) {
                                checkingSources = true
                                scope.launch {
                                    sourceHealth = onCheckSources()
                                    checkingSources = false
                                }
                            }
                        },
                        enabled = !checkingSources,
                    ) {
                        Text(
                            text = if (checkingSources) "Comprobando…" else "Comprobar fuentes",
                            fontSize = 17.sp,
                        )
                    }
                    Text(
                        text = "Comprueba la lista M3U y el acceso a Xtream.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                    )
                }

                sourceHealth?.let { health ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = health.summary(),
                        color = if (health.isHealthy) Color(0xFF4ADE80) else Color(0xFFFCA5A5),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    health.liveMessage?.let { message ->
                        Text(
                            text = "En vivo: $message",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                        )
                    }
                    health.xtreamMessage?.let { message ->
                        Text(
                            text = "Xtream: $message",
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        item { SettingsHeader("Diagnóstico") }
        item {
            Column {
                Text(
                    text = if (errorLog.isEmpty()) {
                        "Sin errores registrados."
                    } else {
                        "Últimos errores:"
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 15.sp,
                )
                errorLog.takeLast(5).reversed().forEach { entry ->
                    Text(
                        text = "${entry.channel} · ${entry.code} · ${entry.message}",
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    errorLogStore.clear()
                    errorLog = emptyList()
                }) {
                    Text("Borrar registro", fontSize = 16.sp)
                }
            }
        }

        item { SettingsHeader("Configuración") }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onRefreshConfig,
                    enabled = !refreshing,
                ) {
                    Text(
                        text = if (refreshing) "Actualizando…" else "Actualizar configuración",
                        fontSize = 17.sp,
                    )
                }
                Text(
                    text = "Se revisa al abrir y al volver a la app.",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                onSettingsChange(DeviceSettings())
                cacheCleared = false
            }) {
                Text("Restablecer ajustes locales", fontSize = 17.sp)
            }
        }
        item {
            Text(
                text = "Los valores del gist se usan cuando no hay un ajuste local. " +
                    "Los cambios se aplican al siguiente inicio de reproducción.",
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF93C5FD),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ChoiceRow(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    Column {
        Text(
            text = label,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (text, value) ->
                Button(onClick = { onSelect(value) }) {
                    Text(
                        text = text,
                        color = if (value == current) Color(0xFF3B82F6) else Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (value == current) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackScreen(
    request: PlaybackRequest,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
    cacheConfig: CacheConfig,
    liveConfig: LiveConfig,
    uiConfig: UiConfig,
    subtitlesConfig: SubtitlesConfig,
    onProgress: (Channel, Long, Long) -> Unit,
    onFinished: (Channel) -> Unit,
    onNext: (PlaybackRequest) -> Unit,
    onAspectChange: (String) -> Unit,
    onQualityChange: (String) -> Unit,
    onRecoverSource: () -> Unit,
    onClose: () -> Unit,
) {
    val channel = request.channel
    val context = LocalContext.current
    val capabilities = remember(channel) { channel.toMediaItem().capabilities }
    val allowed = remember(networkConfig.wifiOnly) {
        NetworkPolicy.isAllowed(context, networkConfig.wifiOnly)
    }
    if (!allowed) {
        NetworkBlockedScreen()
        return
    }

    var retryToken by remember(channel) { mutableIntStateOf(0) }
    var autoRetries by remember(channel) { mutableIntStateOf(0) }
    var errorMessage by remember(channel) { mutableStateOf<String?>(null) }
    var sourceFailure by remember(channel) { mutableStateOf(false) }
    var activeUrl by remember(channel) { mutableStateOf(channel.url) }
    var fallbackTried by remember(channel) { mutableStateOf(false) }
    var showNext by remember(channel) { mutableStateOf(false) }
    var countdown by remember(channel) { mutableIntStateOf(10) }
    var currentTracks by remember(channel) { mutableStateOf(Tracks.EMPTY) }
    var showTracks by remember(channel) { mutableStateOf(false) }
    var showStats by remember(channel) { mutableStateOf(false) }
    var showQuality by remember(channel) { mutableStateOf(false) }
    var showSpeed by remember(channel) { mutableStateOf(false) }
    var showControls by remember(channel) { mutableStateOf(false) }
    var qualityOptions by remember(channel) { mutableStateOf<List<QualityOption>>(emptyList()) }
    var speed by remember(channel) { mutableStateOf(1f) }
    var aspectMode by remember(playerConfig.aspectMode) { mutableStateOf(playerConfig.aspectMode) }
    var decoderName by remember(channel) { mutableStateOf<String?>(null) }
    var bandwidthBps by remember(channel) { mutableStateOf(0L) }
    var streamStats by remember(channel) { mutableStateOf<StreamStats?>(null) }
    val retryFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val tracksButtonFocus = remember { FocusRequester() }
    val aspectButtonFocus = remember { FocusRequester() }
    val firstTrackFocus = remember { FocusRequester() }
    val statsFirstFocus = remember { FocusRequester() }
    val qualityFirstFocus = remember { FocusRequester() }
    val speedFirstFocus = remember { FocusRequester() }
    val controlsFirstFocus = remember { FocusRequester() }
    var aspectTouched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val errorLog = remember(context) { ErrorLogStore(context) }
    val subtitleCache = remember(context) { SubtitleCache(context) }
    val subtitleOffsetStore = remember(context) { SubtitleOffsetStore(context) }
    val subtitleRepository = remember(subtitlesConfig, playerConfig.userAgent) {
        SubtitleRepository(
            config = subtitlesConfig,
            cache = subtitleCache,
            client = OpenSubtitlesClient(
                config = subtitlesConfig,
                userAgent = "Streaming/${BuildConfig.VERSION_NAME}",
            ),
        )
    }
    val mediaDescriptor = remember(channel) { MediaDescriptor.fromChannel(channel) }
    var subtitleFile by remember(channel) { mutableStateOf<File?>(null) }
    var subtitleLanguage by remember(channel) { mutableStateOf("es") }
    var subtitleOffsetMs by remember(channel) {
        mutableStateOf(subtitleOffsetStore.load()[mediaDescriptor.key] ?: 0L)
    }
    var subtitleMessage by remember(channel) { mutableStateOf<String?>(null) }
    var subtitleCandidates by remember(channel) {
        mutableStateOf<List<SubtitleCandidate>>(emptyList())
    }
    var showSubtitleSearch by remember(channel) { mutableStateOf(false) }
    var subtitleSearching by remember(channel) { mutableStateOf(false) }
    val subtitlePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                }.getOrNull()
                if (bytes != null) {
                    val content = SrtCodec.decode(bytes)
                    val file = subtitleRepository.importLocal(
                        mediaKey = mediaDescriptor.key,
                        language = "es",
                        content = content,
                        offsetMs = subtitleOffsetMs,
                    )
                    subtitleLanguage = "es"
                    subtitleFile = file
                    subtitleMessage = null
                } else {
                    subtitleMessage = "No se pudo leer el archivo."
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(
        channel,
        networkConfig,
        cacheConfig,
        retryToken,
        activeUrl,
    ) {
        buildPlayer(
            context = context,
            channel = channel.copy(url = activeUrl),
            config = playerConfig,
            network = networkConfig,
            cache = cacheConfig,
        ).also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (BuildConfig.DEBUG) Log.d("Streaming", "state=$playbackState")

                    if (playbackState == Player.STATE_ENDED && channel.isVod) {
                        onFinished(channel)
                        if (request.next != null) {
                            countdown = 10
                            showNext = true
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("Streaming", "player error ${error.errorCodeName}")
                    val friendly = friendlyPlaybackError(error)
                    errorMessage = friendly
                    sourceFailure = isSourceFailure(error)
                    scope.launch(Dispatchers.IO) {
                        errorLog.record(
                            ErrorLogEntry(
                                timestamp = System.currentTimeMillis(),
                                channel = channel.name,
                                code = error.errorCodeName,
                                message = friendly,
                            ),
                        )
                    }
                }

                override fun onTracksChanged(tracks: Tracks) {
                    currentTracks = tracks
                    qualityOptions = QualitySelection.options(tracks)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (BuildConfig.DEBUG) Log.d("Streaming", "playing=$isPlaying")
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (BuildConfig.DEBUG) {
                        Log.d("Streaming", "video=${videoSize.width}x${videoSize.height}")
                    }
                }
            })

            exoPlayer.addAnalyticsListener(object : AnalyticsListener {
                override fun onVideoDecoderInitialized(
                    eventTime: AnalyticsListener.EventTime,
                    name: String,
                    initializedTimestampMs: Long,
                    initializationDurationMs: Long,
                ) {
                    decoderName = name
                }

                override fun onBandwidthEstimate(
                    eventTime: AnalyticsListener.EventTime,
                    elapsedMs: Int,
                    bytesTransferred: Long,
                    bitrateEstimate: Long,
                ) {
                    bandwidthBps = bitrateEstimate
                }
            })
        }
    }

    LaunchedEffect(player) {
        player.prepare()
        player.playWhenReady = true
        if (request.startPositionMs > 0L) {
            player.seekTo(request.startPositionMs)
        }
        QualitySelection.apply(
            player = player,
            preference = playerConfig.qualityPreference,
            qualityCap = playerConfig.qualityCap,
            maxBitrateKbps = networkConfig.maxBitrateKbps,
        )
        player.setPlaybackSpeed(if (channel.isVod) speed else 1f)
    }

    LaunchedEffect(
        channel,
        subtitlesConfig.enabled,
        subtitlesConfig.autoDownload,
        subtitlesConfig.apiKey,
        subtitleOffsetMs,
    ) {
        if (!channel.isVod || !subtitlesConfig.enabled) return@LaunchedEffect

        val cached = subtitleRepository.cachedAny(mediaDescriptor.key, subtitleOffsetMs)
        if (cached != null) {
            subtitleLanguage = subtitleRepository.languageOf(cached)
            subtitleFile = cached
            return@LaunchedEffect
        }

        if (!subtitlesConfig.autoDownload || subtitlesConfig.apiKey.isBlank()) {
            return@LaunchedEffect
        }

        val downloaded = subtitleRepository.autoFetch(channel, subtitleOffsetMs)
        if (downloaded != null) {
            subtitleLanguage = subtitleRepository.languageOf(downloaded)
            subtitleFile = downloaded
            subtitleMessage = null
        }
    }

    LaunchedEffect(subtitleFile, player, activeUrl) {
        val file = subtitleFile ?: return@LaunchedEffect
        val position = player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.isPlaying
        player.setMediaItem(buildMediaItem(channel.copy(url = activeUrl), playerConfig, file, subtitleLanguage))
        player.prepare()
        if (position > 0L) player.seekTo(position)
        if (wasPlaying) player.play()
    }

    LaunchedEffect(player, channel) {
        if (!channel.isVod) return@LaunchedEffect

        while (true) {
            delay(5_000)
            val duration = player.duration
            if (duration > 0L) {
                onProgress(channel, player.currentPosition, duration)
            }
        }
    }

    LaunchedEffect(showNext, countdown) {
        if (!showNext) return@LaunchedEffect

        if (countdown <= 0) {
            showNext = false
            request.nextRequest()?.let(onNext)
            return@LaunchedEffect
        }

        delay(1_000)
        countdown -= 1
    }

    LaunchedEffect(showNext) {
        if (showNext) {
            delay(150)
            runCatching { nextFocus.requestFocus() }
        }
    }

    LaunchedEffect(errorMessage, channel.isVod, activeUrl) {
        if (errorMessage == null) return@LaunchedEffect

        if (!channel.isVod) {
            val fallback = if (activeUrl == channel.url) {
                channel.fallbackUrl ?: HlsFallback.tsUrl(activeUrl)
            } else {
                null
            }
            if (!fallbackTried && fallback != null && fallback != activeUrl) {
                delay(400)
                fallbackTried = true
                autoRetries = 0
                errorMessage = null
                activeUrl = fallback
                retryToken += 1
                return@LaunchedEffect
            }

            if (autoRetries < playerConfig.retryCount) {
                val delayMs = (playerConfig.retryDelayMs.toLong().coerceAtLeast(500L) * (autoRetries + 1))
                    .coerceAtMost(5_000L)
                delay(delayMs)
                autoRetries += 1
                errorMessage = null
                retryToken += 1
                return@LaunchedEffect
            }
        }

        delay(150)
        runCatching { retryFocus.requestFocus() }
    }

    DisposableEffect(player, channel) {
        onDispose {
            if (channel.isVod) {
                val duration = player.duration
                if (duration > 0L) {
                    onProgress(channel, player.currentPosition, duration)
                }
            }
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player, channel) {
        var resumeOnStart = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnStart = player.isPlaying
                    if (channel.isVod) {
                        val duration = player.duration
                        if (duration > 0L) {
                            onProgress(channel, player.currentPosition, duration)
                        }
                    }
                    player.pause()
                }

                Lifecycle.Event.ON_START -> if (resumeOnStart) player.play()

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val audioChoices = remember(currentTracks) { TrackChoices.audio(currentTracks) }
    val subtitleChoices = remember(currentTracks) { TrackChoices.subtitles(currentTracks) }
    val subtitlesEnabled = remember(currentTracks) { TrackChoices.hasSelectedSubtitles(currentTracks) }

    LaunchedEffect(player, showStats) {
        if (!showStats) return@LaunchedEffect
        while (true) {
            streamStats = readStreamStats(player, channel, decoderName, bandwidthBps, playerConfig)
            delay(1_000)
        }
    }

    val zap: (Int) -> Unit = { delta ->
        val queue = request.queue
        if (!channel.isVod && liveConfig.zapping && queue.size > 1) {
            val currentIndex = request.index
                .takeIf { it in queue.indices }
                ?: queue.indexOfFirst { it.url == channel.url }
            val base = currentIndex.takeIf { it >= 0 } ?: 0
            val next = ((base + delta) % queue.size + queue.size) % queue.size
            onNext(PlaybackRequest(queue[next], queue, next))
        }
    }

    val openExternal: () -> Unit = {
        val intent = ExternalPlayer.buildIntent(channel, playerConfig)
        val hasHandler = intent.resolveActivity(context.packageManager) != null
        if (hasHandler) {
            runCatching {
                context.startActivity(
                    Intent.createChooser(intent, "Abrir en otro reproductor")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure {
                Toast.makeText(context, "No se pudo abrir otro reproductor.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No hay otro reproductor instalado.", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(
        enabled = showTracks || showStats || showQuality || showSpeed || showSubtitleSearch || showControls,
    ) {
        when {
            showStats -> showStats = false
            showSubtitleSearch -> showSubtitleSearch = false
            showTracks -> showTracks = false
            showQuality -> showQuality = false
            showSpeed -> showSpeed = false
            showControls -> showControls = false
        }
    }

    LaunchedEffect(showTracks, showStats, showQuality, showSpeed, showControls) {
        delay(150)
        runCatching {
            when {
                showTracks -> firstTrackFocus.requestFocus()
                showStats -> statsFirstFocus.requestFocus()
                showQuality -> qualityFirstFocus.requestFocus()
                showSpeed -> speedFirstFocus.requestFocus()
                showControls -> controlsFirstFocus.requestFocus()
                else -> tracksButtonFocus.requestFocus()
            }
        }
    }

    LaunchedEffect(aspectMode, aspectTouched) {
        if (aspectTouched) {
            delay(120)
            runCatching { aspectButtonFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || showTracks || showStats || showQuality || showSpeed || showSubtitleSearch || showControls) {
                    return@onPreviewKeyEvent false
                }

                when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    AndroidKeyEvent.KEYCODE_HEADSETHOOK,
                    -> {
                        if (player.isPlaying) player.pause() else player.play()
                        true
                    }

                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        player.play()
                        true
                    }

                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        player.pause()
                        true
                    }

                    AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (!channel.isVod && liveConfig.zapping) {
                            zap(1)
                            true
                        } else {
                            false
                        }
                    }

                    AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> {
                        if (channel.isVod) {
                            player.seekTo((player.currentPosition + 30_000L).coerceAtMost(player.duration))
                            true
                        } else if (liveConfig.zapping) {
                            zap(1)
                            true
                        } else {
                            false
                        }
                    }

                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (!channel.isVod && liveConfig.zapping) {
                            zap(-1)
                            true
                        } else {
                            false
                        }
                    }

                    AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                        if (channel.isVod) {
                            player.seekTo((player.currentPosition - 30_000L).coerceAtLeast(0L))
                            true
                        } else if (liveConfig.zapping) {
                            zap(-1)
                            true
                        } else {
                            false
                        }
                    }

                    AndroidKeyEvent.KEYCODE_MENU -> {
                        showStats = false
                        showTracks = true
                        true
                    }

                    AndroidKeyEvent.KEYCODE_INFO -> {
                        showTracks = false
                        showStats = true
                        true
                    }

                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { viewContext ->
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.player_view, null) as PlayerView).apply {
                    this.player = player
                    keepScreenOn = true
                    applyAspect(this, aspectMode)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.player = player
                applyAspect(view, aspectMode)
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 32.dp,
                    top = if (channel.isVod) 96.dp else 32.dp,
                    end = 32.dp,
                )
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = channel.name,
                color = Color.White,
                fontSize = 18.sp,
            )
            if (!channel.isVod && liveConfig.zapping) {
                Text(
                    text = "CH+ / CH− cambia de canal",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                )
            }
        }

        if (uiConfig.playerBar == "compact") {
            Button(
                onClick = { showControls = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp),
            ) {
                Text("Controles", fontSize = 16.sp)
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if ("info" in uiConfig.playerButtons) {
                    Button(onClick = {
                        showTracks = false
                        showStats = true
                    }) {
                        Text("Info", fontSize = 16.sp)
                    }
                }
                if ("aspect" in uiConfig.playerButtons) {
                    Button(
                        onClick = {
                            aspectTouched = true
                            val next = nextAspectMode(aspectMode)
                            aspectMode = next
                            onAspectChange(next)
                        },
                        modifier = Modifier.focusRequester(aspectButtonFocus),
                    ) {
                        Text("Aspecto: ${aspectLabel(aspectMode)}", fontSize = 15.sp, maxLines = 1)
                    }
                }
                if ("quality" in uiConfig.playerButtons && capabilities.canChangeQuality) {
                    Button(onClick = {
                        showStats = false
                        showTracks = false
                        showSpeed = false
                        showQuality = true
                    }) {
                        Text("Calidad", fontSize = 15.sp, maxLines = 1)
                    }
                }
                if ("speed" in uiConfig.playerButtons && capabilities.canChangeSpeed) {
                    Button(onClick = {
                        showStats = false
                        showTracks = false
                        showQuality = false
                        showSpeed = true
                    }) {
                        Text("Velocidad ${speedLabel(speed)}", fontSize = 15.sp, maxLines = 1)
                    }
                }
                if ("tracks" in uiConfig.playerButtons) {
                    Button(
                        onClick = {
                            showStats = false
                            showQuality = false
                            showSpeed = false
                            showTracks = true
                        },
                        modifier = Modifier.focusRequester(tracksButtonFocus),
                    ) {
                        Text("Audio/Subs", fontSize = 15.sp, maxLines = 1)
                    }
                }
                if ("external" in uiConfig.playerButtons && capabilities.canOpenExternally) {
                    Button(onClick = openExternal) {
                        Text("Externo", fontSize = 15.sp, maxLines = 1)
                    }
                }
            }
        }

        val error = errorMessage
        if (error != null) {
            PlaybackErrorOverlay(
                message = error,
                sourceFailure = sourceFailure,
                retryFocus = retryFocus,
                onRetry = {
                    autoRetries = 0
                    errorMessage = null
                    retryToken += 1
                },
                onUpdateConfig = onRecoverSource,
                onClose = onClose,
            )
        }

        if (showNext) {
            NextEpisodeOverlay(
                title = request.next?.name.orEmpty(),
                seconds = countdown,
                focusRequester = nextFocus,
                onPlayNow = {
                    showNext = false
                    request.nextRequest()?.let(onNext)
                },
                onCancel = { showNext = false },
            )
        }

        if (showTracks) {
            TrackPickerOverlay(
                audio = audioChoices,
                subtitles = subtitleChoices,
                subtitlesEnabled = subtitlesEnabled,
                firstFocus = firstTrackFocus,
                subtitleMessage = subtitleMessage,
                offsetMs = subtitleOffsetMs,
                canSearchSubtitles = channel.isVod && subtitlesConfig.enabled,
                searchingSubtitles = subtitleSearching,
                onSelectAudio = { choice -> applyAudioTrack(player, choice) },
                onSelectSubtitles = { choice -> applySubtitleTrack(player, choice) },
                onDisableSubtitles = { disableSubtitles(player) },
                onOffsetChange = { delta ->
                    val updated = subtitleOffsetMs + delta
                    subtitleOffsetMs = updated
                    scope.launch(Dispatchers.IO) {
                        val offsets = subtitleOffsetStore.load().toMutableMap()
                        offsets[mediaDescriptor.key] = updated
                        subtitleOffsetStore.save(offsets)
                    }
                    if (subtitleFile != null) {
                        subtitleRepository.applyOffset(
                            mediaKey = mediaDescriptor.key,
                            language = subtitleLanguage,
                            offsetMs = updated,
                        )?.let { subtitleFile = it }
                    }
                },
                onResetOffset = {
                    subtitleOffsetMs = 0L
                    scope.launch(Dispatchers.IO) {
                        val offsets = subtitleOffsetStore.load().toMutableMap()
                        offsets.remove(mediaDescriptor.key)
                        subtitleOffsetStore.save(offsets)
                    }
                    if (subtitleFile != null) {
                        subtitleRepository.applyOffset(
                            mediaKey = mediaDescriptor.key,
                            language = subtitleLanguage,
                            offsetMs = 0L,
                        )?.let { subtitleFile = it }
                    }
                },
                onSearchSubtitles = {
                    if (!subtitleSearching) {
                        subtitleSearching = true
                        subtitleMessage = null
                        scope.launch {
                            when (val result = subtitleRepository.search(channel)) {
                                is SubtitleResult.Success -> {
                                    subtitleCandidates = result.value
                                    showSubtitleSearch = true
                                }

                                is SubtitleResult.Failure -> {
                                    subtitleMessage = result.message
                                }
                            }
                            subtitleSearching = false
                        }
                    }
                },
                onOpenSubtitleFile = {
                    subtitlePicker.launch(arrayOf("*/*"))
                },
                onClose = { showTracks = false },
            )
        }

        if (showSubtitleSearch) {
            SubtitleSearchOverlay(
                candidates = subtitleCandidates,
                loading = subtitleSearching,
                message = subtitleMessage,
                firstFocus = firstTrackFocus,
                onSelect = { candidate ->
                    scope.launch {
                        subtitleSearching = true
                        val file = subtitleRepository.download(
                            channel = channel,
                            candidate = candidate,
                            offsetMs = subtitleOffsetMs,
                        )
                        if (file != null) {
                            subtitleLanguage = candidate.language
                            subtitleFile = file
                            subtitleMessage = null
                            showSubtitleSearch = false
                        } else {
                            subtitleMessage = "No se pudo descargar el subtítulo."
                        }
                        subtitleSearching = false
                    }
                },
                onClose = { showSubtitleSearch = false },
            )
        }

        if (showControls) {
            PlayerControlsOverlay(
                buttons = uiConfig.playerButtons,
                isVod = capabilities.canSeek,
                firstFocus = controlsFirstFocus,
                onInfo = {
                    showControls = false
                    showStats = true
                },
                onAspect = {
                    aspectTouched = true
                    val next = nextAspectMode(aspectMode)
                    aspectMode = next
                    onAspectChange(next)
                    showControls = false
                },
                onTracks = {
                    showControls = false
                    showTracks = true
                },
                onQuality = {
                    showControls = false
                    showQuality = true
                },
                onSpeed = {
                    showControls = false
                    showSpeed = true
                },
                onExternal = openExternal,
                onClose = { showControls = false },
            )
        }

        if (showQuality) {
            QualityOverlay(
                options = qualityOptions,
                current = playerConfig.qualityPreference,
                firstFocus = qualityFirstFocus,
                onSelect = { preference ->
                    QualitySelection.apply(
                        player = player,
                        preference = preference,
                        qualityCap = playerConfig.qualityCap,
                        maxBitrateKbps = networkConfig.maxBitrateKbps,
                    )
                    onQualityChange(preference)
                    showQuality = false
                },
                onClose = { showQuality = false },
            )
        }

        if (showSpeed) {
            SpeedOverlay(
                current = speed,
                firstFocus = speedFirstFocus,
                onSelect = { value ->
                    speed = value
                    player.setPlaybackSpeed(value)
                    showSpeed = false
                },
                onClose = { showSpeed = false },
            )
        }

        if (showStats) {
            StreamStatsOverlay(
                stats = streamStats,
                firstFocus = statsFirstFocus,
                onClose = { showStats = false },
            )
        }
    }
}

private data class StreamStats(
    val codec: String,
    val resolution: String,
    val frameRate: String,
    val bitrate: String,
    val buffer: String,
    val bufferConfig: String,
    val droppedFrames: String,
    val decoder: String,
    val bandwidth: String,
    val source: String,
    val kind: String,
)

private fun readStreamStats(
    player: ExoPlayer,
    channel: Channel,
    decoderName: String?,
    bandwidthBps: Long,
    playerConfig: PlayerConfig,
): StreamStats {
    val video = player.videoFormat
    player.videoDecoderCounters?.ensureUpdated()

    val width = video?.width?.takeIf { it > 0 } ?: player.videoSize.width
    val height = video?.height?.takeIf { it > 0 } ?: player.videoSize.height

    return StreamStats(
        codec = StreamStatsFormat.codec(video?.sampleMimeType, video?.codecs),
        resolution = StreamStatsFormat.resolution(width, height),
        frameRate = StreamStatsFormat.frameRate(video?.frameRate ?: 0f),
        bitrate = StreamStatsFormat.bitrate(video?.bitrate?.toLong() ?: 0L),
        buffer = StreamStatsFormat.buffer(player.totalBufferedDuration),
        bufferConfig = "${playerConfig.bufferMs / 1000} s inicial · " +
            "${playerConfig.preBufferMs / 1000} s tras corte",
        droppedFrames = StreamStatsFormat.droppedFrames(
            player.videoDecoderCounters?.droppedBufferCount ?: 0,
        ),
        decoder = decoderName ?: "—",
        bandwidth = StreamStatsFormat.bitrate(bandwidthBps),
        source = StreamStatsFormat.maskedSource(channel.url),
        kind = if (channel.isVod) "VOD" else "En vivo",
    )
}

@Composable
private fun StreamStatsOverlay(
    stats: StreamStats?,
    firstFocus: FocusRequester,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(820.dp)
                .fillMaxHeight(0.96f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Información del stream",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Datos actualizados cada segundo. La URL completa se oculta.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.focusRequester(firstFocus),
                ) {
                    Text("Cerrar", fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StatRow("Tipo", stats?.kind ?: "—")
                    StatRow("Códec de vídeo", stats?.codec ?: "—")
                    StatRow("Resolución", stats?.resolution ?: "—")
                    StatRow("Fotogramas por segundo", stats?.frameRate ?: "—")
                    StatRow("Bitrate de vídeo", stats?.bitrate ?: "—")
                }
                Spacer(Modifier.width(32.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StatRow("Búfer actual", stats?.buffer ?: "—")
                    StatRow("Búfer configurado", stats?.bufferConfig ?: "—")
                    StatRow("Fotogramas descartados", stats?.droppedFrames ?: "—")
                    StatRow("Decodificador", stats?.decoder ?: "—")
                    StatRow("Ancho de banda estimado", stats?.bandwidth ?: "—")
                    StatRow("Fuente", stats?.source ?: "oculta")
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
        )
    }
}

@Composable
private fun TrackPickerOverlay(
    audio: List<TrackChoice>,
    subtitles: List<TrackChoice>,
    subtitlesEnabled: Boolean,
    firstFocus: FocusRequester,
    subtitleMessage: String?,
    offsetMs: Long,
    canSearchSubtitles: Boolean,
    searchingSubtitles: Boolean,
    onSelectAudio: (TrackChoice) -> Unit,
    onSelectSubtitles: (TrackChoice) -> Unit,
    onDisableSubtitles: () -> Unit,
    onOffsetChange: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onSearchSubtitles: () -> Unit,
    onOpenSubtitleFile: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .fillMaxHeight(0.9f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "Audio y subtítulos",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "Audio",
                        color = Color(0xFF93C5FD),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (audio.isEmpty()) {
                    item {
                        Text(
                            text = "Sin pistas de audio.",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    itemsIndexed(audio, key = { _, choice -> "audio-${choice.id}" }) { index, choice ->
                        TrackOptionButton(
                            label = choice.label,
                            selected = choice.selected,
                            focusRequester = if (index == 0) firstFocus else null,
                            onClick = { onSelectAudio(choice) },
                        )
                    }
                }

                item {
                    Text(
                        text = "Subtítulos",
                        color = Color(0xFF93C5FD),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                item {
                    TrackOptionButton(
                        label = "Desactivado",
                        selected = !subtitlesEnabled,
                        focusRequester = if (audio.isEmpty()) firstFocus else null,
                        onClick = onDisableSubtitles,
                    )
                }
                if (subtitles.isEmpty()) {
                    item {
                        Text(
                            text = "Este contenido no incluye subtítulos.",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    itemsIndexed(
                        subtitles,
                        key = { _, choice -> "subtitle-${choice.id}" },
                    ) { _, choice ->
                        TrackOptionButton(
                            label = choice.label,
                            selected = choice.selected,
                            focusRequester = null,
                            onClick = { onSelectSubtitles(choice) },
                        )
                    }
                }

            }

            if (canSearchSubtitles) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSearchSubtitles) {
                        Text(
                            text = if (searchingSubtitles) "Buscando…" else "Buscar subtítulos",
                            fontSize = 15.sp,
                        )
                    }
                    Button(onClick = onOpenSubtitleFile) {
                        Text("Archivo .srt", fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Sincronización: ${offsetSecondsLabel(offsetMs)}",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onOffsetChange(-5_000L) }) { Text("−5 s", fontSize = 13.sp) }
                    Button(onClick = { onOffsetChange(-500L) }) { Text("−0.5 s", fontSize = 13.sp) }
                    Button(onClick = { onOffsetChange(500L) }) { Text("+0.5 s", fontSize = 13.sp) }
                    Button(onClick = { onOffsetChange(5_000L) }) { Text("+5 s", fontSize = 13.sp) }
                    Button(onClick = onResetOffset) { Text("0", fontSize = 13.sp) }
                }
                subtitleMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun TrackOptionButton(
    label: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (selected) Color(0xFF3B82F6) else Color.White,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(
                    text = "✓",
                    color = Color(0xFF3B82F6),
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun QualityOverlay(
    options: List<QualityOption>,
    current: String,
    firstFocus: FocusRequester,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .fillMaxHeight(0.85f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "Calidad de vídeo",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    TrackOptionButton(
                        label = "Auto",
                        selected = current == "auto",
                        focusRequester = firstFocus,
                        onClick = { onSelect("auto") },
                    )
                }
                if (options.isEmpty()) {
                    item {
                        Text(
                            text = "Este canal tiene una sola calidad.",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    if (options.size == 1) {
                        item {
                            Text(
                                text = "Este canal tiene una sola calidad.",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp,
                            )
                        }
                    }
                    itemsIndexed(options, key = { _, option -> option.height }) { _, option ->
                        val preference = "${option.height}p"
                        TrackOptionButton(
                            label = option.label,
                            selected = current == preference,
                            focusRequester = null,
                            onClick = { onSelect(preference) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun SpeedOverlay(
    current: Float,
    firstFocus: FocusRequester,
    onSelect: (Float) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .fillMaxHeight(0.9f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(20.dp),
        ) {
            Text(
                text = "Velocidad de reproducción",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(SettingsOptions.SPEEDS) { index, speed ->
                    TrackOptionButton(
                        label = speedLabel(speed),
                        selected = current == speed,
                        focusRequester = if (index == 0) firstFocus else null,
                        onClick = { onSelect(speed) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", fontSize = 17.sp)
            }
        }
    }
}

private fun speedLabel(speed: Float): String {
    val text = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    return "${text}x"
}

private fun scaleLabel(scale: Float): String = "${(scale * 100).toInt()} %"

private fun yearOnly(value: String?): String? {
    val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return text.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: text
}

@Composable
private fun PlayerControlsOverlay(
    buttons: List<String>,
    isVod: Boolean,
    firstFocus: FocusRequester,
    onInfo: () -> Unit,
    onAspect: () -> Unit,
    onTracks: () -> Unit,
    onQuality: () -> Unit,
    onSpeed: () -> Unit,
    onExternal: () -> Unit,
    onClose: () -> Unit,
) {
    val actions = buildList {
        if ("info" in buttons) add("Info" to onInfo)
        if ("aspect" in buttons) add("Aspecto" to onAspect)
        if ("tracks" in buttons) add("Audio y subtítulos" to onTracks)
        if ("quality" in buttons) add("Calidad" to onQuality)
        if ("speed" in buttons && isVod) add("Velocidad" to onSpeed)
        if ("external" in buttons && isVod) add("Abrir en otro reproductor" to onExternal)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "Controles",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            actions.forEachIndexed { index, (label, action) ->
                Button(
                    onClick = action,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        ),
                ) {
                    Text(label, fontSize = 17.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun SubtitleSearchOverlay(
    candidates: List<SubtitleCandidate>,
    loading: Boolean,
    message: String?,
    firstFocus: FocusRequester,
    onSelect: (SubtitleCandidate) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .fillMaxHeight(0.9f)
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(24.dp),
        ) {
            Text(
                text = "Buscar subtítulos · OpenSubtitles",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            if (loading) {
                CenteredMessage("Buscando…")
            } else if (candidates.isEmpty()) {
                CenteredMessage(message ?: "No hay resultados.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        candidates,
                        key = { _, candidate -> candidate.fileId },
                    ) { index, candidate ->
                        TrackOptionButton(
                            label = buildString {
                                append(candidate.language.uppercase(Locale.ROOT))
                                append(" · ")
                                append(candidate.title.take(60))
                                if (candidate.downloads > 0) {
                                    append(" · ")
                                    append("${candidate.downloads} descargas")
                                }
                            },
                            selected = false,
                            focusRequester = if (index == 0) firstFocus else null,
                            onClick = { onSelect(candidate) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", fontSize = 17.sp)
            }
        }
    }
}

private fun offsetSecondsLabel(offsetMs: Long): String {
    if (offsetMs == 0L) return "0 s"
    return String.format(Locale.US, "%+.1f s", offsetMs / 1_000.0)
}

private fun applyAudioTrack(player: ExoPlayer, choice: TrackChoice) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(TrackSelectionOverride(choice.group, choice.trackIndex))
        .build()
}

private fun applySubtitleTrack(player: ExoPlayer, choice: TrackChoice) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .setOverrideForType(TrackSelectionOverride(choice.group, choice.trackIndex))
        .build()
}

private fun disableSubtitles(player: ExoPlayer) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

@Composable
private fun NextEpisodeOverlay(
    title: String,
    seconds: Int,
    focusRequester: FocusRequester,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Siguiente episodio",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                color = Color(0xFF94A3B8),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Se reproduce en $seconds s",
                color = Color(0xFF3B82F6),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlayNow,
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    Text("Reproducir ahora", fontSize = 18.sp)
                }
                Button(onClick = onCancel) {
                    Text("Cancelar", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    sourceFailure: Boolean,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
    onUpdateConfig: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No se pudo reproducir",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            if (sourceFailure) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Puede ser un problema de la fuente o de la cuenta.",
                    color = Color(0xFF94A3B8),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sourceFailure) {
                    Button(onClick = onUpdateConfig) {
                        Text("Actualizar configuración", fontSize = 18.sp)
                    }
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.focusRequester(retryFocus),
                ) {
                    Text("Reintentar", fontSize = 18.sp)
                }
                Button(onClick = onClose) {
                    Text("Cerrar", fontSize = 18.sp)
                }
            }
        }
    }
}

private fun isSourceFailure(error: PlaybackException): Boolean {
    val cause = error.cause
    return cause is HttpDataSource.InvalidResponseCodeException &&
        (cause.responseCode == 401 || cause.responseCode == 403 || cause.responseCode >= 500)
}

private fun friendlyPlaybackError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "El canal no está disponible en este momento."

    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "No se pudo conectar. Revisa la red del TV."

    PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "El TV no puede decodificar este formato."

    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
        "El contenido ya no está disponible."

    else -> "No se pudo reproducir este contenido."
}

private fun buildPlayer(
    context: Context,
    channel: Channel,
    config: PlayerConfig,
    network: NetworkConfig,
    cache: CacheConfig,
): ExoPlayer {
    val headers = buildMap {
        putAll(config.headers)
        channel.userAgent?.let { put("User-Agent", it) }
        channel.referer?.let { put("Referer", it) }
    }
    val userAgent = channel.userAgent
        ?: config.userAgent
        ?: "Streaming/${BuildConfig.VERSION_NAME}"

    val httpDataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setDefaultRequestProperties(headers)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(network.connectTimeoutMs)
        .setReadTimeoutMs(network.readTimeoutMs)

    val dataSourceFactory: DataSource.Factory =
        if (channel.isVod && cache.vodEnabled) {
            CacheDataSource.Factory()
                .setCache(VideoCache.get(context, cache.sizeMb))
                .setUpstreamDataSourceFactory(httpDataSource)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } else {
            httpDataSource
        }

    val trackSelector = DefaultTrackSelector(context).apply {
        val parameters = buildUponParameters()
        when (config.qualityCap) {
            "1080p" -> parameters.setMaxVideoSize(1920, 1080)
            "720p" -> parameters.setMaxVideoSize(1280, 720)
            "480p" -> parameters.setMaxVideoSize(854, 480)
            "360p" -> parameters.setMaxVideoSize(640, 360)
        }
        if (network.maxBitrateKbps > 0) {
            parameters.setMaxVideoBitrate(network.maxBitrateKbps * 1000)
        }
        setParameters(parameters.build())
    }

    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            maxOf(config.minBufferMs, config.bufferMs),
            maxOf(config.maxBufferMs, config.minBufferMs, config.bufferMs),
            config.bufferMs.coerceAtLeast(500),
            config.preBufferMs.coerceAtLeast(1_000),
        )
        .build()

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            setMediaItem(buildMediaItem(channel, config, null, "es"))
        }
}

private fun buildMediaItem(
    channel: Channel,
    config: PlayerConfig,
    subtitleFile: File?,
    subtitleLanguage: String,
): MediaItem = MediaItem.Builder()
    .setUri(channel.url)
    .apply {
        if (!channel.isVod && config.liveLowLatency) {
            setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(2_000)
                    .build(),
            )
        }
        if (subtitleFile != null) {
            setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(subtitleFile))
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage(subtitleLanguage)
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build(),
                ),
            )
        }
    }
    .build()

private fun aspectResizeMode(value: String): Int = when (value) {
    "fill", "stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

private fun nextAspectMode(current: String): String {
    val modes = SettingsOptions.UI_ASPECT_MODES
    val index = modes.indexOf(current)
    return modes[(index + 1).takeIf { it in modes.indices } ?: 0]
}

private fun aspectLabel(mode: String): String = when (mode) {
    "fill" -> "Rellenar"
    "stretch" -> "Estirar"
    "zoom" -> "Zoom"
    "original" -> "Original"
    "16:9" -> "16:9"
    "4:3" -> "4:3"
    else -> "Ajustar"
}

private fun applyAspect(view: PlayerView, mode: String) {
    val frame = view.videoSurfaceView as? AspectRatioFrameLayout
    when (mode) {
        "16:9" -> {
            frame?.setAspectRatio(16f / 9f)
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        "4:3" -> {
            frame?.setAspectRatio(4f / 3f)
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        else -> {
            frame?.setAspectRatio(0f)
            view.resizeMode = aspectResizeMode(mode)
        }
    }
}

@Composable
private fun NetworkBlockedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220))
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Solo WiFi/Ethernet",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Desactiva \"Solo WiFi/Ethernet\" en Ajustes o conecta el TV a la red local.",
                color = Color(0xFF94A3B8),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
