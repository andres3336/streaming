package com.andres3336.streaming.data

/**
 * Parser for the curated M3U playlist.
 *
 * Supports EXTINF attributes (double, single or unquoted values), EXTGRP before
 * or after EXTINF, EXTVLCOPT HTTP headers, a UTF-8 BOM, CRLF input, and skips
 * malformed entries that have no URL.
 */
object M3uParser {

    private val attribute = Regex(
        """([A-Za-z0-9_-]+)=(?:"([^"]*)"|'([^']*)'|([^,\s]+))""",
    )

    fun parse(text: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var pending: PendingEntry? = null
        var nextGroup: String? = null

        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    pending = parseExtInf(line.substringAfter(':')).also { entry ->
                        entry.group = entry.group ?: nextGroup
                    }
                    nextGroup = null
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    val group = line.substringAfter(':').trim().ifEmpty { null }
                    val current = pending
                    if (current != null) {
                        current.group = group
                    } else {
                        nextGroup = group
                    }
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
            .associate { match ->
                val value = match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }.orEmpty()
                match.groupValues[1].lowercase() to value
            }

        return PendingEntry(
            name = displayName.ifEmpty { values["tvg-name"].orEmpty() },
            group = values["group-title"]?.trim()?.ifEmpty { null },
            logo = values["tvg-logo"]?.trim()?.ifEmpty { null },
            tvgId = values["tvg-id"]?.trim()?.ifEmpty { null },
            language = values["tvg-language"]?.trim()?.ifEmpty { null }
                ?: values["tvg-lang"]?.trim()?.ifEmpty { null },
        )
    }

    /** Finds the comma that separates attributes from the display name. */
    private fun firstUnquotedComma(text: String): Int {
        var quote: Char? = null
        for (index in text.indices) {
            when (text[index]) {
                '"', '\'' -> {
                    if (quote == null) quote = text[index]
                    else if (quote == text[index]) quote = null
                }

                ',' -> if (quote == null) return index
            }
        }
        return -1
    }

    private class PendingEntry(
        var name: String,
        var group: String?,
        var logo: String?,
        var tvgId: String?,
        var language: String?,
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
            language = language,
            userAgent = userAgent,
            referer = referer,
        )
    }
}
