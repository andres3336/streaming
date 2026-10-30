package com.andres3336.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.andres3336.streaming.data.subtitles.SubtitleCandidate
import com.andres3336.streaming.data.settings.SettingsOptions
import com.andres3336.streaming.ui.PillButton
import com.andres3336.streaming.ui.StreamColors
import com.andres3336.streaming.player.QualityOption
import com.andres3336.streaming.player.TrackChoice
import java.util.Locale

@Composable
internal fun StreamStatsOverlay(
    stats: StreamStats?,
    firstFocus: FocusRequester,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(820.dp)
                .fillMaxHeight(0.96f)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
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
                        color = StreamColors.TextMuted,
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
internal fun StatRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = StreamColors.TextSecondary,
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
internal fun TrackPickerOverlay(
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
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(680.dp)
                .fillMaxHeight(0.9f)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
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
                        color = StreamColors.AccentBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (audio.isEmpty()) {
                    item {
                        Text(
                            text = "Sin pistas de audio.",
                            color = StreamColors.TextMuted,
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
                        color = StreamColors.AccentBright,
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
                            color = StreamColors.TextMuted,
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
                    color = StreamColors.TextSecondary,
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
                        color = StreamColors.Danger,
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
internal fun TrackOptionButton(
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
                color = if (selected) StreamColors.Accent else Color.White,
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(
                    text = "✓",
                    color = StreamColors.Accent,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
internal fun QualityOverlay(
    options: List<QualityOption>,
    current: String,
    firstFocus: FocusRequester,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(560.dp)
                .fillMaxHeight(0.85f)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
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
                            color = StreamColors.TextMuted,
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    if (options.size == 1) {
                        item {
                            Text(
                                text = "Este canal tiene una sola calidad.",
                                color = StreamColors.TextMuted,
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
internal fun SpeedOverlay(
    current: Float,
    firstFocus: FocusRequester,
    onSelect: (Float) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .fillMaxHeight(0.9f)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
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

internal fun speedLabel(speed: Float): String {
    val text = String.format(Locale.US, "%.2f", speed).trimEnd('0').trimEnd('.')
    return "${text}x"
}

@Composable
internal fun PlayerControlsOverlay(
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
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(380.dp)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, StreamColors.Border, RoundedCornerShape(12.dp))
                .padding(18.dp),
        ) {
            Text(
                text = "Controles",
                color = StreamColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            actions.forEachIndexed { index, (label, action) ->
                PillButton(
                    label = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (index == 0) Modifier.focusRequester(firstFocus) else Modifier,
                        ),
                    onClick = action,
                )
                Spacer(Modifier.height(8.dp))
            }
            PillButton(
                label = "Cerrar",
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose,
            )
        }
    }
}

@Composable
internal fun SubtitleSearchOverlay(
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
            .background(StreamColors.Scrim)
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .fillMaxHeight(0.9f)
                .background(StreamColors.Surface, RoundedCornerShape(12.dp))
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

internal fun offsetSecondsLabel(offsetMs: Long): String {
    if (offsetMs == 0L) return "0 s"
    return String.format(Locale.US, "%+.1f s", offsetMs / 1_000.0)
}

@Composable
internal fun NextEpisodeOverlay(
    title: String,
    seconds: Int,
    focusRequester: FocusRequester,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Scrim)
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
                color = StreamColors.TextSecondary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Se reproduce en $seconds s",
                color = StreamColors.Accent,
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
internal fun PlaybackErrorOverlay(
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
            .background(StreamColors.Scrim)
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
                color = StreamColors.Danger,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            )
            if (sourceFailure) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Puede ser un problema de la fuente o de la cuenta.",
                    color = StreamColors.TextSecondary,
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
