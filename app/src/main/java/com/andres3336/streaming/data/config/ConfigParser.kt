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

    private val sections = setOf("live", "movies", "series")
    private val pinPattern = Regex("""\d{4,8}""")

    fun parse(text: String): ConfigResult {
        val root = try {
            JSONObject(text)
        } catch (error: JSONException) {
            return ConfigResult.Invalid(listOf("JSON inválido: ${error.message}"))
        }

        val errors = mutableListOf<String>()

        val version = root.optInt("version", 0)
        if (version < 1) errors += "version debe ser mayor o igual a 1"

        val sourcesJson = root.optJSONObject("sources")
        if (sourcesJson == null) errors += "falta sources"

        val mode = sourcesJson?.optString("liveMode")
            ?.lowercase()
            ?.let(modes::get)
        if (mode == null) {
            errors += "sources.liveMode debe ser gist, xtream o merge"
        }

        val m3uUrl = sourcesJson?.optString("m3uUrl")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if ((mode == LiveMode.GIST || mode == LiveMode.MERGE) && !isHttpUrl(m3uUrl)) {
            errors += "sources.m3uUrl debe ser una URL http o https"
        }

        val xtream = sourcesJson?.optJSONObject("xtream")?.let { json ->
            XtreamConfig(
                baseUrl = json.optString("baseUrl").trim(),
                username = json.optString("username"),
                password = json.optString("password"),
            )
        }
        if (mode == LiveMode.XTREAM || mode == LiveMode.MERGE) {
            when {
                xtream == null -> errors += "falta sources.xtream"
                !isHttpUrl(xtream.baseUrl) -> errors += "sources.xtream.baseUrl inválida"
                xtream.username.isBlank() -> errors += "sources.xtream.username vacío"
                xtream.password.isBlank() -> errors += "sources.xtream.password vacío"
            }
        }

        val playerJson = root.optJSONObject("player")
        val player = PlayerConfig(
            userAgent = playerJson?.optString("userAgent")
                ?.trim()
                ?.takeIf(String::isNotEmpty),
            headers = playerJson?.optJSONObject("headers")?.let { headers ->
                buildMap {
                    headers.keys().forEach { key -> put(key, headers.optString(key)) }
                }
            }.orEmpty(),
            bufferMs = playerJson?.optInt("bufferMs", 3000) ?: 3000,
        )
        if (player.bufferMs !in 0..30_000) errors += "player.bufferMs fuera de rango"

        val epgUrl = if (root.has("epgUrl") && !root.isNull("epgUrl")) {
            root.optString("epgUrl").trim().takeIf(String::isNotEmpty)
        } else {
            null
        }
        if (epgUrl != null && !isHttpUrl(epgUrl)) errors += "epgUrl inválida"

        val uiJson = root.optJSONObject("ui")
        val enabledSections = uiJson?.optJSONArray("sections")?.let { array ->
            (0 until array.length()).map { array.optString(it) }
        } ?: listOf("live")
        if (enabledSections.isEmpty() || enabledSections.any { it !in sections }) {
            errors += "ui.sections inválidas"
        }

        val pin = uiJson?.optString("pin")?.takeIf(String::isNotBlank)
        if (pin != null && !pinPattern.matches(pin)) {
            errors += "ui.pin debe tener entre 4 y 8 dígitos"
        }

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
                epgUrl = epgUrl,
                ui = UiConfig(
                    simpleMode = uiJson?.optBoolean("simpleMode", false) ?: false,
                    pin = pin,
                    sections = enabledSections,
                ),
                forceUpdate = root.optBoolean("forceUpdate", false),
            ),
        )
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
