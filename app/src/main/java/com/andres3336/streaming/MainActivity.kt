package com.andres3336.streaming

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.M3uFetch
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
import com.andres3336.streaming.data.config.LiveMode
import com.andres3336.streaming.data.config.NetworkConfig
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.SourcesConfig
import com.andres3336.streaming.data.config.XtreamConfig
import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.data.settings.DeviceSettingsStore
import com.andres3336.streaming.data.settings.SettingsOptions
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.data.xtream.XtreamSeriesInfo
import com.andres3336.streaming.player.NetworkPolicy
import com.andres3336.streaming.player.VideoCache
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StreamingApp()
            }
        }
    }
}

private sealed interface AppState {
    data object Loading : AppState
    data class Error(val message: String) : AppState
    data class Ready(
        val channels: List<Channel>,
        val config: AppConfig,
        val sourceLabel: String,
        val etag: String?,
    ) : AppState
}

@Composable
private fun StreamingApp() {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val configRepository = remember(client) { ConfigRepository(BuildConfig.CONFIG_URL, client) }
    val m3uRepository = remember(client) { M3uRepository(client) }
    val configCache = remember { ConfigCache(context) }
    val settingsStore = remember { DeviceSettingsStore(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf<AppState>(AppState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    var etag by remember { mutableStateOf<String?>(null) }
    var lastReady by remember { mutableStateOf<AppState.Ready?>(null) }
    var centralConfig by remember { mutableStateOf<AppConfig?>(null) }
    var deviceSettings by remember { mutableStateOf(DeviceSettings()) }
    var playing by remember { mutableStateOf<Channel?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    var pendingRefresh by remember { mutableStateOf(false) }
    var hasStopped by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceSettings = withContext(Dispatchers.IO) { settingsStore.load() }
    }

    LaunchedEffect(retry) {
        state = AppState.Loading
        val loaded = loadApp(context, configRepository, m3uRepository, configCache, etag, lastReady)
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
        val refreshed = refreshApp(configRepository, m3uRepository, configCache, current.etag)
        refreshing = false

        if (refreshed != null) {
            if (playing != null) {
                pendingRefresh = true
            } else {
                state = refreshed
                lastReady = refreshed
                etag = refreshed.etag
                centralConfig = refreshed.config
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

    val closePlayback = {
        playing = null
        if (pendingRefresh) {
            pendingRefresh = false
            refreshTick++
        }
    }

    val effectiveConfig = centralConfig?.let { ConfigMerger.apply(it, deviceSettings) }

    when (val current = state) {
        is AppState.Loading -> LoadingScreen()
        is AppState.Error -> ErrorScreen(current.message) { retry++ }
        is AppState.Ready -> ReadyScreen(
            ready = current,
            config = effectiveConfig ?: current.config,
            settings = deviceSettings,
            selected = playing,
            onSelect = { playing = it },
            onClose = closePlayback,
            refreshing = refreshing,
            onRefreshConfig = { if (playing == null) refreshTick++ },
            onSettingsChange = { updated ->
                deviceSettings = updated
                scope.launch(Dispatchers.IO) { settingsStore.save(updated) }
            },
        )
    }
}

private suspend fun loadApp(
    context: Context,
    configRepository: ConfigRepository,
    m3uRepository: M3uRepository,
    configCache: ConfigCache,
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
            ?: loadCached(configCache, m3uRepository)
            ?: AppState.Error("La configuración no cambió y no hay copia local")

        is ConfigFetch.Failure -> loadCached(configCache, m3uRepository)
            ?: AppState.Error(fetched.message)

        is ConfigFetch.Success -> {
            configCache.save(fetched.raw)
            loadFromConfig(
                config = fetched.config,
                etag = fetched.etag,
                sourceLabel = if (fetched.config.sources.liveMode == LiveMode.GIST) {
                    "Gist"
                } else {
                    "Remoto"
                },
                m3uRepository = m3uRepository,
            )
        }
    }
}

private suspend fun loadCached(
    configCache: ConfigCache,
    m3uRepository: M3uRepository,
): AppState? {
    val cached = configCache.load() ?: return null
    val parsed = ConfigParser.parse(cached)
    if (parsed !is ConfigResult.Valid) return null

    val config = parsed.config
    return loadFromConfig(
        config = config,
        etag = null,
        sourceLabel = if (config.sources.liveMode == LiveMode.GIST) "Gist · caché" else "Remoto · caché",
        m3uRepository = m3uRepository,
    )
}

private suspend fun loadFromConfig(
    config: AppConfig,
    etag: String?,
    sourceLabel: String,
    m3uRepository: M3uRepository,
): AppState {
    val m3uUrl = config.sources.m3uUrl
        ?: return AppState.Error("La configuración no incluye una lista M3U")

    return when (val playlist = m3uRepository.load(
        url = m3uUrl,
        userAgent = config.player.userAgent,
        headers = config.player.headers,
    )) {
        is M3uFetch.Success -> AppState.Ready(
            channels = playlist.channels,
            config = config,
            sourceLabel = sourceLabel,
            etag = etag,
        )

        is M3uFetch.Failure -> AppState.Error(playlist.message)
    }
}

private suspend fun refreshApp(
    configRepository: ConfigRepository,
    m3uRepository: M3uRepository,
    configCache: ConfigCache,
    etag: String?,
): AppState.Ready? = when (val fetched = configRepository.fetch(etag)) {
    is ConfigFetch.Success -> {
        configCache.save(fetched.raw)
        when (val loaded = loadFromConfig(
            config = fetched.config,
            etag = fetched.etag,
            sourceLabel = if (fetched.config.sources.liveMode == LiveMode.GIST) "Gist" else "Remoto",
            m3uRepository = m3uRepository,
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
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
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
            Button(onClick = onRetry) {
                Text("Reintentar", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ReadyScreen(
    ready: AppState.Ready,
    config: AppConfig,
    settings: DeviceSettings,
    selected: Channel?,
    onSelect: (Channel) -> Unit,
    onClose: () -> Unit,
    refreshing: Boolean,
    onRefreshConfig: () -> Unit,
    onSettingsChange: (DeviceSettings) -> Unit,
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("live") }

    BackHandler(enabled = selected != null) { onClose() }

    val channel = selected
    if (channel != null) {
        PlaybackScreen(
            channel = channel,
            playerConfig = config.player,
            networkConfig = config.network,
            cacheConfig = config.cache,
            onClose = onClose,
        )
        return
    }

    val xtream = config.sources.xtream
    val showMovies = xtream != null && "movies" in config.ui.sections
    val showSeries = xtream != null && "series" in config.ui.sections
    val activeSection = when {
        section == "movies" && !showMovies -> "live"
        section == "series" && !showSeries -> "live"
        else -> section
    }

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
            text = when (activeSection) {
                "movies" -> "Xtream · Películas"
                "series" -> "Xtream · Series"
                "settings" -> "Ajustes · solo este TV"
                else -> "${ready.sourceLabel} · ${ready.channels.size} canales"
            },
            color = Color(0xFF94A3B8),
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(12.dp))
        SectionBar(
            section = activeSection,
            showMovies = showMovies,
            showSeries = showSeries,
            showSettings = true,
            onSelect = { section = it },
        )
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (activeSection) {
                "movies" -> {
                    if (xtream != null) {
                        XtreamMoviesScreen(
                            xtream = xtream,
                            playerConfig = config.player,
                            onPlay = { onSelect(it) },
                        )
                    }
                }

                "series" -> {
                    if (xtream != null) {
                        XtreamSeriesScreen(
                            xtream = xtream,
                            playerConfig = config.player,
                            onPlay = { onSelect(it) },
                        )
                    }
                }

                "settings" -> SettingsScreen(
                    config = config,
                    settings = settings,
                    refreshing = refreshing,
                    onRefreshConfig = onRefreshConfig,
                    onSettingsChange = onSettingsChange,
                    onClearCache = { VideoCache.clear(context) },
                )

                else -> ChannelList(ready.channels) { onSelect(it) }
            }
        }
    }
}

@Composable
private fun SectionBar(
    section: String,
    showMovies: Boolean,
    showSeries: Boolean,
    showSettings: Boolean,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionButton("En vivo", selected = section == "live") { onSelect("live") }
        if (showMovies) {
            SectionButton("Películas", selected = section == "movies") { onSelect("movies") }
        }
        if (showSeries) {
            SectionButton("Series", selected = section == "series") { onSelect("series") }
        }
        if (showSettings) {
            SectionButton("Ajustes", selected = section == "settings") { onSelect("settings") }
        }
    }
}

@Composable
private fun SectionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) Color(0xFF3B82F6) else Color.White,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ChannelList(
    channels: List<Channel>,
    onSelect: (Channel) -> Unit,
) {
    val groups = remember(channels) { channels.groupBy { it.group ?: "Sin grupo" } }
    val showLogos = remember(channels) {
        channels.mapNotNull { it.logo?.takeIf(String::isNotBlank) }.distinct().size > 1
    }
    val firstChannel = remember(channels) { channels.firstOrNull() }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(firstChannel) {
        if (firstChannel != null) {
            delay(150)
            runCatching { firstFocus.requestFocus() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        groups.forEach { (group, groupChannels) ->
            item(key = "group-$group") {
                Text(
                    text = group,
                    color = Color(0xFF93C5FD),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(groupChannels) { channel ->
                Button(
                    onClick = { onSelect(channel) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (channel == firstChannel) {
                                Modifier.focusRequester(firstFocus)
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (showLogos) {
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
                        Text(
                            text = channel.name,
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun XtreamMoviesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
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

    LaunchedEffect(retry) {
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

    LaunchedEffect(selectedCategory, retry) {
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

    when {
        error != null -> RetryPane(error!!) { retry++ }
        loadingCategories && categories.isEmpty() -> CenteredMessage("Cargando películas…")
        else -> Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    Button(
                        onClick = { selectedCategory = category },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = category.name,
                            fontSize = 16.sp,
                            fontWeight = if (category == selectedCategory) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            if (loadingMovies) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cargando…",
                        color = Color(0xFF94A3B8),
                        fontSize = 18.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(movies) { movie ->
                        MovieRow(movie) {
                            onPlay(
                                Channel(
                                    name = movie.name,
                                    url = client.movieUrl(movie),
                                    group = "Películas",
                                    logo = movie.logo,
                                    tvgId = movie.id,
                                    isVod = true,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieRow(movie: XtreamMovie, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = movie.logo,
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
                text = movie.name,
                fontSize = 18.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun XtreamSeriesScreen(
    xtream: XtreamConfig,
    playerConfig: PlayerConfig,
    onPlay: (Channel) -> Unit,
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

    LaunchedEffect(retry) {
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

    LaunchedEffect(selectedCategory, retry) {
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
            onBack = { detail = null },
            onPlay = onPlay,
        )
        return
    }

    when {
        error != null -> RetryPane(error!!) { retry++ }
        loadingCategories && categories.isEmpty() -> CenteredMessage("Cargando series…")
        else -> Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categories) { category ->
                    Button(
                        onClick = { selectedCategory = category },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = category.name,
                            fontSize = 16.sp,
                            fontWeight = if (category == selectedCategory) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            if (loadingSeries) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cargando…",
                        color = Color(0xFF94A3B8),
                        fontSize = 18.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(series) { item ->
                        SeriesRow(item) { detail = item }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesRow(series: XtreamSeries, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = series.cover,
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
                text = series.name,
                fontSize = 18.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SeriesDetail(
    client: XtreamClient,
    series: XtreamSeries,
    onBack: () -> Unit,
    onPlay: (Channel) -> Unit,
) {
    var info by remember(series.id) { mutableStateOf<XtreamSeriesInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }

    BackHandler { onBack() }

    LaunchedEffect(series.id, retry) {
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
                maxLines = 2,
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
                error != null -> RetryPane(error!!) { retry++ }
                seriesInfo == null || seriesInfo.seasons.isEmpty() -> {
                    CenteredMessage("No hay episodios")
                }

                else -> LazyColumn(
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
                                    onPlay(
                                        Channel(
                                            name = "${series.name} T${episode.season}E${episode.episode} · ${episode.title}",
                                            url = client.episodeUrl(episode),
                                            group = "Series",
                                            logo = series.cover,
                                            tvgId = episode.id,
                                            isVod = true,
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
private fun RetryPane(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = Color(0xFFFCA5A5),
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Reintentar", fontSize = 18.sp)
            }
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
private fun SettingsScreen(
    config: AppConfig,
    settings: DeviceSettings,
    refreshing: Boolean,
    onRefreshConfig: () -> Unit,
    onSettingsChange: (DeviceSettings) -> Unit,
    onClearCache: () -> Unit,
) {
    var cacheCleared by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                label = "Calidad máxima",
                current = config.player.qualityCap,
                options = listOf(
                    "Auto" to "auto",
                    "1080p" to "1080p",
                    "720p" to "720p",
                    "480p" to "480p",
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
                    "Zoom" to "zoom",
                    "Original" to "original",
                ),
            ) { value -> onSettingsChange(settings.copy(aspectMode = value)) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    onClearCache()
                    cacheCleared = true
                }) {
                    Text(
                        text = if (cacheCleared) "Caché borrada" else "Borrar caché",
                        fontSize = 17.sp,
                    )
                }
                Text(
                    text = "Solo se cachean películas y series.",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
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
    channel: Channel,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
    cacheConfig: CacheConfig,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
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
    val retryFocus = remember { FocusRequester() }

    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(channel, playerConfig, networkConfig, cacheConfig, retryToken) {
        buildPlayer(context, channel, playerConfig, networkConfig, cacheConfig).also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (BuildConfig.DEBUG) Log.d("Streaming", "state=$playbackState")
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("Streaming", "player error", error)
                    errorMessage = friendlyPlaybackError(error)
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
        }
    }

    LaunchedEffect(player) {
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(errorMessage, channel.isVod) {
        if (errorMessage == null) return@LaunchedEffect

        if (!channel.isVod && autoRetries < playerConfig.retryCount) {
            val delayMs = (playerConfig.retryDelayMs.toLong().coerceAtLeast(500L) * (autoRetries + 1))
                .coerceAtMost(5_000L)
            delay(delayMs)
            autoRetries += 1
            errorMessage = null
            retryToken += 1
        } else {
            delay(150)
            runCatching { retryFocus.requestFocus() }
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    DisposableEffect(lifecycleOwner, player) {
        var resumeOnStart = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeOnStart = player.isPlaying
                    player.pause()
                }

                Lifecycle.Event.ON_START -> if (resumeOnStart) player.play()

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { viewContext ->
                (LayoutInflater.from(viewContext)
                    .inflate(R.layout.player_view, null) as PlayerView).apply {
                    this.player = player
                    keepScreenOn = true
                    resizeMode = aspectResizeMode(playerConfig.aspectMode)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.player = player
                view.resizeMode = aspectResizeMode(playerConfig.aspectMode)
            },
        )

        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(32.dp)
                .background(Color(0x99000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        val error = errorMessage
        if (error != null) {
            PlaybackErrorOverlay(
                message = error,
                retryFocus = retryFocus,
                onRetry = {
                    autoRetries = 0
                    errorMessage = null
                    retryToken += 1
                },
                onClose = onClose,
            )
        }
    }
}

@Composable
private fun PlaybackErrorOverlay(
    message: String,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
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
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            config.bufferMs.coerceAtLeast(1_000),
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
            setMediaItem(MediaItem.fromUri(channel.url))
        }
}

private fun aspectResizeMode(value: String): Int = when (value) {
    "fill", "stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
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
