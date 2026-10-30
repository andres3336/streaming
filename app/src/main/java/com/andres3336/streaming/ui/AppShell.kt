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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** One entry of the left navigation pill. */
data class NavDestination(
    val id: String,
    val label: String,
)

/**
 * App-wide TV shell: the rounded navigation pill on the left, a compact top
 * bar, and the section content.
 */
@Composable
fun AppShell(
    section: String,
    destinations: List<NavDestination>,
    title: String,
    onSelect: (String) -> Unit,
    leading: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .padding(Dimens.ScreenPadding),
    ) {
        NavigationPill(
            section = section,
            destinations = destinations,
            onSelect = onSelect,
        )
        Spacer(Modifier.width(Dimens.SectionGap))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            TopBar(title = title, leading = leading)
            Spacer(Modifier.height(Dimens.SectionGap))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun NavigationPill(
    section: String,
    destinations: List<NavDestination>,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(Dimens.RailWidth)
            .fillMaxHeight()
            .background(StreamColors.PanelElevated, RoundedCornerShape(Dimens.RailCorner))
            .padding(Dimens.RailPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        destinations.forEach { destination ->
            NavigationPillItem(
                destination = destination,
                selected = section == destination.id,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(destination.id) },
            )
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
    val ring = when {
        focused -> StreamColors.Accent
        selected -> StreamColors.NavAccent.copy(alpha = 0.65f)
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, ring, shape),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) StreamColors.SelectedNav else Color.Transparent,
            contentColor = StreamColors.TextPrimary,
            focusedContainerColor = StreamColors.CardHover,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.08f,
            pressedScale = 0.96f,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            NavGlyph(id = destination.id)
            Spacer(Modifier.height(5.dp))
            Text(
                text = destination.label,
                color = when {
                    selected -> StreamColors.TextPrimary
                    focused -> StreamColors.TextPrimary
                    else -> StreamColors.TextSecondary
                },
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NavGlyph(id: String) {
    Box(
        modifier = Modifier
            .size(Dimens.RailIcon)
            .background(StreamColors.NavAccent, RoundedCornerShape(Dimens.RailIconCorner)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(Dimens.RailIcon).padding(6.dp)) {
            val stroke = Stroke(
                width = size.minDimension * 0.13f,
                cap = StrokeCap.Round,
            )
            val white = Color.White
            when (id) {
                "live" -> {
                    val path = Path().apply {
                        moveTo(size.width * 0.18f, size.height * 0.12f)
                        lineTo(size.width * 0.88f, size.height * 0.5f)
                        lineTo(size.width * 0.18f, size.height * 0.88f)
                        close()
                    }
                    drawPath(path, white)
                }

                "channels" -> {
                    listOf(0.2f, 0.5f, 0.8f).forEach { y ->
                        drawLine(
                            color = white,
                            start = Offset(size.width * 0.1f, size.height * y),
                            end = Offset(size.width * 0.9f, size.height * y),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round,
                        )
                    }
                }

                "movies" -> {
                    drawRoundRect(
                        color = white,
                        topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
                        size = Size(size.width * 0.84f, size.height * 0.64f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.width * 0.08f,
                        ),
                        style = stroke,
                    )
                    drawLine(
                        color = white,
                        start = Offset(size.width * 0.55f, size.height * 0.36f),
                        end = Offset(size.width * 0.78f, size.height * 0.5f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = white,
                        start = Offset(size.width * 0.78f, size.height * 0.5f),
                        end = Offset(size.width * 0.55f, size.height * 0.64f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }

                "series" -> {
                    drawRoundRect(
                        color = white,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.1f),
                        size = Size(size.width * 0.72f, size.height * 0.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.width * 0.08f,
                        ),
                        style = stroke,
                    )
                    drawRoundRect(
                        color = white,
                        topLeft = Offset(size.width * 0.08f, size.height * 0.4f),
                        size = Size(size.width * 0.72f, size.height * 0.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.width * 0.08f,
                        ),
                        style = stroke,
                    )
                }

                "search" -> {
                    drawCircle(
                        color = white,
                        radius = size.width * 0.27f,
                        center = Offset(size.width * 0.42f, size.height * 0.42f),
                        style = stroke,
                    )
                    drawLine(
                        color = white,
                        start = Offset(size.width * 0.62f, size.height * 0.62f),
                        end = Offset(size.width * 0.9f, size.height * 0.9f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }

                "continue" -> {
                    drawCircle(
                        color = white,
                        radius = size.width * 0.36f,
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        style = stroke,
                    )
                    drawLine(
                        color = white,
                        start = Offset(size.width * 0.5f, size.height * 0.3f),
                        end = Offset(size.width * 0.5f, size.height * 0.52f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = white,
                        start = Offset(size.width * 0.5f, size.height * 0.52f),
                        end = Offset(size.width * 0.66f, size.height * 0.62f),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }

                else -> {
                    listOf(0.3f, 0.55f, 0.8f).forEachIndexed { index, y ->
                        drawLine(
                            color = white,
                            start = Offset(size.width * 0.1f, size.height * y),
                            end = Offset(size.width * 0.9f, size.height * y),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = white,
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
}

@Composable
private fun TopBar(
    title: String,
    leading: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.TopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = leading,
        )
        Text(
            text = title,
            color = StreamColors.TextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1.25f),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Streaming",
                color = StreamColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.width(9.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(StreamColors.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "S",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Small pill used for the top-bar quick switches. */
@Composable
fun QuickChip(
    label: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) {
                StreamColors.Accent.copy(alpha = 0.2f)
            } else {
                StreamColors.Card
            },
            contentColor = if (selected) StreamColors.Accent else StreamColors.TextSecondary,
            focusedContainerColor = StreamColors.SelectedRow,
            focusedContentColor = StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = 1.05f,
            pressedScale = 0.97f,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp),
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
