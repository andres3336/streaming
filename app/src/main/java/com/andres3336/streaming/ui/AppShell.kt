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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.util.Calendar

/** One entry of the left navigation pill. */
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
 * App-wide TV shell: an icon-only navigation pill on the left, a status row with
 * the source chips and the clock, the screen content, and a keycap hint strip.
 *
 * The shell is flat: solid fills, 1 dp borders, no transparency and no motion.
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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .padding(start = 16.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
    ) {
        NavigationPill(
            section = section,
            destinations = destinations,
            onSelect = onSelect,
            onHome = onHome,
        )
        Spacer(Modifier.width(Dimens.SectionGap))
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            StatusRow(
                title = title,
                subtitle = subtitle,
                showTitle = showTitle,
                chips = chips,
            )
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
}

@Composable
private fun NavigationPill(
    section: String,
    destinations: List<NavDestination>,
    onSelect: (String) -> Unit,
    onHome: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .width(Dimens.RailWidth)
            .fillMaxHeight()
            .background(StreamColors.Surface, RoundedCornerShape(Dimens.RailCorner))
            .padding(Dimens.RailPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.RailGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HomeTile(onClick = onHome)
        Spacer(Modifier.height(2.dp))
        destinations.forEach { destination ->
            NavigationPillItem(
                destination = destination,
                selected = section == destination.id,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(destination.id) },
            )
        }
        ProfileDot()
    }
}

@Composable
private fun HomeTile(onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(Dimens.RailWidth - Dimens.RailPadding * 2)
            .background(StreamColors.Accent, shape),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = { onClick?.invoke() },
            enabled = onClick != null,
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                contentColor = StreamColors.OnAccent,
                focusedContainerColor = StreamColors.Accent,
                focusedContentColor = StreamColors.OnAccent,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "S",
                    color = StreamColors.OnAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun NavigationPillItem(
    destination: NavDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.RailItemCorner)
    var focused by remember { mutableStateOf(false) }
    val container = when {
        focused && !selected -> StreamColors.SurfaceRaised
        selected -> StreamColors.Accent
        else -> Color.Transparent
    }
    val glyph = when {
        selected -> StreamColors.OnAccent
        focused -> StreamColors.TextPrimary
        else -> StreamColors.TextSecondary
    }
    val border = when {
        focused -> StreamColors.AccentBright
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, border, shape),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = glyph,
            focusedContainerColor = container,
            focusedContentColor = glyph,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            NavGlyph(
                id = destination.id,
                color = glyph,
            )
        }
    }
}

@Composable
private fun ProfileDot() {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .size(34.dp)
            .background(StreamColors.SurfaceRaised, CircleShape)
            .border(1.dp, StreamColors.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "S",
            color = StreamColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PlayGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.22f, size.height * 0.1f)
            lineTo(size.width * 0.9f, size.height * 0.5f)
            lineTo(size.width * 0.22f, size.height * 0.9f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun NavGlyph(id: String, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(
            width = size.minDimension * 0.12f,
            cap = StrokeCap.Round,
        )
        val corner = CornerRadius(size.width * 0.08f, size.width * 0.08f)
        when (id) {
            "home" -> {
                val roof = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.48f)
                    lineTo(size.width * 0.5f, size.height * 0.14f)
                    lineTo(size.width * 0.9f, size.height * 0.48f)
                }
                drawPath(roof, color, style = stroke)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.2f, size.height * 0.44f),
                    size = Size(size.width * 0.6f, size.height * 0.42f),
                    cornerRadius = corner,
                    style = stroke,
                )
            }

            "live" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.12f)
                    lineTo(size.width * 0.88f, size.height * 0.5f)
                    lineTo(size.width * 0.2f, size.height * 0.88f)
                    close()
                }
                drawPath(path, color)
            }

            "tv" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.06f, size.height * 0.16f),
                    size = Size(size.width * 0.88f, size.height * 0.56f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.72f),
                    end = Offset(size.width * 0.5f, size.height * 0.86f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.32f, size.height * 0.88f),
                    end = Offset(size.width * 0.68f, size.height * 0.88f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "channels" -> {
                listOf(0.22f, 0.5f, 0.78f).forEach { y ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.1f, size.height * y),
                        end = Offset(size.width * 0.9f, size.height * y),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
            }

            "movies" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
                    size = Size(size.width * 0.84f, size.height * 0.64f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.55f, size.height * 0.36f),
                    end = Offset(size.width * 0.78f, size.height * 0.5f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.78f, size.height * 0.5f),
                    end = Offset(size.width * 0.55f, size.height * 0.64f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "series" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.1f),
                    size = Size(size.width * 0.7f, size.height * 0.46f),
                    cornerRadius = corner,
                    style = stroke,
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.08f, size.height * 0.44f),
                    size = Size(size.width * 0.7f, size.height * 0.46f),
                    cornerRadius = corner,
                    style = stroke,
                )
            }

            "search" -> {
                drawCircle(
                    color = color,
                    radius = size.width * 0.26f,
                    center = Offset(size.width * 0.42f, size.height * 0.42f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.62f, size.height * 0.62f),
                    end = Offset(size.width * 0.9f, size.height * 0.9f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            "continue" -> {
                drawCircle(
                    color = color,
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.3f),
                    end = Offset(size.width * 0.5f, size.height * 0.52f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.5f, size.height * 0.52f),
                    end = Offset(size.width * 0.66f, size.height * 0.62f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }

            else -> {
                listOf(0.3f, 0.55f, 0.8f).forEachIndexed { index, y ->
                    drawLine(
                        color = color,
                        start = Offset(size.width * 0.1f, size.height * y),
                        end = Offset(size.width * 0.9f, size.height * y),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = color,
                        radius = size.width * 0.09f,
                        center = Offset(
                            size.width * if (index % 2 == 0) 0.62f else 0.35f,
                            size.height * y,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String?,
    showTitle: Boolean,
    chips: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(Dimens.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTitle) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = StreamColors.TextPrimary,
                    fontSize = TextSizes.ScreenTitle,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = StreamColors.TextSecondary,
                        fontSize = TextSizes.Small,
                        maxLines = 1,
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = chips,
        )
        Spacer(Modifier.width(10.dp))
        Clock()
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
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
        )
        if (day.isNotEmpty()) {
            Text(
                text = day,
                color = StreamColors.TextMuted,
                fontSize = 11.sp,
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
