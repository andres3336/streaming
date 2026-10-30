package com.andres3336.streaming.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Actions available in the player's bottom bar. */
enum class PlayerAction(val label: String) {
    INFO("Información"),
    ASPECT("Aspecto"),
    QUALITY("Calidad"),
    TRACKS("Audio y subtítulos"),
    SPEED("Velocidad"),
    EXTERNAL("Abrir en otro reproductor"),
    CONTROLS("Controles"),
}

/** Seek ladder: 5 s, 10 s, 30 s, then a flat 60 s per press. */
private val SEEK_STEPS_MS = longArrayOf(5_000L, 10_000L, 30_000L, 60_000L)

/** Idle time after which the next seek starts the ladder again. */
private const val SEEK_LADDER_TIMEOUT_MS = 1_500L

/**
 * One flat player bar, anchored at the bottom of the screen: title, then the
 * seek bar, then the actions split around a centred transport group
 * (`prev`, `-5..60 s`, play/pause, `+5..60 s`, `next`). The transport group is
 * symmetric in every case and the two side clusters take equal weight, so
 * play/pause stays on the exact centre of the screen.
 *
 * The caller controls visibility (auto-hide) and keeps the bar on screen while
 * one of its buttons has focus, so D-pad navigation never loses its target.
 */
@Composable
fun PlayerBar(
    title: String,
    actions: List<PlayerAction>,
    visible: Boolean,
    onAction: (PlayerAction) -> Unit,
    firstActionFocus: FocusRequester,
    aspectFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    canSeek: Boolean,
    showItemArrows: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onPlayPause: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevItem: () -> Unit,
    onNextItem: () -> Unit,
    transportFocus: FocusRequester,
    seekFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    // One ladder for the seek bar and the chevrons, so repeated presses grow the
    // jump instead of forcing a dozen taps. It resets when the bar hides and after
    // a moment without seek presses.
    var seekStepIndex by remember { mutableStateOf(0) }
    var lastSeekAtMs by remember { mutableStateOf(0L) }
    LaunchedEffect(visible) {
        if (!visible) {
            seekStepIndex = 0
            lastSeekAtMs = 0L
        }
    }
    if (!visible) return
    val seekStep: (Int) -> Unit = { direction ->
        val now = System.nanoTime() / 1_000_000L
        if (now - lastSeekAtMs >= SEEK_LADDER_TIMEOUT_MS) seekStepIndex = 0
        val step = SEEK_STEPS_MS[seekStepIndex.coerceIn(SEEK_STEPS_MS.indices)]
        lastSeekAtMs = now
        seekStepIndex = (seekStepIndex + 1).coerceAtMost(SEEK_STEPS_MS.lastIndex)
        onSeekBy(step * direction)
    }
    // First half of the actions sits left of the transport group, the rest right.
    val split = (actions.size + 1) / 2
    val leftActions = actions.take(split)
    val rightActions = actions.drop(split)
    val firstAction = actions.firstOrNull()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StreamColors.SurfaceRaised, shape)
            .border(1.dp, StreamColors.Border, shape),
    ) {
        // Title row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = StreamColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(StreamColors.Border))

        // Seek row: the bar itself is selectable and Left/Right step the ladder.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = clock(positionMs),
                color = StreamColors.TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.width(46.dp),
            )
            SeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                enabled = canSeek,
                onSeek = seekStep,
                focusRequester = seekFocus,
                onFocusChanged = onFocusChanged,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (durationMs > 0L) clock(durationMs) else "--:--",
                color = StreamColors.TextMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(46.dp),
            )
        }

        // Transport row: actions on the edges, play/pause dead centre.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leftActions.forEach { action ->
                    PlayerActionButton(
                        action = action,
                        focusRequester = when {
                            action == PlayerAction.ASPECT -> aspectFocus
                            action == firstAction -> firstActionFocus
                            else -> null
                        },
                        onClick = { onAction(action) },
                        onFocusChanged = onFocusChanged,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Symmetric around play/pause: item jumps outside, seeks inside.
                // A missing neighbour keeps its slot as a dimmed, non-focusable icon
                // so the row never goes lopsided and play/pause never slides.
                if (showItemArrows) {
                    if (canPrev) {
                        TransportButton(
                            label = "Anterior",
                            onClick = onPrevItem,
                            onFocusChanged = onFocusChanged,
                        ) { focused ->
                            SkipItemGlyph(
                                forward = false,
                                color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
                            )
                        }
                    } else {
                        TransportSlot {
                            SkipItemGlyph(forward = false, color = StreamColors.TextMuted)
                        }
                    }
                }
                if (canSeek) {
                    TransportButton(
                        label = "Retroceder",
                        onClick = { seekStep(-1) },
                        onFocusChanged = onFocusChanged,
                    ) { focused ->
                        SeekChevronGlyph(
                            forward = false,
                            color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
                        )
                    }
                }
                TransportButton(
                    focusRequester = transportFocus,
                    label = if (isPlaying) "Pausa" else "Reproducir",
                    onClick = onPlayPause,
                    onFocusChanged = onFocusChanged,
                ) { focused ->
                    PlayPauseGlyph(
                        playing = isPlaying,
                        color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
                    )
                }
                if (canSeek) {
                    TransportButton(
                        label = "Avanzar",
                        onClick = { seekStep(1) },
                        onFocusChanged = onFocusChanged,
                    ) { focused ->
                        SeekChevronGlyph(
                            forward = true,
                            color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
                        )
                    }
                }
                if (showItemArrows) {
                    if (canNext) {
                        TransportButton(
                            label = "Siguiente",
                            onClick = onNextItem,
                            onFocusChanged = onFocusChanged,
                        ) { focused ->
                            SkipItemGlyph(
                                forward = true,
                                color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
                            )
                        }
                    } else {
                        TransportSlot {
                            SkipItemGlyph(forward = true, color = StreamColors.TextMuted)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rightActions.forEach { action ->
                    PlayerActionButton(
                        action = action,
                        focusRequester = when {
                            action == PlayerAction.ASPECT -> aspectFocus
                            action == firstAction -> firstActionFocus
                            else -> null
                        },
                        onClick = { onAction(action) },
                        onFocusChanged = onFocusChanged,
                    )
                }
            }
        }
    }
}

/**
 * Selectable timeline. Focus it and Left/Right step through the seek ladder
 * (5, 10, 30, then 60 s per press) instead of walking the film in 10 s taps.
 */
@Composable
private fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    enabled: Boolean,
    onSeek: (Int) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val fraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = 6.dp)
            .then(
                if (enabled) {
                    Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            focused = it.isFocused
                            onFocusChanged(it.isFocused)
                        }
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    onSeek(-1)
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    onSeek(1)
                                    true
                                }

                                else -> false
                            }
                        }
                        .background(
                            color = if (focused) StreamColors.Surface else Color.Transparent,
                            shape = shape,
                        )
                        .border(
                            width = 1.dp,
                            color = if (focused) StreamColors.AccentBright else StreamColors.Border,
                            shape = shape,
                        )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            val y = size.height / 2f
            val stroke = 3.dp.toPx()
            drawLine(
                color = StreamColors.BorderStrong,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            val head = (size.width * fraction).coerceIn(0f, size.width)
            drawLine(
                color = StreamColors.Accent,
                start = Offset(0f, y),
                end = Offset(head, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            if (enabled) {
                drawCircle(
                    color = if (focused) StreamColors.AccentBright else StreamColors.Accent,
                    radius = 4.dp.toPx(),
                    center = Offset(head, y),
                )
            }
        }
    }
}

@Composable
private fun TransportButton(
    focusRequester: FocusRequester? = null,
    label: String? = null,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    content: @Composable (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(7.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 42.dp, height = 30.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .border(1.dp, if (focused) StreamColors.AccentBright else Color.Transparent, shape)
            .then(
                if (label != null) {
                    Modifier.semantics { contentDescription = label }
                } else {
                    Modifier
                },
            )
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = StreamColors.TextSecondary,
            focusedContainerColor = StreamColors.Surface,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content(focused)
        }
    }
}

@Composable
private fun TransportSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(width = 42.dp, height = 30.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PlayPauseGlyph(playing: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = this.size.width
        val h = this.size.height
        if (playing) {
            val bar = w * 0.22f
            drawRect(color, topLeft = Offset(w * 0.18f, h * 0.12f), size = Size(bar, h * 0.76f))
            drawRect(color, topLeft = Offset(w * 0.60f, h * 0.12f), size = Size(bar, h * 0.76f))
        } else {
            val path = Path().apply {
                moveTo(w * 0.22f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.5f)
                lineTo(w * 0.22f, h * 0.90f)
                close()
            }
            drawPath(path, color)
        }
    }
}

/**
 * Outline chevron for the seek buttons: the shape stays visibly different from
 * the filled play triangle and the double item arrows next to it.
 */
@Composable
private fun SeekChevronGlyph(forward: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = this.size.width
        val h = this.size.height
        val dir = if (forward) 1f else -1f
        val cx = w * 0.5f
        val path = Path().apply {
            moveTo(cx - dir * w * 0.13f, h * 0.18f)
            lineTo(cx + dir * w * 0.17f, h * 0.5f)
            lineTo(cx - dir * w * 0.13f, h * 0.82f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = w * 0.12f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/** Two filled triangles: previous / next episode, or channel on live. */
@Composable
private fun SkipItemGlyph(forward: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = this.size.width
        val h = this.size.height
        val dir = if (forward) 1f else -1f
        val cx = w * 0.5f
        fun triangle(centerX: Float) {
            val path = Path().apply {
                moveTo(centerX - dir * w * 0.15f, h * 0.20f)
                lineTo(centerX + dir * w * 0.15f, h * 0.5f)
                lineTo(centerX - dir * w * 0.15f, h * 0.80f)
                close()
            }
            drawPath(path, color)
        }
        triangle(cx - dir * w * 0.19f)
        triangle(cx + dir * w * 0.19f)
    }
}

private fun clock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun PlayerActionButton(
    action: PlayerAction,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 40.dp, height = 36.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .border(1.dp, if (focused) StreamColors.AccentBright else Color.Transparent, shape)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .semantics { contentDescription = action.label },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = StreamColors.TextSecondary,
            focusedContainerColor = StreamColors.Surface,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PlayerActionGlyph(
                action = action,
                color = if (focused) StreamColors.TextPrimary else StreamColors.TextSecondary,
            )
        }
    }
}

/** Small line icons, matching the hand-drawn Canvas glyphs used by the shell. */
@Composable
fun PlayerActionGlyph(
    action: PlayerAction,
    color: Color,
    size: Dp = 18.dp,
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        val corner = CornerRadius(w * 0.08f, w * 0.08f)
        when (action) {
            PlayerAction.INFO -> {
                drawCircle(
                    color = color,
                    radius = w * 0.40f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke,
                )
                drawCircle(
                    color = color,
                    radius = w * 0.045f,
                    center = Offset(w * 0.5f, h * 0.32f),
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.5f, h * 0.46f),
                    end = Offset(w * 0.5f, h * 0.68f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            PlayerAction.ASPECT -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.56f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.25f, h * 0.34f),
                    end = Offset(w * 0.25f, h * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.25f, h * 0.34f),
                    end = Offset(w * 0.41f, h * 0.34f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.75f, h * 0.66f),
                    end = Offset(w * 0.75f, h * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.75f, h * 0.66f),
                    end = Offset(w * 0.59f, h * 0.66f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            PlayerAction.QUALITY -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.10f, h * 0.20f),
                    size = Size(w * 0.80f, h * 0.60f),
                    cornerRadius = corner,
                    style = stroke,
                )
                listOf(0.30f, 0.46f, 0.62f).forEachIndexed { index, x ->
                    drawLine(
                        color = color,
                        start = Offset(w * x, h * 0.62f),
                        end = Offset(w * x, h * (0.46f - index * 0.08f)),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
            }

            PlayerAction.TRACKS -> {
                val path = Path().apply {
                    moveTo(w * 0.14f, h * 0.22f)
                    lineTo(w * 0.86f, h * 0.22f)
                    lineTo(w * 0.86f, h * 0.66f)
                    lineTo(w * 0.48f, h * 0.66f)
                    lineTo(w * 0.34f, h * 0.80f)
                    lineTo(w * 0.34f, h * 0.66f)
                    lineTo(w * 0.14f, h * 0.66f)
                    close()
                }
                drawPath(path, color, style = stroke)
                drawLine(
                    color = color,
                    start = Offset(w * 0.26f, h * 0.38f),
                    end = Offset(w * 0.74f, h * 0.38f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.26f, h * 0.50f),
                    end = Offset(w * 0.58f, h * 0.50f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            PlayerAction.SPEED -> {
                drawArc(
                    color = color,
                    startAngle = 150f,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.18f),
                    size = Size(w * 0.76f, h * 0.76f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.50f, h * 0.56f),
                    end = Offset(w * 0.68f, h * 0.36f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = color, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.56f))
            }

            PlayerAction.EXTERNAL -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.56f, h * 0.58f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.46f, h * 0.52f),
                    end = Offset(w * 0.84f, h * 0.16f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.62f, h * 0.16f),
                    end = Offset(w * 0.84f, h * 0.16f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.84f, h * 0.16f),
                    end = Offset(w * 0.84f, h * 0.38f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            PlayerAction.CONTROLS -> {
                listOf(0.30f, 0.50f, 0.70f).forEachIndexed { index, y ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.16f, h * y),
                        end = Offset(w * 0.84f, h * y),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = color,
                        radius = w * 0.07f,
                        center = Offset(w * (if (index == 1) 0.62f else 0.38f), h * y),
                    )
                }
            }
        }
    }
}
