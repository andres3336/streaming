package com.andres3336.streaming.data.xtream

/**
 * Providers often insert divider categories/streams such as `-----`, `•••••` or
 * `=== DEPORTES ===` with no playable content. These helpers keep that noise out
 * of the live rail without preloading every Xtream category.
 */
object XtreamCategoryFilter {

    private val separatorChars = setOf(
        '-', '_', '=', '*', '#', '·', '•', '.', '~', '|', '+', '/', '\\', ':', ';', ' ',
    )

    fun isPlaceholderName(raw: String): Boolean {
        val name = raw.trim()
        if (name.isEmpty()) return true

        val letters = name.count { it.isLetterOrDigit() }
        if (letters < 2) return true

        val separators = name.count { it in separatorChars }
        return separators >= name.length - 1
    }

    fun isUsable(stream: XtreamLiveStream): Boolean =
        stream.name.isNotBlank() && !isPlaceholderName(stream.name)

    fun usableStreams(streams: List<XtreamLiveStream>): List<XtreamLiveStream> =
        streams.filter(::isUsable)

    fun isVisibleCategory(
        category: XtreamCategory,
        hiddenIds: Set<String>,
    ): Boolean = category.id !in hiddenIds && !isPlaceholderName(category.name)
}
