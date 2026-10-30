package com.andres3336.streaming.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared visual tokens for the TV shell. The app keeps the existing dark
 * palette; the wireframe colors were only region markers, not a theme.
 */
object StreamColors {
    val Background = Color(0xFF0B1220)
    val Panel = Color(0xFF111827)
    val PanelElevated = Color(0xFF141C2B)
    val Card = Color(0xFF0F172A)
    val CardHover = Color(0xFF1E293B)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)
    val Accent = Color(0xFF3B82F6)
    val NavAccent = Color(0xFF4CAF50)
    val Favorite = Color(0xFFFBBF24)
    val Danger = Color(0xFFFCA5A5)
    val BadgeScrim = Color(0xCC000000)
    val SelectedNav = Color(0xFF1B2A22)
    val SelectedRow = Color(0xFF1E293B)
}

object Dimens {
    val ScreenPadding = 24.dp
    val RailWidth = 104.dp
    val RailCorner = 52.dp
    val RailPadding = 12.dp
    val RailItemCorner = 16.dp
    val RailIcon = 26.dp
    val RailIconCorner = 7.dp
    val TopBarHeight = 64.dp
    val SectionGap = 20.dp
    val PanelCorner = 16.dp
    val PanelPadding = 14.dp
    val CardCorner = 12.dp
    val CardInnerCorner = 8.dp
    val GridGapH = 16.dp
    val GridGapV = 18.dp
    val GridPadding = 10.dp
    val CardScale = 1.05f
    val FavoriteBorder = 2.dp
    val CategoryWidthFraction = 0.26f
    val ContentWidthFraction = 0.74f
}
