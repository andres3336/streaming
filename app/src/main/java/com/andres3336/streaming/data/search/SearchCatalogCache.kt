package com.andres3336.streaming.data.search

/**
 * Session cache for the on-demand Xtream search catalogue. Building it costs
 * several large requests, so switching tabs must not reload it every time.
 */
object SearchCatalogCache {

    private var key: String? = null
    private var items: List<SearchItem> = emptyList()
    private var complete: Boolean = false

    fun get(cacheKey: String): List<SearchItem>? =
        if (key == cacheKey && items.isNotEmpty()) items else null

    fun isComplete(cacheKey: String): Boolean = key == cacheKey && complete

    fun put(cacheKey: String, items: List<SearchItem>, complete: Boolean) {
        key = cacheKey
        this.items = items
        this.complete = complete
    }

    fun clear() {
        key = null
        items = emptyList()
        complete = false
    }
}
