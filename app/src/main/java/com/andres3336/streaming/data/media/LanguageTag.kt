package com.andres3336.streaming.data.media

/**
 * Normalizes the loose language hints found in playlists and Xtream metadata
 * into the short badges shown on posters.
 *
 * Providers are inconsistent: some send ISO codes, some send "Spanish" or
 * "Latino", and many send nothing at all. Unknown values are hidden instead of
 * guessing, so the badge never lies about a title.
 */
object LanguageTag {

    fun normalize(raw: String?): String? {
        val value = raw?.trim()?.lowercase() ?: return null
        if (value.isBlank()) return null
        if (value in setOf("audio", "original", "und", "unknown", "none", "null")) return null

        val normalized = when {
            value.contains("dual") || value.contains("multi") -> "DUAL"
            value in setOf(
                "es", "spa", "esp", "spanish", "español", "espanol",
                "castellano", "latino", "lat", "es-la", "es_la",
            ) -> "ESP"

            value in setOf(
                "en", "eng", "english", "inglés", "ingles", "en-us", "en_us",
            ) -> "ENG"

            else -> value
                .replace(Regex("[^a-z]"), "")
                .take(3)
                .uppercase()
                .takeIf { it.length >= 2 }
        }

        return normalized
    }
}
