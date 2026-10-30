package com.andres3336.streaming.data.media

/**
 * Namespaced favorite keys stored next to the live channel favorites.
 *
 * Live channels keep their existing raw `tvg-id`/URL keys so the encrypted
 * state migrates without touching the user's data. VOD entries use a prefix to
 * avoid collisions with channel ids.
 */
object FavoriteKeys {

    fun movie(id: String): String = "movie:$id"

    fun series(id: String): String = "series:$id"
}
