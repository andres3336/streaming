package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel
import org.json.JSONArray
import org.json.JSONObject

const val RECENT_CHANNEL_LIMIT = 10

/**
 * Per-device live TV state: favorites, recent channels and the last channel.
 *
 * Channels are stored by key (tvg-id when present, otherwise the URL) so the
 * state survives playlist refreshes and reordering.
 */
data class LiveState(
    val favorites: List<String> = emptyList(),
    val recents: List<String> = emptyList(),
    val lastChannel: String? = null,
) {

    fun isFavorite(key: String): Boolean = key in favorites

    fun toggleFavorite(key: String): LiveState {
        if (key.isBlank()) return this
        return if (key in favorites) {
            copy(favorites = favorites.filterNot { it == key })
        } else {
            copy(favorites = listOf(key) + favorites)
        }
    }

    /** Records a channel as the most recent one and as the last channel. */
    fun remember(key: String): LiveState {
        if (key.isBlank()) return this
        return copy(
            recents = (listOf(key) + recents.filterNot { it == key })
                .take(RECENT_CHANNEL_LIMIT),
            lastChannel = key,
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("favorites", JSONArray(favorites))
        put("recents", JSONArray(recents))
        lastChannel?.let { put("lastChannel", it) }
    }

    companion object {

        /**
         * Resolves stored keys against the current channel list, preserving the
         * stored order and dropping channels that no longer exist.
         */
        fun resolve(keys: List<String>, channels: List<Channel>): List<Channel> {
            if (keys.isEmpty() || channels.isEmpty()) return emptyList()
            val byKey = channels.associateBy(Channel::liveKey)
            return keys.mapNotNull(byKey::get)
        }

        fun fromJson(json: JSONObject): LiveState {
            fun keys(name: String, limit: Int = Int.MAX_VALUE): List<String> {
                val array = json.optJSONArray(name) ?: return emptyList()
                return (0 until array.length())
                    .mapNotNull { index ->
                        array.optString(index).takeIf(String::isNotBlank)
                    }
                    .distinct()
                    .take(limit)
            }

            return LiveState(
                favorites = keys("favorites"),
                recents = keys("recents", RECENT_CHANNEL_LIMIT),
                lastChannel = json.optString("lastChannel").takeIf(String::isNotBlank),
            )
        }
    }
}

fun Channel.liveKey(): String = tvgId?.takeIf(String::isNotBlank) ?: url
