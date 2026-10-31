package com.andres3336.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.SourceHealth
import com.andres3336.streaming.data.diagnostics.ErrorLogEntry
import com.andres3336.streaming.data.diagnostics.ErrorLogStore
import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.data.settings.HomeLauncher
import com.andres3336.streaming.data.settings.SettingsOptions
import com.andres3336.streaming.ui.AppMark
import com.andres3336.streaming.ui.ModalCard
import com.andres3336.streaming.ui.QuickChip
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.player.VideoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsScreen(
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StreamColors.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, StreamColors.Border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(StreamColors.Accent, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AppMark(size = 22.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Streaming",
                        color = StreamColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} · ${liveSourceLabel(config)}",
                        color = StreamColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }

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
                    color = StreamColors.TextMuted,
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
                label = "Barra del reproductor",
                current = config.ui.playerBar,
                options = listOf(
                    "Completa" to "full",
                    "Compacta" to "compact",
                ),
            ) { value -> onSettingsChange(settings.copy(playerBar = value)) }
        }
        item {
            var focusedIndex by remember { mutableStateOf<Int?>(null) }
            SettingsRow(
                label = "Botones del reproductor",
                highlighted = focusedIndex != null,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "info" to "Info",
                        "aspect" to "Aspecto",
                        "tracks" to "Audio",
                        "quality" to "Calidad",
                        "speed" to "Velocidad",
                        "external" to "Externo",
                    ).forEachIndexed { index, (id, label) ->
                        val enabled = id in config.ui.playerButtons
                        QuickChip(
                            label = if (enabled) "✓ $label" else label,
                            selected = enabled,
                            onFocusChanged = { focused ->
                                focusedIndex = if (focused) {
                                    index
                                } else if (focusedIndex == index) {
                                    null
                                } else {
                                    focusedIndex
                                }
                            },
                            onClick = {
                                val updated = if (enabled) {
                                    config.ui.playerButtons - id
                                } else {
                                    config.ui.playerButtons + id
                                }
                                onSettingsChange(settings.copy(playerButtons = updated))
                            },
                        )
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
                        StreamColors.AccentBright
                    } else {
                        StreamColors.Favorite
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
            ActionRow(
                label = "Borrar caché",
                buttonLabel = if (cacheCleared) "Borrada" else "Borrar",
                status = "Uso actual: ${cacheSizeMb ?: "…"} MB · solo películas y series",
            ) {
                onClearCache()
                cacheCleared = true
                cacheSizeMb = 0L
            }
        }

        item { SettingsHeader("Red") }
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
                ActionRow(
                    label = "Comprobar fuentes",
                    buttonLabel = if (checkingSources) "Comprobando…" else "Comprobar",
                    status = "Comprueba la lista M3U y el acceso a Xtream.",
                    enabled = !checkingSources,
                ) {
                    checkingSources = true
                    scope.launch {
                        sourceHealth = onCheckSources()
                        checkingSources = false
                    }
                }

                sourceHealth?.let { health ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = health.summary(),
                        color = if (health.isHealthy) StreamColors.AccentBright else StreamColors.Danger,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    health.liveMessage?.let { message ->
                        Text(
                            text = "En vivo: $message",
                            color = StreamColors.Danger,
                            fontSize = 13.sp,
                        )
                    }
                    health.xtreamMessage?.let { message ->
                        Text(
                            text = "Xtream: $message",
                            color = StreamColors.Danger,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        item { SettingsHeader("Diagnóstico") }
        item {
            Column {
                ActionRow(
                    label = "Borrar registro",
                    buttonLabel = "Borrar",
                    status = when {
                        errorLog.isEmpty() -> "Sin errores registrados"
                        errorLog.size == 1 -> "1 error registrado"
                        else -> "${errorLog.size} errores registrados"
                    },
                ) {
                    errorLogStore.clear()
                    errorLog = emptyList()
                }
                errorLog.takeLast(5).reversed().forEach { entry ->
                    Text(
                        text = "${entry.channel} · ${entry.code} · ${entry.message}",
                        color = StreamColors.Danger,
                        fontSize = 13.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        item { SettingsHeader("Configuración") }
        item {
            ActionRow(
                label = "Actualizar configuración",
                buttonLabel = if (refreshing) "Actualizando…" else "Actualizar",
                status = "Se revisa al abrir y al volver a la app.",
                enabled = !refreshing,
                onClick = onRefreshConfig,
            )
        }

        item {
            ActionRow(
                label = "Restablecer ajustes",
                buttonLabel = "Restablecer",
                status = "Vuelve a los valores del gist.",
            ) {
                onSettingsChange(DeviceSettings())
                cacheCleared = false
            }
        }
    }
}

@Composable
internal fun SettingsHeader(text: String) {
    Row(
        modifier = Modifier.padding(top = 6.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(StreamColors.Accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            color = StreamColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
internal fun ChoiceRow(
    label: String,
    current: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    if (options.size <= 2) {
        var focusedIndex by remember { mutableStateOf<Int?>(null) }
        SettingsRow(label = label, highlighted = focusedIndex != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { index, (text, value) ->
                    val selected = value == current
                    QuickChip(
                        label = if (selected) "\u2713 $text" else text,
                        selected = selected,
                        onFocusChanged = { focused ->
                            focusedIndex = if (focused) {
                                index
                            } else if (focusedIndex == index) {
                                null
                            } else {
                                focusedIndex
                            }
                        },
                        onClick = { onSelect(value) },
                    )
                }
            }
        }
        return
    }

    var open by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.second == current }?.first ?: current
    SettingsRow(label = label, onClick = { open = true }) {
        Text(
            text = "$currentLabel  \u203a",
            color = StreamColors.AccentBright,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
    if (open) {
        ChoiceDropdown(
            label = label,
            options = options,
            current = current,
            onSelect = { value ->
                open = false
                onSelect(value)
            },
            onDismiss = { open = false },
        )
    }
}

/** Single-row action card: label and status on the left, button on the right. */
@Composable
internal fun ActionRow(
    label: String,
    buttonLabel: String,
    status: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    SettingsRow(label = label, highlighted = focused) {
        if (status != null) {
            Text(
                text = status,
                color = StreamColors.TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.onFocusChanged { focused = it.isFocused },
        ) {
            Text(buttonLabel, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    trailing: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    if (onClick == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(
                    if (highlighted) StreamColors.SurfaceRaised else StreamColors.Surface,
                    shape,
                )
                .border(
                    width = 1.dp,
                    color = if (highlighted) StreamColors.AccentBright else StreamColors.Border,
                    shape = shape,
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(label)
            trailing()
        }
    } else {
        var focused by remember { mutableStateOf(false) }
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(
                    width = 1.dp,
                    color = if (focused) StreamColors.AccentBright else StreamColors.Border,
                    shape = shape,
                )
                .onFocusChanged { focused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = StreamColors.Surface,
                contentColor = StreamColors.TextPrimary,
                focusedContainerColor = StreamColors.SurfaceRaised,
                focusedContentColor = StreamColors.TextPrimary,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsLabel(label)
                trailing()
            }
        }
    }
}

@Composable
private fun RowScope.SettingsLabel(label: String) {
    Text(
        text = label,
        color = StreamColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

/** Modal list of every value for a setting; the current one is pre-focused. */
@Composable
private fun ChoiceDropdown(
    label: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedFocus = remember { FocusRequester() }

    ModalCard(onDismiss = onDismiss, initialFocus = selectedFocus) {
        Text(
            text = label,
            color = StreamColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options, key = { it.second }) { (text, value) ->
                val selected = value == current
                val shape = RoundedCornerShape(8.dp)
                var focused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { onSelect(value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                        .border(
                            width = 1.dp,
                            color = if (focused) StreamColors.AccentBright else Color.Transparent,
                            shape = shape,
                        )
                        .onFocusChanged { focused = it.isFocused },
                    shape = ClickableSurfaceDefaults.shape(shape),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = if (selected) StreamColors.Accent else StreamColors.SurfaceRaised,
                        contentColor = if (selected) StreamColors.OnAccent else StreamColors.TextPrimary,
                        focusedContainerColor = if (selected) StreamColors.Accent else StreamColors.SurfaceRaised,
                        focusedContentColor = if (selected) StreamColors.OnAccent else StreamColors.TextPrimary,
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                ) {
                    Text(
                        text = text,
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

internal fun scaleLabel(scale: Float): String = "${(scale * 100).toInt()} %"
