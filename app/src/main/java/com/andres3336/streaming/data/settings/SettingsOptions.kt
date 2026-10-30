package com.andres3336.streaming.data.settings

object SettingsOptions {
    val BUFFER_PRESETS = listOf(2_000, 3_000, 5_000)
    val QUALITY_CAPS = listOf("auto", "1080p", "720p", "480p", "360p")
    val ALL_ASPECT_MODES = listOf("fit", "fill", "stretch", "zoom", "original", "16:9", "4:3")
    val UI_ASPECT_MODES = ALL_ASPECT_MODES
    val CACHE_SIZES = listOf(100, 250, 500)
    val BITRATES_KBPS = listOf(0, 15_000, 10_000, 8_000, 5_000, 3_000)
}
