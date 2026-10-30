package com.andres3336.streaming.data.cache

import com.andres3336.streaming.data.config.AppConfig
import java.security.MessageDigest

/**
 * Stable, non-reversible identities for the sources that own a cache.
 * A changed URL, header set, user agent or Xtream account invalidates only
 * that source's cache.
 */
object CacheIdentity {

    fun m3u(config: AppConfig): String? {
        val url = config.sources.m3uUrl?.takeIf(String::isNotBlank) ?: return null
        val headers = config.player.headers.toSortedMap()
            .entries
            .joinToString("|") { "${it.key}:${it.value}" }
        return sha256("m3u|$url|${config.player.userAgent.orEmpty()}|$headers")
    }

    fun xtream(config: AppConfig): String? {
        val xtream = config.sources.xtream ?: return null
        return sha256("xtream|${xtream.baseUrl}|${xtream.username}|${xtream.password}")
    }

    fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
