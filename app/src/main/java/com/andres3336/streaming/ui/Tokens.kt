package com.andres3336.streaming.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flat TV theme: one dark monochrome ramp plus a single slate-blue accent.
 *
 * There is no transparency and no animation in this palette. Every surface is a
 * solid fill, focus is a solid block plus a 1 dp border, and state changes are
 * instant. Keep it that way: no alpha colors, no gradients, no animated scales.
 */
object StreamColors {
    /** App background and the backing block behind text over artwork. */
    val Background = Color(0xFF0B0C0E)

    /** Default panel: navigation pill, category rail, settings cards. */
    val Surface = Color(0xFF141619)

    /** Raised rows and list items. */
    val SurfaceRaised = Color(0xFF1B1E23)

    /** Sunken area behind images and text fields. */
    val SurfaceSunken = Color(0xFF101215)

    val Border = Color(0xFF2A2E36)
    val BorderStrong = Color(0xFF3A404B)

    /** The only accent: focus fill, primary buttons, progress, active nav. */
    val Accent = Color(0xFF485584)

    /** Pressed/selected fill, one step darker than [Accent]. */
    val AccentDeep = Color(0xFF39446A)

    /** Accent text and focus ring on the dark background. */
    val AccentBright = Color(0xFF7C8BC4)

    val OnAccent = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFF1F2F4)
    val TextSecondary = Color(0xFF9AA0A9)
    val TextMuted = Color(0xFF6B7178)

    /** Favorites stay monochrome: they reuse the accent, not a gold star. */
    val Favorite = AccentBright

    /** Errors and destructive actions only. */
    val Danger = Color(0xFFA85B5B)

    // Legacy aliases so screens can be restyled one at a time.
    val Panel = Surface
    val PanelElevated = SurfaceRaised
    val Card = SurfaceSunken
    val CardHover = AccentDeep
    val NavAccent = AccentBright
    /** Badge backing over artwork. Solid, not translucent. */
    val BadgeScrim = SurfaceRaised

    /**
     * Modal backdrop over playing video. The only non-opaque colour in the theme: a
     * fully solid layer would hide the stream the dialog is about.
     */
    val Scrim = Color(0xE60B0C0E)
    val SelectedNav = Accent
    val SelectedRow = AccentDeep
}

/**
 * Sizes tuned for a 960 x 540 dp Android TV surface. No motion: focus is
 * immediate, so anything that used to scale now keeps [CardScale] at 1.
 */
object Dimens {
    val ScreenPadding = 16.dp
    val RailWidth = 64.dp
    val RailCorner = 32.dp
    val RailPadding = 6.dp
    val RailItemCorner = 10.dp
    val RailIcon = 26.dp
    val RailIconCorner = 6.dp
    val RailGap = 5.dp
    val TopBarHeight = 40.dp
    val SectionGap = 12.dp
    val PanelCorner = 10.dp
    val PanelPadding = 10.dp
    val CardCorner = 8.dp
    val CardInnerCorner = 6.dp
    val GridGapH = 8.dp
    val GridGapV = 10.dp
    val GridPadding = 4.dp
    val CardScale = 1f
    val FavoriteBorder = 2.dp
    val RailBorder = 1.dp
    val CategoryWidthFraction = 0.25f
    val ContentWidthFraction = 0.75f
}

/** Type ramp for the flat shell. Display sizes are for hero titles. */
object TextSizes {
    val Display = 44.sp
    val ScreenTitle = 30.sp
    val SectionTitle = 20.sp
    val RowTitle = 18.sp
    val Body = 16.sp
    val Meta = 12.sp
    val Small = 11.sp
    val Tiny = 10.sp
}
