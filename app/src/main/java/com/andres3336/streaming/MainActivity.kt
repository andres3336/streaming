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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import com.andres3336.streaming.data.config.LiveMode
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.XtreamConfig
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamMovie
import com.andres3336.streaming.data.xtream.XtreamResult
import com.andres3336.streaming.data.xtream.XtreamSeries
import com.andres3336.streaming.data.xtream.XtreamSeriesInfo
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        val player: PlayerConfig,
        val sourceLabel: String,
        val etag: String?,
        val xtream: XtreamConfig?,
        val sections: List<String>,
    ) : AppState
}

@Composable
private fun StreamingApp() {
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val configRepository = remember(client) { ConfigRepository(BuildConfig.CONFIG_URL, client) }
    val m3uRepository = remember(client) { M3uRepository(client) }

    var state by remember { mutableStateOf<AppState>(AppState.Loading) }
    var retry by remember { mutableIntStateOf(0) }
    var etag by remember { mutableStateOf<String?>(null) }
    var lastReady by remember { mutableStateOf<AppState.Ready?>(null) }

    LaunchedEffect(retry) {
        state = AppState.Loading
        val loaded = loadApp(context, configRepository, m3uRepository, etag, lastReady)
        state = loaded
        if (loaded is AppState.Ready) {
            lastReady = loaded
            etag = loaded.etag
        }
    }

    when (val current = state) {
        is AppState.Loading -> LoadingScreen()
        is AppState.Error -> ErrorScreen(current.message) { retry++ }
        is AppState.Ready -> ReadyScreen(current)
    }
}

private suspend fun loadApp(
    context: Context,
    configRepository: ConfigRepository,
    m3uRepository: M3uRepository,
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
            player = PlayerConfig(),
            sourceLabel = "Demo",
            etag = null,
            xtream = null,
            sections = listOf("live"),
        )
    }

    return when (val fetched = configRepository.fetch(etag)) {
        is ConfigFetch.MissingUrl -> AppState.Error("Falta la URL de configuración")
        is ConfigFetch.NotModified -> lastReady
            ?: AppState.Error("La configuración no cambió y no hay copia local")

        is ConfigFetch.Failure -> AppState.Error(fetched.message)
        is ConfigFetch.Success -> {
            val config = fetched.config
            val m3uUrl = config.sources.m3uUrl
            if (m3uUrl == null) {
                AppState.Error("La configuración no incluye una lista M3U")
            } else {
                when (val playlist = m3uRepository.load(
                    url = m3uUrl,
                    userAgent = config.player.userAgent,
                    headers = config.player.headers,
                )) {
                    is M3uFetch.Success -> AppState.Ready(
                        channels = playlist.channels,
                        player = config.player,
                        sourceLabel = if (config.sources.liveMode == LiveMode.GIST) "Gist" else "Remoto",
                        etag = fetched.etag,
                        xtream = config.sources.xtream,
                        sections = config.ui.sections,
                    )

                    is M3uFetch.Failure -> AppState.Error(playlist.message)
                }
            }
        }
    }
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
private fun ReadyScreen(ready: AppState.Ready) {
    var selected by remember(ready.channels) { mutableStateOf<Channel?>(null) }
    var section by remember { mutableStateOf("live") }

    BackHandler(enabled = selected != null) { selected = null }

    val channel = selected
    if (channel != null) {
        PlaybackScreen(channel, ready.player)
        return
    }

    val showMovies = ready.xtream != null && "movies" in ready.sections
    val showSeries = ready.xtream != null && "series" in ready.sections
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
            onSelect = { section = it },
        )
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (activeSection) {
                "movies" -> {
                    val xtream = ready.xtream
                    if (xtream != null) {
                        XtreamMoviesScreen(
                            xtream = xtream,
                            playerConfig = ready.player,
                            onPlay = { selected = it },
                        )
                    }
                }

                "series" -> {
                    val xtream = ready.xtream
                    if (xtream != null) {
                        XtreamSeriesScreen(
                            xtream = xtream,
                            playerConfig = ready.player,
                            onPlay = { selected = it },
                        )
                    }
                }

                else -> ChannelList(ready.channels) { selected = it }
            }
        }
    }
}

@Composable
private fun SectionBar(
    section: String,
    showMovies: Boolean,
    showSeries: Boolean,
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
private fun PlaybackScreen(channel: Channel, playerConfig: PlayerConfig) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(channel, playerConfig) {
        buildPlayer(context, channel, playerConfig).also { exoPlayer ->
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (BuildConfig.DEBUG) Log.d("Streaming", "state=$playbackState")
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("Streaming", "player error", error)
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
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view -> view.player = player },
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
    }
}

private fun buildPlayer(
    context: Context,
    channel: Channel,
    config: PlayerConfig,
): ExoPlayer {
    val headers = buildMap {
        putAll(config.headers)
        channel.userAgent?.let { put("User-Agent", it) }
        channel.referer?.let { put("Referer", it) }
    }
    val userAgent = channel.userAgent
        ?: config.userAgent
        ?: "Streaming/${BuildConfig.VERSION_NAME}"

    val dataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setDefaultRequestProperties(headers)
        .setAllowCrossProtocolRedirects(true)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(channel.url))
        }
}
