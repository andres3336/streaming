package com.andres3336.streaming.data.config

import java.net.URI
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses and validates the remote config. Errors are user-facing and in Spanish.
 */
object ConfigParser {

    private val modes = mapOf(
        "gist" to LiveMode.GIST,
        "xtream" to LiveMode.XTREAM,
        "merge" to LiveMode.MERGE,
    )

    private val allowedSections = setOf("live", "movies", "series")
    private val qualityCaps = setOf("auto", "1080p", "720p", "480p", "360p")
    private val aspectModes = setOf("fit", "fill", "stretch", "zoom", "original")
    private val themes = setOf("dark", "light", "system")
    private val logLevels = setOf("debug", "info", "warn", "error")
    private val cacheSizes = setOf(100, 250, 500)
    private val accentPattern = Regex("""#[0-9A-Fa-f]{6}""")

    fun parse(text: String): ConfigResult {
        val root = try {
            JSONObject(text)
        } catch (error: JSONException) {
            return ConfigResult.Invalid(listOf("JSON inválido: ${error.message}"))
        }

        val errors = mutableListOf<String>()

        val version = root.optInt("version", 0)
        if (version < 1) errors += "version debe ser mayor o igual a 1"

        val sourcesJson = root.objectOrNull("sources")
        if (sourcesJson == null) errors += "falta sources"

        val mode = sourcesJson?.optString("liveMode")
            ?.lowercase()
            ?.let(modes::get)
        if (mode == null) {
            errors += "sources.liveMode debe ser gist, xtream o merge"
        }

        val m3uUrl = sourcesJson?.stringOrNull("m3uUrl")
        if ((mode == LiveMode.GIST || mode == LiveMode.MERGE) && !isHttpUrl(m3uUrl)) {
            errors += "sources.m3uUrl debe ser una URL http o https"
        }

        val xtream = sourcesJson?.objectOrNull("xtream")?.let { json ->
            XtreamConfig(
                baseUrl = json.optString("baseUrl").trim(),
                username = json.optString("username"),
                password = json.optString("password"),
            )
        }
        if (xtream == null && (mode == LiveMode.XTREAM || mode == LiveMode.MERGE)) {
            errors += "falta sources.xtream"
        }
        if (xtream != null) {
            if (!isHttpUrl(xtream.baseUrl)) errors += "sources.xtream.baseUrl inválida"
            if (xtream.username.isBlank()) errors += "sources.xtream.username vacío"
            if (xtream.password.isBlank()) errors += "sources.xtream.password vacío"
        }

        val playerJson = root.objectOrNull("player")
        val player = PlayerConfig(
            userAgent = playerJson?.stringOrNull("userAgent"),
            headers = playerJson?.objectOrNull("headers")?.let { headers ->
                buildMap {
                    headers.keys().forEach { key -> put(key, headers.optString(key)) }
                }
            }.orEmpty(),
            bufferMs = playerJson?.optInt("bufferMs", 3000) ?: 3000,
            minBufferMs = playerJson?.optInt("minBufferMs", 15000) ?: 15000,
            maxBufferMs = playerJson?.optInt("maxBufferMs", 50000) ?: 50000,
            retryCount = playerJson?.optInt("retryCount", 3) ?: 3,
            retryDelayMs = playerJson?.optInt("retryDelayMs", 1000) ?: 1000,
            reconnect = playerJson?.optBoolean("reconnect", true) ?: true,
            qualityCap = playerJson?.optString("qualityCap", "auto")
                ?.lowercase()
                ?.takeIf(String::isNotEmpty) ?: "auto",
            aspectMode = playerJson?.optString("aspectMode", "fit")
                ?.lowercase()
                ?.takeIf(String::isNotEmpty) ?: "fit",
            audioOnly = playerJson?.optBoolean("audioOnly", false) ?: false,
            sleepTimerMinutes = playerJson?.optInt("sleepTimerMinutes", 0) ?: 0,
        )
        if (player.bufferMs !in 0..30_000) errors += "player.bufferMs fuera de rango"
        if (player.minBufferMs !in 1_000..60_000) errors += "player.minBufferMs fuera de rango"
        if (player.maxBufferMs !in 1_000..120_000) errors += "player.maxBufferMs fuera de rango"
        if (player.maxBufferMs < player.minBufferMs) {
            errors += "player.maxBufferMs debe ser mayor o igual a player.minBufferMs"
        }
        if (player.retryCount !in 0..10) errors += "player.retryCount fuera de rango"
        if (player.retryDelayMs !in 0..10_000) errors += "player.retryDelayMs fuera de rango"
        if (player.qualityCap !in qualityCaps) errors += "player.qualityCap inválida"
        if (player.aspectMode !in aspectModes) errors += "player.aspectMode inválido"
        if (player.sleepTimerMinutes !in 0..720) errors += "player.sleepTimerMinutes fuera de rango"

        val networkJson = root.objectOrNull("network")
        val network = NetworkConfig(
            wifiOnly = networkJson?.optBoolean("wifiOnly", false) ?: false,
            maxBitrateKbps = networkJson?.optInt("maxBitrateKbps", 0) ?: 0,
            connectTimeoutMs = networkJson?.optInt("connectTimeoutMs", 15000) ?: 15000,
            readTimeoutMs = networkJson?.optInt("readTimeoutMs", 20000) ?: 20000,
        )
        if (network.maxBitrateKbps !in 0..1_000_000) {
            errors += "network.maxBitrateKbps fuera de rango"
        }
        if (network.connectTimeoutMs !in 1_000..60_000) {
            errors += "network.connectTimeoutMs fuera de rango"
        }
        if (network.readTimeoutMs !in 1_000..120_000) {
            errors += "network.readTimeoutMs fuera de rango"
        }

        val cacheJson = root.objectOrNull("cache")
        val cache = CacheConfig(
            vodEnabled = cacheJson?.optBoolean("vodEnabled", true) ?: true,
            sizeMb = cacheJson?.optInt("sizeMb", 250) ?: 250,
            clearOnStart = cacheJson?.optBoolean("clearOnStart", false) ?: false,
        )
        if (cache.sizeMb !in cacheSizes) errors += "cache.sizeMb debe ser 100, 250 o 500"

        val liveJson = root.objectOrNull("live")
        val live = LiveConfig(
            startLastChannel = liveJson?.optBoolean("startLastChannel", true) ?: true,
            zapping = liveJson?.optBoolean("zapping", true) ?: true,
        )

        val epgJson = root.objectOrNull("epg")
        val legacyEpgUrl = root.stringOrNull("epgUrl")
        val epgUrl = epgJson?.stringOrNull("url") ?: legacyEpgUrl
        val epg = EpgConfig(
            url = epgUrl,
            refreshHours = epgJson?.optInt("refreshHours", 12) ?: 12,
        )
        if (epgUrl != null && !isHttpUrl(epgUrl)) errors += "epg.url inválida"
        if (epg.refreshHours !in 1..168) errors += "epg.refreshHours fuera de rango"

        val diagnosticsJson = root.objectOrNull("diagnostics")
        val diagnostics = DiagnosticsConfig(
            showStats = diagnosticsJson?.optBoolean("showStats", false) ?: false,
            logLevel = diagnosticsJson?.optString("logLevel", "warn")
                ?.lowercase()
                ?.takeIf(String::isNotEmpty) ?: "warn",
        )
        if (diagnostics.logLevel !in logLevels) errors += "diagnostics.logLevel inválido"

        val uiJson = root.objectOrNull("ui")
        val enabledSections = uiJson?.optJSONArray("sections")?.let { array ->
            (0 until array.length()).map { array.optString(it).lowercase() }
        } ?: listOf("live")
        if (enabledSections.isEmpty() || enabledSections.any { it !in allowedSections }) {
            errors += "ui.sections inválidas"
        }

        val accentColor = uiJson?.optString("accentColor", "#3B82F6") ?: "#3B82F6"
        if (!accentPattern.matches(accentColor)) errors += "ui.accentColor inválido"

        val fontScale = uiJson?.optDouble("fontScale", 1.0)?.toFloat() ?: 1.0f
        if (fontScale !in 0.8f..2.0f) errors += "ui.fontScale fuera de rango"

        val theme = uiJson?.optString("theme", "dark")?.lowercase()?.takeIf(String::isNotEmpty)
            ?: "dark"
        if (theme !in themes) errors += "ui.theme inválido"

        val ui = UiConfig(
            sections = enabledSections,
            theme = theme,
            accentColor = accentColor,
            fontScale = fontScale,
            showChannelNumbers = uiJson?.optBoolean("showChannelNumbers", true) ?: true,
            showLogos = uiJson?.optBoolean("showLogos", true) ?: true,
            showNowNext = uiJson?.optBoolean("showNowNext", false) ?: false,
        )

        if (errors.isNotEmpty()) return ConfigResult.Invalid(errors)

        return ConfigResult.Valid(
            AppConfig(
                version = version,
                sources = SourcesConfig(
                    liveMode = requireNotNull(mode),
                    m3uUrl = m3uUrl,
                    xtream = xtream,
                ),
                player = player,
                network = network,
                cache = cache,
                live = live,
                epg = epg,
                diagnostics = diagnostics,
                ui = ui,
                forceUpdate = root.optBoolean("forceUpdate", false),
            ),
        )
    }

    private fun JSONObject.objectOrNull(name: String): JSONObject? =
        if (has(name) && !isNull(name)) optJSONObject(name) else null

    private fun JSONObject.stringOrNull(name: String): String? =
        if (has(name) && !isNull(name)) {
            optString(name).trim().takeIf(String::isNotEmpty)
        } else {
            null
        }

    private fun isHttpUrl(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        return try {
            val uri = URI(value)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }
}
