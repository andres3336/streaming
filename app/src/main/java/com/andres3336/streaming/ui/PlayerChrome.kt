package com.andres3336.streaming.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Actions available in the player's uniform top bar. */
enum class PlayerAction(val label: String) {
    INFO("Información"),
    ASPECT("Aspecto"),
    QUALITY("Calidad"),
    TRACKS("Audio y subtítulos"),
    SPEED("Velocidad"),
    EXTERNAL("Abrir en otro reproductor"),
    CONTROLS("Controles"),
}

/**
 * One flat player bar: title on the left and icon-only actions on the right.
 *
 * The caller controls visibility (auto-hide) and keeps the bar on screen while
 * one of its buttons has focus, so D-pad navigation never loses its target.
 */
@Composable
fun PlayerTopBar(
    title: String,
    actions: List<PlayerAction>,
    visible: Boolean,
    onAction: (PlayerAction) -> Unit,
    firstActionFocus: FocusRequester,
    aspectFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(StreamColors.SurfaceRaised, shape)
            .border(1.dp, StreamColors.Border, shape)
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
        Spacer(Modifier.width(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEachIndexed { index, action ->
                val focusRequester = when {
                    index == 0 -> firstActionFocus
                    action == PlayerAction.ASPECT -> aspectFocus
                    else -> null
                }
                PlayerActionButton(
                    action = action,
                    focusRequester = focusRequester,
                    onClick = { onAction(action) },
                    onFocusChanged = onFocusChanged,
                )
            }
        }
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
