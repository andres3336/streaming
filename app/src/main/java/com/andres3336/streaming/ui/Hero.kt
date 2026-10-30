package com.andres3336.streaming.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/** Filled primary pill or flat outlined secondary pill. Focus adds a 1 dp ring. */
@Composable
fun PillButton(
    label: String,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    var focused by remember { mutableStateOf(false) }
    val container = when {
        primary -> StreamColors.Accent
        focused -> StreamColors.SurfaceRaised
        else -> Color.Transparent
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(36.dp)
            .border(
                width = 1.dp,
                color = when {
                    focused -> StreamColors.AccentBright
                    primary -> Color.Transparent
                    else -> StreamColors.BorderStrong
                },
                shape = shape,
            )
            .onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = if (primary) StreamColors.OnAccent else StreamColors.TextPrimary,
            focusedContainerColor = container,
            focusedContentColor = if (primary) StreamColors.OnAccent else StreamColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * Hero banner with a solid text block on the left and the artwork on the right.
 * No scrim, no gradient: the text never sits on the photo.
 */
@Composable
fun HeroBlock(
    title: String,
    badge: String?,
    meta: List<String>,
    synopsis: String?,
    image: String?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    height: Int = 268,
    titleSize: TextUnit = TextSizes.Display,
) {
    // The hero grows with the text scale so a bigger type never clips its meta.
    val fontScale = LocalDensity.current.fontScale
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height((height * fontScale).dp),
    ) {
        Box(
            modifier = Modifier
                .weight(0.56f)
                .fillMaxHeight()
                .background(StreamColors.Surface, RoundedCornerShape(Dimens.PanelCorner))
                .border(1.dp, StreamColors.Border, RoundedCornerShape(Dimens.PanelCorner))
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .background(StreamColors.Accent, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = badge.uppercase(),
                            color = StreamColors.OnAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = title,
                    color = StreamColors.TextPrimary,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = meta.filter { it.isNotBlank() }.joinToString("  ·  "),
                        color = StreamColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!synopsis.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = synopsis,
                        color = StreamColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = if (height < 240) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(if (height < 240) 10.dp else 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(label = primaryLabel, primary = true, onClick = onPrimary)
                    if (secondaryLabel != null && onSecondary != null) {
                        PillButton(label = secondaryLabel, onClick = onSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(0.44f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Dimens.PanelCorner))
                .background(StreamColors.SurfaceSunken)
                .border(1.dp, StreamColors.Border, RoundedCornerShape(Dimens.PanelCorner)),
        ) {
            coil.compose.AsyncImage(
                model = image,
                contentDescription = null,
                placeholder = androidx.compose.ui.res.painterResource(
                    com.andres3336.streaming.R.drawable.ic_channel_placeholder,
                ),
                error = androidx.compose.ui.res.painterResource(
                    com.andres3336.streaming.R.drawable.ic_channel_placeholder,
                ),
                fallback = androidx.compose.ui.res.painterResource(
                    com.andres3336.streaming.R.drawable.ic_channel_placeholder,
                ),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
            )
        }
    }
}
