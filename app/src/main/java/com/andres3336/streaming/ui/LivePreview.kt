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
import com.andres3336.streaming.data.epg.EpgGuide
import com.andres3336.streaming.data.epg.EpgNowNext
import kotlinx.coroutines.delay

/**
 * Shared focus sink for the live preview. The channel list reports the focused
 * row here; each live screen provides the callback and renders the preview pane.
 */
val LocalLiveFocus = androidx.compose.runtime.staticCompositionLocalOf<((Channel) -> Unit)?> { null }

/**
 * Right-hand preview for the live list: muted-free (audio on), flat pane with the
 * programme guide below it. The stream starts only after the focus settles, so
 * scrolling the list does not open a request per row.
 */
@Composable
fun LivePreviewPane(
    channel: Channel?,
    nowNext: EpgNowNext?,
    nowMs: Long,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
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

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = channel?.name ?: "Sin canal",
                color = StreamColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val current = nowNext?.current
        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = current.title,
                color = StreamColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${EpgGuide.formatClock(current.startMs)} – ${EpgGuide.formatClock(current.stopMs)}",
                    color = StreamColors.TextMuted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = EpgGuide.formatTimeLeft(current.stopMs - nowMs),
                    color = StreamColors.AccentBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressLine(
                fraction = (
                    (nowMs - current.startMs).toFloat() /
                        (current.stopMs - current.startMs).coerceAtLeast(1L)
                    ).coerceIn(0f, 1f),
            )
            nowNext?.next?.let { next ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Después",
                    color = StreamColors.TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${EpgGuide.formatClock(next.startMs)}  ${next.title}",
                    color = StreamColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (channel != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sin guía de programación para este canal.",
                color = StreamColors.TextMuted,
                fontSize = 12.sp,
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
        )
        if (focused) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(StreamColors.SurfaceRaised, RoundedCornerShape(5.dp))
                    .border(1.dp, StreamColors.Border, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "OK \u00b7 Reproducir",
                    color = StreamColors.TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
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

/** Solid progress line: no alpha, no animation. */
@Composable
fun ProgressLine(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Int = 3,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(StreamColors.Border, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxSize()
                .background(StreamColors.Accent, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun ChannelPreview(
    channel: Channel,
    playerConfig: PlayerConfig,
    networkConfig: NetworkConfig,
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
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        delay(600)
        runCatching {
            player.prepare()
            player.playWhenReady = true
            started = true
        }.onFailure { failed = true }
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
