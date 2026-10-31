package com.andres3336.streaming.data.media

import java.text.Normalizer
import java.util.Locale

/**
 * Language/variant tags that Xtream providers prepend to a title: `SU-`, `CA-`,
 * `ES-`, `EN-`, `LA-`. The app shows the three-letter form and groups the variants
 * of the same title so the grid does not repeat them.
 */
object TitleVariants {

    const val VO = "VO"

    // Providers write both `SU-` and `Sub `; `Cas` is Castellano, `CA-` is CAM.
    private val tagMap = mapOf(
        "su" to "SUB",
        "sub" to "SUB",
        "ca" to "CAM",
        "es" to "ESP",
        "esp" to "ESP",
        "spa" to "ESP",
        "cas" to "ESP",
        "en" to "ENG",
        "eng" to "ENG",
        "la" to "LAT",
        "lat" to "LAT",
        "vo" to VO,
    )

    private val tagPattern = Regex("^([A-Za-z]{2,3})\\s*-\\s*(.+)$")
    private val spaceTagPattern = Regex("^([A-Za-z]{3})\\s+(.+)$")
    private val diacritics = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    data class Parsed(val tag: String, val baseTitle: String)

    /** `SU- Velocidad y Deseo` -> `SUB` + `Velocidad y Deseo`. */
    fun parse(raw: String): Parsed {
        val title = raw.trim()
        for (pattern in listOf(tagPattern, spaceTagPattern)) {
            val match = pattern.find(title) ?: continue
            val tag = tagMap[match.groupValues[1].lowercase(Locale.ROOT)] ?: continue
            val rest = match.groupValues[2].trim()
            if (rest.isNotEmpty()) return Parsed(tag, rest)
        }
        return Parsed(VO, title)
    }

    /** Stable key for grouping the same title across its language variants. */
    fun groupKey(name: String, year: String?): String {
        val base = normalize(parse(name).baseTitle)
        val cleanYear = year?.trim()?.take(4)
            ?.takeIf { it.length == 4 && it.all(Char::isDigit) }
        return if (cleanYear != null) "$base|$cleanYear" else base
    }

    /**
     * Display order for the variant chips: Spanish first (`ESP`, then `LAT`), SUB
     * next, then the other variants, and English last. The first variant in this
     * order is also the one the detail opens on.
     */
    fun priority(tag: String): Int = when (tag) {
        "ESP" -> 0
        "LAT" -> 1
        "SUB" -> 2
        "CAM" -> 3
        "VO" -> 4
        "ENG" -> 5
        else -> 3
    }

    fun normalize(text: String): String = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(diacritics, "")
        .lowercase(Locale.ROOT)
        .replace(nonAlphanumeric, " ")
        .trim()
}
