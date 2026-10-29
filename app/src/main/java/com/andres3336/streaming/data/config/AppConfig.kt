package com.andres3336.streaming.data.config

enum class LiveMode { GIST, XTREAM, MERGE }

data class XtreamConfig(
    val baseUrl: String,
    val username: String,
    val password: String,
)

data class SourcesConfig(
    val liveMode: LiveMode,
    val m3uUrl: String? = null,
    val xtream: XtreamConfig? = null,
)

data class PlayerConfig(
    val userAgent: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val bufferMs: Int = 3000,
)

data class UiConfig(
    val simpleMode: Boolean = false,
    val pin: String? = null,
    val sections: List<String> = listOf("live"),
)

data class AppConfig(
    val version: Int,
    val sources: SourcesConfig,
    val player: PlayerConfig = PlayerConfig(),
    val epgUrl: String? = null,
    val ui: UiConfig = UiConfig(),
    val forceUpdate: Boolean = false,
)

sealed interface ConfigResult {
    data class Valid(val config: AppConfig) : ConfigResult
    data class Invalid(val errors: List<String>) : ConfigResult
}
