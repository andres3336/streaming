package com.andres3336.streaming.data.config

import com.andres3336.streaming.data.M3uFetch
import com.andres3336.streaming.data.M3uRepository
import com.andres3336.streaming.data.xtream.XtreamClient
import com.andres3336.streaming.data.xtream.XtreamResult
import okhttp3.OkHttpClient

data class SourceHealth(
    val liveChecked: Boolean = false,
    val liveHealthy: Boolean = false,
    val liveMessage: String? = null,
    val xtreamChecked: Boolean = false,
    val xtreamHealthy: Boolean = false,
    val xtreamMessage: String? = null,
) {
    val isHealthy: Boolean
        get() = (!liveChecked || liveHealthy) && (!xtreamChecked || xtreamHealthy)

    fun summary(): String = buildList {
        if (liveChecked) add(if (liveHealthy) "En vivo: OK" else "En vivo: fallo")
        if (xtreamChecked) add(if (xtreamHealthy) "Canales: OK" else "Canales: fallo")
        if (isEmpty()) add("No hay fuentes que comprobar")
    }.joinToString(" · ")
}

/**
 * Checks only the sources enabled by the current config and mode. Individual
 * channel failures are handled by the player and never reach this path.
 */
class SourceHealthChecker(
    private val client: OkHttpClient,
    private val m3uRepository: M3uRepository,
) {

    suspend fun check(config: AppConfig): SourceHealth {
        var liveChecked = false
        var liveHealthy = false
        var liveMessage: String? = null
        var xtreamChecked = false
        var xtreamHealthy = false
        var xtreamMessage: String? = null

        val m3uUrl = config.sources.m3uUrl
        val checkLive = m3uUrl != null
        if (checkLive) {
            liveChecked = true
            when (
                val result = m3uRepository.load(
                    url = m3uUrl,
                    userAgent = config.player.userAgent,
                    headers = config.player.headers,
                )
            ) {
                is M3uFetch.Success -> liveHealthy = true
                is M3uFetch.Failure -> liveMessage = result.message
            }
        }

        val xtream = config.sources.xtream
        if (xtream != null) {
            xtreamChecked = true
            val xtreamClient = XtreamClient(
                config = xtream,
                client = client,
                userAgent = config.player.userAgent,
            )
            when (val result = xtreamClient.accountStatus()) {
                is XtreamResult.Success -> {
                    xtreamHealthy = result.value.authenticated
                    if (!xtreamHealthy) {
                        xtreamMessage = "La cuenta Xtream no está activa (${result.value.status})."
                    }
                }

                is XtreamResult.Failure -> xtreamMessage = result.message
            }
        }

        return SourceHealth(
            liveChecked = liveChecked,
            liveHealthy = liveHealthy,
            liveMessage = liveMessage,
            xtreamChecked = xtreamChecked,
            xtreamHealthy = xtreamHealthy,
            xtreamMessage = xtreamMessage,
        )
    }
}
