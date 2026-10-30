package com.andres3336.streaming.data.settings

object SettingsOptions {
    val BUFFER_PRESETS = listOf(2_000, 3_000, 5_000)
    val PRE_BUFFER_PRESETS = listOf(1_000, 2_000, 3_000, 5_000)
    val QUALITY_CAPS = listOf("auto", "1080p", "720p", "480p", "360p")
    val QUALITY_PREFERENCES = listOf("auto", "1080p", "720p", "480p", "360p")
    val ALL_ASPECT_MODES = listOf("fit", "fill", "stretch", "zoom", "original", "16:9", "4:3")
    val UI_ASPECT_MODES = ALL_ASPECT_MODES
    val CACHE_SIZES = listOf(100, 250, 500)
    val BITRATES_KBPS = listOf(0, 200_000, 100_000, 50_000)
    val TEXT_SCALES = listOf(0.9f, 1.0f, 1.15f, 1.3f, 1.45f)
    val NAVIGATION_MODES = listOf("top", "rail")
    val PLAYER_BARS = listOf("full", "compact")
    val PLAYER_BUTTONS = listOf("info", "aspect", "tracks", "quality", "speed", "external")
    val MIN_CATEGORY_ITEMS = listOf(0, 2, 3, 5)
    val SPEEDS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
}
