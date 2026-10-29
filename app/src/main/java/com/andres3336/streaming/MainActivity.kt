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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
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
        return AppState.Ready(channels, PlayerConfig(), "Demo", null)
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

    BackHandler(enabled = selected != null) { selected = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220)),
    ) {
        val channel = selected
        if (channel == null) {
            ChannelList(ready.channels, ready.sourceLabel) { selected = it }
        } else {
            PlaybackScreen(channel, ready.player)
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<Channel>,
    sourceLabel: String,
    onSelect: (Channel) -> Unit,
) {
    val groups = remember(channels) { channels.groupBy { it.group ?: "Sin grupo" } }
    val firstChannel = remember(channels) { channels.firstOrNull() }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(firstChannel) {
        if (firstChannel != null) {
            delay(150)
            runCatching { firstFocus.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
    ) {
        Text(
            text = "Streaming",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "$sourceLabel · ${channels.size} canales",
            color = Color(0xFF94A3B8),
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (group, groupChannels) ->
                item(key = "group-$group") {
                    Text(
                        text = group,
                        color = Color(0xFF93C5FD),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
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
                        Text(
                            text = channel.name,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
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
