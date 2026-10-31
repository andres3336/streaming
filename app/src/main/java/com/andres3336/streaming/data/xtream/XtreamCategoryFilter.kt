package com.andres3336.streaming.data.xtream

import java.text.Normalizer
import java.util.Locale

/**
 * Providers often insert divider categories/streams such as `-----`, `•••••` or
 * `▆▆▆ DEPORTES ▆▆▆` with no playable content. These helpers keep that noise out
 * of the live rail without preloading every Xtream category.
 *
 * The remote config can also hide whole categories by name
 * (`sources.xtream.hideCategories`). Matching is a substring test on a normalized
 * name: accents, case, punctuation and emoji are ignored, so `Costa Rica` hides
 * `🇨🇷 Costa Rica` and `24/7` hides both `📼 24 7 Cinema` and `▆▆▆２４／７▆▆▆`.
 */
object XtreamCategoryFilter {

    private val separatorChars = setOf(
        '-', '_', '=', '*', '#', '·', '•', '.', '~', '|', '+', '/', '\\', ':', ';', ' ',
    )

    /** Graphic blocks providers use to wrap section dividers. */
    private val decorativeChars = setOf(
        '▆', '▁', '▂', '▃', '▄', '▅', '▇', '█',
        '▓', '▒', '░', '■', '□', '▪', '▫', '●', '○', '◆', '◇',
        '★', '☆', '▲', '▼', '◄', '►',
    )

    private val diacritics = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")

    fun isPlaceholderName(raw: String): Boolean {
        val name = raw.trim()
        if (name.isEmpty()) return true

        val letters = name.count { it.isLetterOrDigit() }
        if (letters < 2) return true

        val separators = name.count { it in separatorChars }
        return separators >= name.length - 1
    }

    /**
     * `▆▆▆ＤＥＰＯＲＴＥＳ▆▆▆` is a section divider, not a real category. Wrapping
     * with graphic blocks on both ends is the provider's divider convention; a
     * single decorative prefix (`📺 Entretenimiento`) stays visible.
     */
    fun isDividerName(raw: String): Boolean {
        val name = raw.trim()
        if (name.length < 3) return false
        return name.first() in decorativeChars && name.last() in decorativeChars
    }

    fun isUsable(stream: XtreamLiveStream): Boolean =
        stream.name.isNotBlank() && !isPlaceholderName(stream.name)

    fun usableStreams(streams: List<XtreamLiveStream>): List<XtreamLiveStream> =
        streams.filter(::isUsable)

    fun isVisibleCategory(
        category: XtreamCategory,
        hiddenIds: Set<String>,
        hidePatterns: List<String> = emptyList(),
    ): Boolean = category.id !in hiddenIds &&
        !isPlaceholderName(category.name) &&
        !isDividerName(category.name) &&
        !isHiddenByConfig(category.name, hidePatterns)

    /** Case/accent/emoji-insensitive substring match against the remote filters. */
    fun isHiddenByConfig(name: String, patterns: List<String>): Boolean {
        if (patterns.isEmpty()) return false
        val normalized = normalizeName(name)
        if (normalized.isEmpty()) return false
        return patterns.any { pattern ->
            val needle = normalizeName(pattern)
            needle.isNotEmpty() && normalized.contains(needle)
        }
    }

    /**
     * NFKD folds fullwidth characters (`ＣＯＳＴＡ` → `COSTA`), diacritics are
     * removed (`España` → `espana`) and every symbol group becomes one space, so
     * emoji never have to be copied into the config.
     */
    fun normalizeName(raw: String): String = Normalizer
        .normalize(raw, Normalizer.Form.NFKD)
        .replace(diacritics, "")
        .lowercase(Locale.ROOT)
        .replace(nonAlphanumeric, " ")
        .trim()
}
