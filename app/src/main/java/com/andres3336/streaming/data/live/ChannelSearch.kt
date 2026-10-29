package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel
import java.text.Normalizer
import java.util.Locale

/**
 * Accent-insensitive channel search across the channel name and group.
 */
object ChannelSearch {

    private val diacritics = Regex("\\p{Mn}+")

    fun normalize(text: String): String = Normalizer
        .normalize(text, Normalizer.Form.NFD)
        .replace(diacritics, "")
        .lowercase(Locale.ROOT)
        .trim()

    fun matches(channel: Channel, query: String): Boolean {
        val needle = normalize(query)
        if (needle.isEmpty()) return true

        return normalize(channel.name).contains(needle) ||
            normalize(channel.group.orEmpty()).contains(needle)
    }

    fun filter(channels: List<Channel>, query: String): List<Channel> {
        val needle = normalize(query)
        if (needle.isEmpty()) return channels
        return channels.filter { normalize(it.name).contains(needle) || normalize(it.group.orEmpty()).contains(needle) }
    }
}
