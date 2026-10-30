package com.andres3336.streaming.data.search

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

enum class SearchKind(val label: String) {
    TV("TV"),
    MOVIE("Películas"),
    SERIES("Series"),
    CATEGORY("Categorías"),
}

/**
 * One row in the search catalogue.
 *
 * Deliberately lean: it carries only the identity and the few fields needed to open
 * the result later. The full movie/series metadata is fetched by id on the detail
 * screen, so 40k+ entries stay inside the TV's memory budget.
 */
data class SearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: SearchKind,
    val image: String? = null,
    // Live
    val streamUrl: String? = null,
    val streamGroup: String? = null,
    val streamTvgId: String? = null,
    val streamLanguage: String? = null,
    // Movies
    val movieId: String? = null,
    val movieExtension: String? = null,
    val movieCategoryId: String? = null,
    // Series
    val seriesId: String? = null,
    val seriesCategoryId: String? = null,
    // Categories
    /** Set on [SearchKind.CATEGORY] items: which section the category belongs to. */
    val categoryId: String? = null,
    /** `tv`, `movies` or `series`. */
    val categorySection: String? = null,
)

data class SearchResult(
    val item: SearchItem,
    val score: Int,
)

/**
 * Client-side fuzzy search for live channels, movies, series and their categories.
 *
 * Accents and ñ are folded to their base letters, so `nino` matches `niño` and
 * vice versa. The index keeps only normalized strings and scans them on a
 * debounced query: 40k+ Xtream entries must stay inside a small memory budget
 * on the TV.
 */
class SearchIndex(items: List<SearchItem>) {

    private data class Entry(
        val item: SearchItem,
        val title: String,
        val subtitle: String,
    )

    // Provider catalogues repeat entries: a duplicate id would crash the LazyColumn
    // with "Key ... was already used".
    private val searchEntries: List<Entry> = items.distinctBy { it.id }.map { item ->
        Entry(
            item = item,
            title = SearchText.normalize(item.title),
            subtitle = SearchText.normalize(item.subtitle),
        )
    }

    fun size(): Int = searchEntries.size

    fun search(
        query: String,
        kind: SearchKind? = null,
        limit: Int = 100,
    ): List<SearchResult> {
        val queryTokens = SearchText.tokens(query)
        if (queryTokens.isEmpty()) return emptyList()

        val results = ArrayList<Scored>(minOf(limit * 4, 512))
        searchEntries.forEach { entry ->
            if (kind != null && entry.item.kind != kind) return@forEach
            var matched = 0
            var score = 0
            queryTokens.forEach { token ->
                val tokenScore = match(entry, token)
                if (tokenScore > 0) {
                    matched += 1
                    score += tokenScore
                }
            }
            if (matched > 0) {
                results += Scored(entry.item, score, matched)
            }
        }

        return results
            .sortedWith(
                compareByDescending<Scored> { it.matched }
                    .thenByDescending { it.score }
                    .thenBy { it.item.title.length },
            )
            .take(limit)
            .map { SearchResult(it.item, it.score) }
    }

    private fun match(entry: Entry, token: String): Int {
        val title = entry.title
        if (title == token) return 120
        if (title.startsWith(token)) return 90
        if (title.contains(token)) return 70
        if (entry.subtitle.contains(token)) return 35

        if (token.length >= 3) {
            val maxDistance = if (token.length <= 4) 1 else 2
            val words = title.split(' ')
            if (words.any { word -> fuzzyMatch(word, token, maxDistance) }) {
                return 30
            }
        }
        return 0
    }

    private fun fuzzyMatch(word: String, token: String, maxDistance: Int): Boolean {
        if (abs(word.length - token.length) > maxDistance) return false
        return boundedDistance(token, word, maxDistance) <= maxDistance
    }

    private fun boundedDistance(a: String, b: String, max: Int): Int {
        if (abs(a.length - b.length) > max) return max + 1
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + cost,
                )
                if (current[j] < rowMin) rowMin = current[j]
            }
            if (rowMin > max) return max + 1
            val swap = previous
            previous = current
            current = swap
        }

        return previous[b.length]
    }

    private data class Scored(
        val item: SearchItem,
        val score: Int,
        val matched: Int,
    )
}

object SearchText {

    private val diacritics = Regex("\\p{Mn}+")
    private val nonAlphanumeric = Regex("[^a-z0-9]+")

    fun normalize(text: String): String = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(diacritics, "")
        .lowercase(Locale.ROOT)
        .replace(nonAlphanumeric, " ")
        .trim()

    fun tokens(text: String): List<String> = normalize(text)
        .split(' ')
        .filter(String::isNotEmpty)
}
