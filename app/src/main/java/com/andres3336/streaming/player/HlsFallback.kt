package com.andres3336.streaming.player

/**
 * Best-effort HLS to MPEG-TS fallback. Only URLs that clearly end in .m3u8
 * are rewritten; everything else returns null and is not retried differently.
 */
object HlsFallback {

    fun tsUrl(url: String): String? {
        val queryIndex = url.indexOf('?')
        val base = if (queryIndex >= 0) url.substring(0, queryIndex) else url
        if (!base.endsWith(".m3u8", ignoreCase = true)) return null

        val query = if (queryIndex >= 0) url.substring(queryIndex) else ""
        return base.dropLast(".m3u8".length) + ".ts" + query
    }
}
