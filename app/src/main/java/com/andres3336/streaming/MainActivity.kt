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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.andres3336.streaming.data.M3uParser

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

@Composable
private fun StreamingApp() {
    val context = LocalContext.current
    val channels = remember {
        context.assets.open("demo.m3u").bufferedReader().use { reader ->
            M3uParser.parse(reader.readText())
        }
    }
    var selected by remember { mutableStateOf<Channel?>(null) }

    BackHandler(enabled = selected != null) { selected = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1220)),
    ) {
        val channel = selected
        if (channel == null) {
            ChannelList(channels) { selected = it }
        } else {
            PlaybackScreen(channel)
        }
    }
}

@Composable
private fun ChannelList(channels: List<Channel>, onSelect: (Channel) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
    ) {
        Text(
            text = "Streaming",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Demo · ${channels.size} canales",
            color = Color(0xFF94A3B8),
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(32.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(channels, key = { "${it.name}|${it.url}" }) { channel ->
                Button(
                    onClick = { onSelect(channel) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = channel.name,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackScreen(channel: Channel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(channel) {
        buildPlayer(context, channel).also { exoPlayer ->
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

private fun buildPlayer(context: Context, channel: Channel): ExoPlayer {
    val headers = buildMap {
        channel.userAgent?.let { put("User-Agent", it) }
        channel.referer?.let { put("Referer", it) }
    }

    val dataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(channel.userAgent ?: "Streaming/${BuildConfig.VERSION_NAME}")
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
