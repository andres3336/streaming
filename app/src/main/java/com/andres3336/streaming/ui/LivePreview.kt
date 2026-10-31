package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.andres3336.streaming.R
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.NetworkConfig
import com.andres3336.streaming.data.config.PlayerConfig
import kotlinx.coroutines.delay

/**
 * Shared focus sink for the live preview. The channel list reports the focused
 * row here; each live screen provides the callback and renders the preview pane.
 */
val LocalLiveFocus = androidx.compose.runtime.staticCompositionLocalOf<((Channel) -> Unit)?> { null }

/** State of an autoplaying preview, so the screen can show a loading label. */
enum class PreviewState { Loading, Playing, Failed }

/**
 * Right-hand preview for the live list: a flat pane with the muted-free stream and
 * its identity. There is no programme guide; the EPG was removed as unreliable.
 */
@Composable
fun LivePreviewPane(
    channel: Channel?,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
    source: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(Dimens.CardCorner))
                .background(Color.Black),
        ) {
            if (channel == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Selecciona un canal",
                        color = StreamColors.TextMuted,
                        fontSize = 13.sp,
                    )
                }
            } else {
                ChannelPreview(channel = channel, playerConfig = playerConfig, networkConfig = networkConfig)
                LiveTag(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 25.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(StreamColors.SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (channel != null) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        placeholder = painterResource(R.drawable.ic_channel_placeholder),
                        error = painterResource(R.drawable.ic_channel_placeholder),
                        fallback = painterResource(R.drawable.ic_channel_placeholder),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel?.name ?: "Sin canal",
                color = StreamColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val meta = listOfNotNull(
            source?.takeIf(String::isNotBlank),
        ).joinToString(" \u00b7 ")
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = meta,
                color = StreamColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Autoplaying 16:9 preview used by the movie/series detail view (audio on). */
@Composable
fun VideoPreview(
    channel: Channel,
    playerConfig: PlayerConfig,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.CardCorner)
    var focused by remember { mutableStateOf(false) }
    var previewState by remember(channel.url) { mutableStateOf(PreviewState.Loading) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(Color.Black)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) StreamColors.AccentBright else StreamColors.Border,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onPlay)
            .semantics { contentDescription = "Reproducir" },
    ) {
        ChannelPreview(
            channel = channel,
            playerConfig = playerConfig,
            networkConfig = NetworkConfig(),
            onState = { previewState = it },
        )
        if (previewState == PreviewState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Cargando vista previa\u2026",
                    color = StreamColors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }
        if (previewState == PreviewState.Failed) {
            Text(
                text = "Vista previa no disponible",
                color = StreamColors.TextMuted,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun LiveTag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(StreamColors.Background, RoundedCornerShape(5.dp))
            .border(1.dp, StreamColors.Border, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(StreamColors.Danger, RoundedCornerShape(50)),
        )
        Text(
            text = "EN VIVO",
            color = StreamColors.TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ChannelPreview(
    channel: Channel,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
    onState: (PreviewState) -> Unit = {},
) {
    val context = LocalContext.current
    var failed by remember(channel.url) { mutableStateOf(false) }
    var started by remember(channel.url) { mutableStateOf(false) }
    val player = remember(channel.url) {
        previewPlayer(context, channel, playerConfig, networkConfig)
    }

    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                failed = true
                onState(PreviewState.Failed)
            }

            override fun onRenderedFirstFrame() {
                onState(PreviewState.Playing)
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        onState(PreviewState.Loading)
        delay(600)
        runCatching {
            player.prepare()
            player.playWhenReady = true
            started = true
        }.onFailure {
            failed = true
            onState(PreviewState.Failed)
        }
    }

    if (failed) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null,
                placeholder = painterResource(R.drawable.ic_channel_placeholder),
                error = painterResource(R.drawable.ic_channel_placeholder),
                fallback = painterResource(R.drawable.ic_channel_placeholder),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(24.dp),
            )
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Same layout as the full player so debug builds keep the TextureView
            // that `adb screencap` can capture.
            (android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.player_view, null) as PlayerView).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                isFocusable = false
                isClickable = false
                this.player = player
                if (!started) setKeepContentOnPlayerReset(true)
            }
        },
        update = { view -> view.player = player },
    )
}

private fun previewPlayer(
    context: android.content.Context,
    channel: Channel,
    playerConfig: PlayerConfig,
    network: NetworkConfig,
): ExoPlayer {
    val headers = buildMap {
        putAll(playerConfig.headers)
        channel.userAgent?.let { put("User-Agent", it) }
        channel.referer?.let { put("Referer", it) }
    }
    val dataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(channel.userAgent ?: playerConfig.userAgent ?: "Streaming")
        .setDefaultRequestProperties(headers)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(network.connectTimeoutMs)
        .setReadTimeoutMs(network.readTimeoutMs)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply { setMediaItem(MediaItem.fromUri(channel.url)) }
}
