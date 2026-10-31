package com.andres3336.streaming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.cache.XtreamCatalogCache
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.SourceHealth
import com.andres3336.streaming.data.live.LiveState
import com.andres3336.streaming.data.playback.PlaybackEntry
import com.andres3336.streaming.data.playback.PlaybackRequest
import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.ui.AppShell
import com.andres3336.streaming.ui.CategoryEntry
import com.andres3336.streaming.ui.CategoryPanel
import com.andres3336.streaming.ui.Dimens
import com.andres3336.streaming.ui.ModalCard
import com.andres3336.streaming.ui.NavDestination
import com.andres3336.streaming.ui.PillButton
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.player.VideoCache
import kotlinx.coroutines.delay

@Composable
internal fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Background),
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
                color = StreamColors.TextSecondary,
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
internal fun ErrorScreen(
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
            .background(StreamColors.Background)
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
                color = StreamColors.Danger,
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
                color = StreamColors.TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun ReadyScreen(
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
    onToggleFavoriteKey: (String) -> Unit,
    refreshVersion: Int,
    xtreamCatalogCache: XtreamCatalogCache,
    onCheckSources: suspend () -> SourceHealth,
    refreshing: Boolean,
    onRefreshConfig: () -> Unit,
    onSettingsChange: (DeviceSettings) -> Unit,
) {
    val context = LocalContext.current
    // Debug builds can open a section directly: `am start -e section movies`.
    val launchSection = remember {
        context.findActivity()?.intent?.getStringExtra("section")
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it == "live" || it == "channels") "tv" else it }
    }
    var section by remember { mutableStateOf(launchSection ?: "home") }
    var targetCategory by remember { mutableStateOf<String?>(null) }
    var exitConfirm by remember { mutableStateOf(false) }
    var navFocused by remember { mutableStateOf(false) }
    val navFocus = remember { FocusRequester() }
    val activity = context.findActivity()

    BackHandler(enabled = selected != null) { onClose() }
    // Back first returns focus to the top bar; only from there does it offer to exit.
    BackHandler(enabled = selected == null && !navFocused) {
        runCatching { navFocus.requestFocus() }
    }
    BackHandler(enabled = selected == null && navFocused) {
        if (exitConfirm) {
            exitConfirm = false
        } else {
            exitConfirm = true
        }
    }

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
    val showTv = (config.sources.m3uUrl != null || xtream != null) &&
        ("tv" in config.ui.sections ||
            "live" in config.ui.sections ||
            "channels" in config.ui.sections)
    val showMovies = xtream != null && "movies" in config.ui.sections
    val showSeries = xtream != null && "series" in config.ui.sections
    val showSearch = showTv || showMovies || showSeries
    // Inicio is the landing section; it always exists.
    val firstSection = "home"
    val activeSection = when {
        section == "home" -> "home"
        section == "tv" && !showTv -> firstSection
        section == "movies" && !showMovies -> firstSection
        section == "series" && !showSeries -> firstSection
        section == "search" && !showSearch -> firstSection
        else -> section
    }

    val content: @Composable () -> Unit = {
        when (activeSection) {
            "home" -> HomeScreen(
                channels = ready.channels,
                liveState = liveState,
                playbackEntries = playbackEntries,
                config = config,
                refreshVersion = refreshVersion,
                onPlay = onSelect,
                onOpenSection = { section = it },
            )

            "movies" -> {
                if (xtream != null) {
                    XtreamMoviesScreen(
                        xtream = xtream,
                        playerConfig = config.player,
                        refreshVersion = refreshVersion,
                        onUpdateConfig = onRefreshConfig,
                        favorites = liveState.favorites,
                        onToggleFavoriteKey = onToggleFavoriteKey,
                        initialCategoryId = targetCategory,
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
                        favorites = liveState.favorites,
                        onToggleFavoriteKey = onToggleFavoriteKey,
                        initialCategoryId = targetCategory,
                        onPlay = onSelect,
                        playbackEntries = playbackEntries,
                    )
                }
            }

            "settings" -> SettingsScreen(
                config = config,
                settings = settings,
                refreshing = refreshing,
                onRefreshConfig = onRefreshConfig,
                onCheckSources = onCheckSources,
                onSettingsChange = onSettingsChange,
                onClearCache = { VideoCache.clear(context) },
            )

            "tv" -> {
                val gistError = ready.gistError
                if (gistError != null && ready.channels.isEmpty() && xtream == null) {
                    SourceFailurePane(
                        message = gistError,
                        onRetry = onRefreshConfig,
                        onUpdateConfig = onRefreshConfig,
                    )
                } else {
                    TvBrowseScreen(
                        config = config,
                        gistChannels = ready.channels,
                        refreshVersion = refreshVersion,
                        onUpdateConfig = onRefreshConfig,
                        liveState = liveState,
                        onToggleFavorite = onToggleFavorite,
                        onSelect = onSelect,
                        xtreamCatalogCache = xtreamCatalogCache,
                        playerConfig = config.player,
                        networkConfig = config.network,
                        initialCategoryId = targetCategory,
                    )
                }
            }

            "search" -> UniversalSearchScreen(
                channels = ready.channels,
                config = config,
                refreshVersion = refreshVersion,
                onUpdateConfig = onRefreshConfig,
                favorites = liveState.favorites,
                onToggleFavoriteKey = onToggleFavoriteKey,
                onPlay = onSelect,
                onOpenCategory = { target, categoryId ->
                    targetCategory = categoryId
                    section = target
                },
            )

            else -> CenteredMessage("Sección no disponible")
        }
    }

    val destinations = buildList {
        add(NavDestination("home", "Inicio"))
        if (showTv) add(NavDestination("tv", "TV"))
        if (showMovies) add(NavDestination("movies", "Películas"))
        if (showSeries) add(NavDestination("series", "Series"))
        if (showSearch) add(NavDestination("search", "Buscar"))
        add(NavDestination("settings", "Ajustes"))
    }

    AppShell(
        section = activeSection,
        destinations = destinations,
        onHome = {
            targetCategory = null
            section = "home"
        },
        onSelect = { id ->
            targetCategory = null
            section = id
        },
        navFocusRequester = navFocus,
        onNavFocused = { navFocused = it },
        content = { content() },
    )

    if (exitConfirm) {
        ExitConfirmOverlay(
            onCancel = { exitConfirm = false },
            onExit = {
                exitConfirm = false
                activity?.finish()
            },
        )
    }
}

@Composable
internal fun ExitConfirmOverlay(
    onCancel: () -> Unit,
    onExit: () -> Unit,
) {
    val exitFocus = remember { FocusRequester() }
    val buttonColors = ButtonDefaults.colors(
        containerColor = StreamColors.SurfaceRaised,
        contentColor = StreamColors.TextPrimary,
        focusedContainerColor = StreamColors.Accent,
        focusedContentColor = StreamColors.OnAccent,
        pressedContainerColor = StreamColors.AccentDeep,
        pressedContentColor = StreamColors.OnAccent,
    )

    ModalCard(onDismiss = onCancel, initialFocus = exitFocus) {
        Text(
            text = "¿Salir de Streaming?",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Se cerrará la aplicación.",
            color = StreamColors.TextSecondary,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCancel,
                colors = buttonColors,
            ) {
                Text("Cancelar", fontSize = 18.sp)
            }
            Button(
                onClick = onExit,
                modifier = Modifier.focusRequester(exitFocus),
                colors = buttonColors,
            ) {
                Text("Salir", fontSize = 18.sp)
            }
        }
    }
}

@Composable
internal fun ForceUpdateScreen(onUpdateConfig: () -> Unit) {
    val updateFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { updateFocus.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
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
                color = StreamColors.TextSecondary,
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
internal fun BrowseShell(
    rail: @Composable () -> Unit,
    content: @Composable () -> Unit,
    preview: (@Composable () -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = if (preview != null) {
                Modifier.width(168.dp).fillMaxHeight()
            } else {
                Modifier.weight(Dimens.CategoryWidthFraction).fillMaxHeight()
            },
        ) {
            rail()
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = if (preview != null) {
                Modifier.weight(1f).fillMaxHeight()
            } else {
                Modifier.weight(Dimens.ContentWidthFraction).fillMaxHeight()
            },
        ) {
            content()
        }
        if (preview != null) {
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.width(272.dp).fillMaxHeight()) {
                preview()
            }
        }
    }
}

@Composable
internal fun CinematicBrowse(
    rail: @Composable () -> Unit,
    details: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(180.dp).fillMaxHeight()) {
            rail()
        }
        Spacer(Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
        ) {
            details()
            content()
        }
    }
}

@Composable
internal fun <T> BrowseRail(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    title: String = "Categorías",
    footer: (@Composable () -> Unit)? = null,
) {
    CategoryPanel(
        entries = items.map { CategoryEntry(id = it.toString(), label = label(it)) },
        selectedId = selected?.toString(),
        onSelect = { id ->
            items.firstOrNull { it.toString() == id }?.let(onSelect)
        },
        modifier = Modifier.fillMaxSize(),
        title = title,
        footer = footer,
    )
}

@Composable
internal fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = StreamColors.TextSecondary,
            fontSize = 18.sp,
        )
    }
}

@Composable
internal fun SourceFailurePane(
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
                color = StreamColors.Danger,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    label = "Actualizar configuración",
                    primary = true,
                    modifier = Modifier.focusRequester(updateFocus),
                    onClick = onUpdateConfig,
                )
                PillButton(label = "Reintentar", onClick = onRetry)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Si la actualización falla, se conserva la configuración anterior.",
                color = StreamColors.TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
