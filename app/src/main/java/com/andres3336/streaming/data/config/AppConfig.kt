package com.andres3336.streaming.data.config

enum class LiveMode { GIST, XTREAM, MERGE }

data class XtreamConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
    val minCategoryItems: Int = 2,
    val hideEmptyCategories: Boolean = true,
)

data class SourcesConfig(
    // Legacy v1/v2 field. v3 derives the available sections from the sources.
    val liveMode: LiveMode = LiveMode.GIST,
    val m3uUrl: String? = null,
    val xtream: XtreamConfig? = null,
)

data class PlayerConfig(
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val bufferMs: Int = 3000,
    val preBufferMs: Int = 5000,
    val minBufferMs: Int = 15000,
    val maxBufferMs: Int = 50000,
    val retryCount: Int = 3,
    val retryDelayMs: Int = 1000,
    val reconnect: Boolean = true,
    val qualityCap: String = "auto",
    val qualityPreference: String = "auto",
    val aspectMode: String = "fit",
    val liveLowLatency: Boolean = false,
    val audioOnly: Boolean = false,
    val sleepTimerMinutes: Int = 0,
)

data class NetworkConfig(
    val wifiOnly: Boolean = false,
    val maxBitrateKbps: Int = 0,
    val connectTimeoutMs: Int = 15000,
    val readTimeoutMs: Int = 20000,
)

data class CacheConfig(
    val vodEnabled: Boolean = true,
    val sizeMb: Int = 250,
    val clearOnStart: Boolean = false,
)

data class LiveConfig(
    val startLastChannel: Boolean = true,
    val zapping: Boolean = true,
)

data class EpgConfig(
    val url: String? = null,
    val refreshHours: Int = 12,
)

data class DiagnosticsConfig(
    val showStats: Boolean = false,
    val logLevel: String = "warn",
)

data class SubtitlesConfig(
    val enabled: Boolean = true,
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val languages: List<String> = listOf("es", "en"),
    val autoDownload: Boolean = true,
)

data class UiConfig(
    val sections: List<String> = listOf("live"),
    val theme: String = "dark",
    val accentColor: String = "#3B82F6",
    val fontScale: Float = 1.0f,
    val uiScale: Float = 1.0f,
    val navigation: String = "top",
    val playerBar: String = "full",
    val playerButtons: List<String> = listOf(
        "info",
        "aspect",
        "tracks",
        "quality",
        "speed",
        "external",
    ),
    val showChannelNumbers: Boolean = true,
    val showLogos: Boolean = true,
    val showNowNext: Boolean = false,
)

data class AppConfig(
    val version: Int,
    val sources: SourcesConfig,
    val player: PlayerConfig = PlayerConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val cache: CacheConfig = CacheConfig(),
    val live: LiveConfig = LiveConfig(),
    val epg: EpgConfig = EpgConfig(),
    val diagnostics: DiagnosticsConfig = DiagnosticsConfig(),
    val subtitles: SubtitlesConfig = SubtitlesConfig(),
    val ui: UiConfig = UiConfig(),
    val forceUpdate: Boolean = false,
)

sealed interface ConfigResult {
    data class Valid(val config: AppConfig) : ConfigResult
    data class Invalid(val errors: List<String>) : ConfigResult
}
