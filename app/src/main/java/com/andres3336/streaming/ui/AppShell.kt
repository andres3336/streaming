package com.andres3336.streaming.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Calendar

/** One entry of the top navigation bar. */
data class NavDestination(
    val id: String,
    val label: String,
)

/** One keycap instruction in the bottom-right hint strip. */
data class Hint(
    val key: String,
    val label: String,
)

/**
 * App-wide TV shell: a horizontal navigation bar on top, the screen title, the
 * content and a keycap hint strip.
 *
 * The shell is flat: solid fills, 1 dp borders, no transparency and no motion.
 * A top bar is used instead of a side rail because it is the row the D-pad reaches
 * first with Up, which makes the sections reliably navigable with a remote.
 */
@Composable
fun AppShell(
    section: String,
    destinations: List<NavDestination>,
    title: String,
    onSelect: (String) -> Unit,
    subtitle: String? = null,
    showTitle: Boolean = true,
    onHome: (() -> Unit)? = null,
    chips: @Composable RowScope.() -> Unit = {},
    hints: List<Hint> = emptyList(),
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
    ) {
        TopNavBar(
            section = section,
            destinations = destinations,
            onSelect = onSelect,
            onHome = onHome,
            chips = chips,
        )

        if (showTitle && title.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = title,
                    color = StreamColors.TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = subtitle,
                        color = StreamColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }

        if (hints.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            HintBar(hints)
        }
    }
}

@Composable
private fun TopNavBar(
    section: String,
    destinations: List<NavDestination>,
    onSelect: (String) -> Unit,
    onHome: (() -> Unit)?,
    chips: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HomeTile(onClick = onHome)
        Spacer(Modifier.width(4.dp))
        destinations.forEach { destination ->
            TopNavItem(
                destination = destination,
                selected = section == destination.id,
                onClick = { onSelect(destination.id) },
            )
        }
        Spacer(Modifier.weight(1f))
        chips()
        Spacer(Modifier.width(10.dp))
        Clock()
    }
}

@Composable
private fun HomeTile(onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = StreamColors.Accent,
            contentColor = StreamColors.OnAccent,
            focusedContainerColor = StreamColors.Accent,
            focusedContentColor = StreamColors.OnAccent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        modifier = Modifier.size(40.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "S",
                color = StreamColors.OnAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun TopNavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    var focused by remember { mutableStateOf(false) }
    val container = when {
        selected -> StreamColors.Accent
        focused -> StreamColors.SurfaceRaised
        else -> Color.Transparent
    }
    val content = when {
        selected -> StreamColors.OnAccent
        focused -> StreamColors.TextPrimary
        else -> StreamColors.TextSecondary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(36.dp)
            .border(
                width = 1.dp,
                color = if (focused) StreamColors.AccentBright else Color.Transparent,
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = content,
            focusedContainerColor = container,
            focusedContentColor = content,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavGlyph(id = destination.id, color = content, size = 15.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = destination.label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NavGlyph(id: String, color: Color, size: Dp = 16.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val dim = this.size.minDimension
        val stroke = Stroke(width = dim * 0.13f, cap = StrokeCap.Round)
        val corner = CornerRadius(w * 0.08f, w * 0.08f)
        when (id) {
            "home" -> {
                val roof = Path().apply {
                    moveTo(w * 0.1f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.9f, h * 0.48f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.44f),
                    size = Size(w * 0.6f, h * 0.42f),
                    cornerRadius = corner,
                    style = stroke,
                )
            }

            "live" -> {
                val path = Path().apply {
                    moveTo(w * 0.2f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.5f)
                    lineTo(w * 0.2f, h * 0.88f)
                    close()
                }
                drawPath(path, color)
            }

            "tv" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.06f, h * 0.16f),
                    size = Size(w * 0.88f, h * 0.56f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.5f, h * 0.72f),
                    end = Offset(w * 0.5f, h * 0.86f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.32f, h * 0.88f),
                    end = Offset(w * 0.68f, h * 0.88f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "channels" -> {
                listOf(0.22f, 0.5f, 0.78f).forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.1f, h * y),
                        end = Offset(w * 0.9f, h * y),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
            }

            "movies" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.84f, h * 0.64f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.55f, h * 0.36f),
                    end = Offset(w * 0.78f, h * 0.5f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.78f, h * 0.5f),
                    end = Offset(w * 0.55f, h * 0.64f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "series" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.1f),
                    size = Size(w * 0.7f, h * 0.46f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.08f, h * 0.44f),
                    size = Size(w * 0.7f, h * 0.46f),
                    cornerRadius = corner,
                    style = stroke,
                )
            }

            "search" -> {
                drawCircle(
                    color = color,
                    radius = w * 0.26f,
                    center = Offset(w * 0.42f, h * 0.42f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.62f, h * 0.62f),
                    end = Offset(w * 0.9f, h * 0.9f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "continue" -> {
                drawCircle(
                    color = color,
                    radius = w * 0.36f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.5f, h * 0.3f),
                    end = Offset(w * 0.5f, h * 0.52f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.5f, h * 0.52f),
                    end = Offset(w * 0.66f, h * 0.62f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            else -> {
                listOf(0.3f, 0.55f, 0.8f).forEachIndexed { index, y ->
                    drawLine(
                        color = color,
                        start = Offset(w * 0.1f, h * y),
                        end = Offset(w * 0.9f, h * y),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = color,
                        radius = w * 0.09f,
                        center = Offset(
                            w * if (index % 2 == 0) 0.62f else 0.35f,
                            h * y,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun Clock() {
    var hour by remember { mutableStateOf(0) }
    var minute by remember { mutableStateOf(0) }
    var day by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)
            day = dayLabel(calendar)
            delay(20_000)
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "%02d:%02d".format(hour, minute),
            color = StreamColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
        )
        if (day.isNotEmpty()) {
            Text(
                text = day,
                color = StreamColors.TextMuted,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

private val dayNames = listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")
private val monthNames = listOf(
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic",
)

private fun dayLabel(calendar: Calendar): String {
    val weekday = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
    val month = monthNames[calendar.get(Calendar.MONTH)]
    return "$weekday ${calendar.get(Calendar.DAY_OF_MONTH)} $month"
}

/** Bottom-right keycap instructions, the remote affordance the app was missing. */
@Composable
private fun HintBar(hints: List<Hint>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hints.forEachIndexed { index, hint ->
            if (index > 0) Spacer(Modifier.width(14.dp))
            Keycap(text = hint.key)
            Spacer(Modifier.width(6.dp))
            Text(
                text = hint.label,
                color = StreamColors.TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Keycap(text: String) {
    Box(
        modifier = Modifier
            .background(StreamColors.SurfaceRaised, RoundedCornerShape(5.dp))
            .border(1.dp, StreamColors.Border, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = StreamColors.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Small pill used for the header source and section switches. */
@Composable
fun QuickChip(
    label: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    var focused by remember { mutableStateOf(false) }
    val container = when {
        selected -> StreamColors.Accent
        focused -> StreamColors.SurfaceRaised
        else -> StreamColors.Surface
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(32.dp)
            .border(
                width = 1.dp,
                color = when {
                    focused -> StreamColors.AccentBright
                    selected -> Color.Transparent
                    else -> StreamColors.Border
                },
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = if (selected) StreamColors.OnAccent else StreamColors.TextSecondary,
            focusedContainerColor = container,
            focusedContentColor = if (selected) StreamColors.OnAccent else StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
