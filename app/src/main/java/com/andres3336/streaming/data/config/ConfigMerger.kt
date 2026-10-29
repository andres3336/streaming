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
                ?.takeIf { it in 0..30_000 }
                ?: config.player.bufferMs,
            qualityCap = local.qualityCap
                ?.takeIf { it in SettingsOptions.QUALITY_CAPS }
                ?: config.player.qualityCap,
            aspectMode = local.aspectMode
                ?.takeIf { it in SettingsOptions.ALL_ASPECT_MODES }
                ?: config.player.aspectMode,
        )

        val cache = config.cache.copy(
            sizeMb = local.cacheSizeMb
                ?.takeIf { it in SettingsOptions.CACHE_SIZES }
                ?: config.cache.sizeMb,
        )

        val network = config.network.copy(
            wifiOnly = local.wifiOnly ?: config.network.wifiOnly,
            maxBitrateKbps = local.maxBitrateKbps
                ?.takeIf { it in 0..1_000_000 }
                ?: config.network.maxBitrateKbps,
        )

        return config.copy(player = player, cache = cache, network = network)
    }
}
