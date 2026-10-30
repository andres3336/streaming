package com.andres3336.streaming

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.view.KeyEvent as AndroidKeyEvent
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
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
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.CacheConfig
import com.andres3336.streaming.data.config.LiveConfig
import com.andres3336.streaming.data.config.NetworkConfig
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.SubtitlesConfig
import com.andres3336.streaming.data.config.UiConfig
import com.andres3336.streaming.data.diagnostics.ErrorLogEntry
import com.andres3336.streaming.data.diagnostics.ErrorLogStore
import com.andres3336.streaming.data.media.toMediaItem
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.subtitles.MediaDescriptor
import com.andres3336.streaming.data.subtitles.OpenSubtitlesClient
import com.andres3336.streaming.data.subtitles.SubtitleCandidate
import com.andres3336.streaming.data.subtitles.SubtitleCache
import com.andres3336.streaming.data.subtitles.SubtitleOffsetStore
import com.andres3336.streaming.data.subtitles.SubtitleRepository
import com.andres3336.streaming.data.subtitles.SubtitleResult
import com.andres3336.streaming.data.subtitles.SrtCodec
import com.andres3336.streaming.data.settings.SettingsOptions
import com.andres3336.streaming.ui.PlayerAction
import com.andres3336.streaming.ui.PlayerBar
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.player.ExternalPlayer
import com.andres3336.streaming.player.HlsFallback
import com.andres3336.streaming.player.QualityOption
import com.andres3336.streaming.player.QualitySelection
import com.andres3336.streaming.player.StreamStatsFormat
import com.andres3336.streaming.player.TrackChoice
import com.andres3336.streaming.player.TrackChoices
import com.andres3336.streaming.player.VideoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun PlaybackScreen(
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
    var isPlaying by remember(channel) { mutableStateOf(false) }
    var barPositionMs by remember(channel) { mutableStateOf(0L) }
    var barDurationMs by remember(channel) { mutableStateOf(0L) }
    val transportFocus = remember { FocusRequester() }
    val seekFocus = remember { FocusRequester() }
    var chromeVisible by remember(channel) { mutableStateOf(true) }
    var chromeTick by remember(channel) { mutableIntStateOf(0) }
    var chromeFocused by remember(channel) { mutableStateOf(false) }
    var qualityOptions by remember(channel) { mutableStateOf<List<QualityOption>>(emptyList()) }
    var speed by remember(channel) { mutableStateOf(1f) }
    var aspectMode by remember(playerConfig.aspectMode) { mutableStateOf(playerConfig.aspectMode) }
    var decoderName by remember(channel) { mutableStateOf<String?>(null) }
    var bandwidthBps by remember(channel) { mutableStateOf(0L) }
    var streamStats by remember(channel) { mutableStateOf<StreamStats?>(null) }
    val retryFocus = remember { FocusRequester() }
    val nextFocus = remember { FocusRequester() }
    val chromeFocus = remember { FocusRequester() }
    val videoFocus = remember { FocusRequester() }
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

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    if (BuildConfig.DEBUG) Log.d("Streaming", "playing=$playing")
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
            val currentNumber = request.number ?: (base + 1)
            val nextNumber = ((currentNumber - 1 + delta) % queue.size + queue.size) % queue.size + 1
            onNext(PlaybackRequest(queue[next], queue, next, number = nextNumber))
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

    val overlaysOpen = showTracks || showStats || showQuality || showSpeed ||
        showSubtitleSearch || showControls || showNext || errorMessage != null

    LaunchedEffect(isPlaying, chromeVisible, chromeTick, overlaysOpen, chromeFocused) {
        if (overlaysOpen) {
            chromeVisible = true
            return@LaunchedEffect
        }
        if (!isPlaying) {
            chromeVisible = true
            return@LaunchedEffect
        }
        if (!chromeVisible || chromeFocused) return@LaunchedEffect
        delay(1_000)
        chromeVisible = false
    }

    LaunchedEffect(chromeVisible) {
        if (!chromeVisible) {
            delay(40)
            runCatching { videoFocus.requestFocus() }
        }
    }

    // Elapsed/duration for the transport row, only while the chrome is visible.
    LaunchedEffect(chromeVisible) {
        while (chromeVisible) {
            barPositionMs = player.currentPosition.coerceAtLeast(0L)
            barDurationMs = player.duration.let { if (it > 0L) it else 0L }
            delay(500)
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

    BackHandler(enabled = chromeFocused && !overlaysOpen) {
        chromeVisible = false
        scope.launch {
            delay(40)
            runCatching { videoFocus.requestFocus() }
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
                else -> videoFocus.requestFocus()
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
            // Black, not the app background: it is the colour of the letterbox bars
            // around any stream that does not fill the screen.
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || overlaysOpen) {
                    return@onPreviewKeyEvent false
                }
                val keyCode = event.nativeKeyEvent.keyCode
                val isDpadUpDown = keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_DOWN
                // Remotes without CH+/CH- zap live channels with Up/Down. On live the
                // bar has no second focusable row (its seek row is inert), so Up/Down
                // zap even while the bar has focus; Left/Right still walk its icons.
                // On VOD the same keys move between the seek and transport rows, and
                // Left/Right/Center reveal the bar and focus play/pause. The bar
                // flashes for a second after each zap because the new channel restarts
                // the chrome timer, which doubles as the channel-name feedback.
                if (isDpadUpDown &&
                    !channel.isVod &&
                    liveConfig.zapping &&
                    request.queue.size > 1
                ) {
                    zap(if (keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP) 1 else -1)
                    return@onPreviewKeyEvent true
                }
                // Series queues: CH+/CH- and Up/Down change episode.
                val isEpisodeQueue = channel.isVod &&
                    channel.seriesTitle != null &&
                    request.queue.size > 1
                val isEpisodeKey = isDpadUpDown ||
                    keyCode == AndroidKeyEvent.KEYCODE_CHANNEL_UP ||
                    keyCode == AndroidKeyEvent.KEYCODE_CHANNEL_DOWN
                if (isEpisodeQueue && isEpisodeKey && !chromeFocused) {
                    val forward = keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
                        keyCode == AndroidKeyEvent.KEYCODE_CHANNEL_UP
                    val nextIndex = request.index + if (forward) 1 else -1
                    if (nextIndex in request.queue.indices) {
                        onNext(PlaybackRequest(request.queue[nextIndex], request.queue, nextIndex))
                    }
                    return@onPreviewKeyEvent true
                }
                val isDpad = isDpadUpDown ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_RIGHT ||
                    keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                    keyCode == AndroidKeyEvent.KEYCODE_ENTER
                if (isDpad && !chromeFocused) {
                    chromeVisible = true
                    chromeTick += 1
                    scope.launch {
                        delay(60)
                        runCatching { transportFocus.requestFocus() }
                    }
                    return@onPreviewKeyEvent true
                }
                if (keyCode != AndroidKeyEvent.KEYCODE_BACK) {
                    chromeVisible = true
                    chromeTick += 1
                }

                when (keyCode) {
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
                    useController = false
                    isFocusable = false
                    isClickable = false
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(videoFocus)
                .focusable(),
        )

        val actions = if (uiConfig.playerBar == "compact") {
            listOf(PlayerAction.CONTROLS)
        } else {
            buildList {
                if ("info" in uiConfig.playerButtons) add(PlayerAction.INFO)
                if ("aspect" in uiConfig.playerButtons) add(PlayerAction.ASPECT)
                if ("quality" in uiConfig.playerButtons && capabilities.canChangeQuality) {
                    add(PlayerAction.QUALITY)
                }
                if ("speed" in uiConfig.playerButtons && capabilities.canChangeSpeed) {
                    add(PlayerAction.SPEED)
                }
                if ("tracks" in uiConfig.playerButtons) add(PlayerAction.TRACKS)
                if ("external" in uiConfig.playerButtons && capabilities.canOpenExternally) {
                    add(PlayerAction.EXTERNAL)
                }
            }
        }

        val barTitle = if (!channel.isVod) {
            val number = request.number ?: (request.index + 1).takeIf { it > 0 }
            val group = channel.group?.takeIf { it.isNotBlank() } ?: "TV"
            if (number != null) "CH$number - ${channel.name} - $group" else "${channel.name} - $group"
        } else {
            channel.name
        }

        // Item arrows: neighbouring episode on VOD series, neighbouring channel on
        // live. zap() wraps around the queue, so live always has both neighbours.
        val itemQueue = if (channel.isVod) {
            channel.seriesTitle != null && request.queue.size > 1
        } else {
            liveConfig.zapping && request.queue.size > 1
        }
        val canPrevItem = if (channel.isVod) {
            itemQueue && request.index in 1..request.queue.lastIndex
        } else {
            itemQueue
        }
        val canNextItem = if (channel.isVod) {
            itemQueue && request.index in 0 until request.queue.lastIndex
        } else {
            itemQueue
        }

        PlayerBar(
            title = barTitle,
            actions = actions,
            visible = chromeVisible,
            onAction = { action ->
                when (action) {
                    PlayerAction.INFO -> {
                        showTracks = false
                        showStats = true
                    }

                    PlayerAction.ASPECT -> {
                        aspectTouched = true
                        val next = nextAspectMode(aspectMode)
                        aspectMode = next
                        onAspectChange(next)
                    }

                    PlayerAction.QUALITY -> {
                        showStats = false
                        showTracks = false
                        showSpeed = false
                        showQuality = true
                    }

                    PlayerAction.SPEED -> {
                        showStats = false
                        showTracks = false
                        showQuality = false
                        showSpeed = true
                    }

                    PlayerAction.TRACKS -> {
                        showStats = false
                        showQuality = false
                        showSpeed = false
                        showTracks = true
                    }

                    PlayerAction.EXTERNAL -> openExternal()
                    PlayerAction.CONTROLS -> showControls = true
                }
            },
            firstActionFocus = chromeFocus,
            aspectFocus = aspectButtonFocus,
            onFocusChanged = { chromeFocused = it },
            isPlaying = isPlaying,
            positionMs = barPositionMs,
            durationMs = barDurationMs,
            canSeek = channel.isVod,
            showItemArrows = itemQueue,
            canPrev = canPrevItem,
            canNext = canNextItem,
            onPlayPause = {
                if (player.isPlaying) player.pause() else player.play()
            },
            onSeekBy = { delta ->
                val limit = if (player.duration > 0L) player.duration else Long.MAX_VALUE
                player.seekTo((player.currentPosition + delta).coerceIn(0L, limit))
                barPositionMs = player.currentPosition.coerceAtLeast(0L)
            },
            onPrevItem = {
                if (channel.isVod) {
                    val prevIndex = request.index - 1
                    if (channel.seriesTitle != null && prevIndex in request.queue.indices) {
                        onNext(PlaybackRequest(request.queue[prevIndex], request.queue, prevIndex))
                    }
                } else {
                    zap(-1)
                }
            },
            onNextItem = {
                if (channel.isVod) {
                    if (channel.seriesTitle != null) request.nextRequest()?.let(onNext)
                } else {
                    zap(1)
                }
            },
            transportFocus = transportFocus,
            seekFocus = seekFocus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 32.dp, end = 32.dp, bottom = 32.dp),
        )

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

internal data class StreamStats(
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

internal fun readStreamStats(
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

internal fun applyAudioTrack(player: ExoPlayer, choice: TrackChoice) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setOverrideForType(TrackSelectionOverride(choice.group, choice.trackIndex))
        .build()
}

internal fun applySubtitleTrack(player: ExoPlayer, choice: TrackChoice) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .setOverrideForType(TrackSelectionOverride(choice.group, choice.trackIndex))
        .build()
}

internal fun disableSubtitles(player: ExoPlayer) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

internal fun isSourceFailure(error: PlaybackException): Boolean {
    val cause = error.cause
    return cause is HttpDataSource.InvalidResponseCodeException &&
        (cause.responseCode == 401 || cause.responseCode == 403 || cause.responseCode >= 500)
}

internal fun friendlyPlaybackError(error: PlaybackException): String = when (error.errorCode) {
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

internal fun buildPlayer(
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

internal fun buildMediaItem(
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

internal fun aspectResizeMode(value: String): Int = when (value) {
    "fill", "stretch" -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    "zoom" -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
}

internal fun nextAspectMode(current: String): String {
    val modes = SettingsOptions.UI_ASPECT_MODES
    val index = modes.indexOf(current)
    return modes[(index + 1).takeIf { it in modes.indices } ?: 0]
}

internal fun applyAspect(view: PlayerView, mode: String) {
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
