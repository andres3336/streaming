package com.andres3336.streaming.data

/**
 * Parser for the curated M3U playlist.
 *
 * Supports EXTINF attributes, EXTGRP, EXTVLCOPT HTTP headers, a UTF-8 BOM,
 * CRLF input, and skips malformed entries that have no URL.
 */
object M3uParser {

    private val attribute = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pending: PendingEntry? = null

        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pending = parseExtInf(line.substringAfter(':'))
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    pending?.group = line.substringAfter(':').trim().ifEmpty { null }
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    pending?.applyVlcOption(line.substringAfter(':'))
                }

                line.startsWith("#") -> Unit

                else -> {
                    pending?.let { channels += it.toChannel(line) }
                    pending = null
                }
            }
        }

        return channels
    }

    private fun parseExtInf(body: String): PendingEntry {
        val comma = firstUnquotedComma(body)
        val attributes = if (comma >= 0) body.substring(0, comma) else body
        val displayName = if (comma >= 0) body.substring(comma + 1).trim() else ""

        val values = attribute.findAll(attributes)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

        return PendingEntry(
            name = displayName.ifEmpty { values["tvg-name"].orEmpty() },
            group = values["group-title"],
            logo = values["tvg-logo"],
            tvgId = values["tvg-id"],
        )
    }

    /** Finds the comma that separates attributes from the display name. */
    private fun firstUnquotedComma(text: String): Int {
        var quoted = false
        for (index in text.indices) {
            when (text[index]) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return index
            }
        }
        return -1
    }

    private class PendingEntry(
        var name: String,
        var group: String?,
        var logo: String?,
        var tvgId: String?,
    ) {
        var userAgent: String? = null
        var referer: String? = null

        fun applyVlcOption(option: String) {
            val key = option.substringBefore('=').trim().lowercase()
            val value = option.substringAfter('=', "").trim()
            if (value.isEmpty()) return

            when (key) {
                "http-user-agent" -> userAgent = value
                "http-referrer", "http-referer" -> referer = value
            }
        }

        fun toChannel(url: String) = Channel(
            name = name.ifEmpty { "Sin nombre" },
            url = url,
            group = group,
            logo = logo,
            tvgId = tvgId,
            userAgent = userAgent,
            referer = referer,
        )
    }
}
