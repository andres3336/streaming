package com.andres3336.streaming.data.config

import com.andres3336.streaming.data.settings.DeviceSettings
import com.andres3336.streaming.data.settings.SettingsOptions

/**
 * Deep merge of the central config and the local device overrides.
 * Local values win only for the keys that are present.
 */
object ConfigMerger {

    fun apply(config: AppConfig, local: DeviceSettings): AppConfig {
        val player = config.player.copy(
            bufferMs = local.bufferMs
                ?.takeIf { it in 500..30_000 }
                ?: config.player.bufferMs,
            preBufferMs = local.preBufferMs
                ?.takeIf { it in 500..30_000 }
                ?: config.player.preBufferMs,
            qualityCap = local.qualityCap
                ?.takeIf { it in SettingsOptions.QUALITY_CAPS }
                ?: config.player.qualityCap,
            qualityPreference = local.qualityPreference
                ?.takeIf { it in SettingsOptions.QUALITY_PREFERENCES }
                ?: config.player.qualityPreference,
            aspectMode = local.aspectMode
                ?.takeIf { it in SettingsOptions.ALL_ASPECT_MODES }
                ?: config.player.aspectMode,
            liveLowLatency = local.liveLowLatency ?: config.player.liveLowLatency,
        )

        val cache = config.cache.copy(
            sizeMb = local.cacheSizeMb
                ?.takeIf { it in SettingsOptions.CACHE_SIZES }
                ?: config.cache.sizeMb,
            clearOnStart = local.clearOnStart ?: config.cache.clearOnStart,
        )

        val network = config.network.copy(
            wifiOnly = local.wifiOnly ?: config.network.wifiOnly,
            maxBitrateKbps = local.maxBitrateKbps
                ?.takeIf { it in 0..1_000_000 }
                ?: config.network.maxBitrateKbps,
        )

        val xtream = config.sources.xtream?.copy(
            minCategoryItems = local.minCategoryItems
                ?.takeIf { it in 0..50 }
                ?: config.sources.xtream.minCategoryItems,
            hideEmptyCategories = local.hideEmptyCategories
                ?: config.sources.xtream.hideEmptyCategories,
        )

        val sources = config.sources.copy(xtream = xtream)

        val subtitles = config.subtitles.copy(
            autoDownload = local.autoDownloadSubtitles ?: config.subtitles.autoDownload,
        )

        val ui = config.ui.copy(
            fontScale = local.textScale
                ?.takeIf { it in 0.8f..1.5f }
                ?: config.ui.fontScale,
            uiScale = local.uiScale
                ?.takeIf { it in 0.8f..1.5f }
                ?: config.ui.uiScale,
            navigation = local.navigation
                ?.takeIf { it in SettingsOptions.NAVIGATION_MODES }
                ?: config.ui.navigation,
            playerBar = local.playerBar
                ?.takeIf { it in SettingsOptions.PLAYER_BARS }
                ?: config.ui.playerBar,
            playerButtons = local.playerButtons
                ?.filter { it in SettingsOptions.PLAYER_BUTTONS }
                ?: config.ui.playerButtons,
        )

        return config.copy(
            player = player,
            cache = cache,
            network = network,
            sources = sources,
            subtitles = subtitles,
            ui = ui,
        )
    }
}
