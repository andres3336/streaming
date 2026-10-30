package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel

/**
 * Merge Xtream live channels into the curated gist list without duplicates.
 * The curated list wins; Xtream entries are matched by accent-insensitive name.
 */
fun mergeLiveChannels(
    gist: List<Channel>,
    xtream: List<Channel>,
): List<Channel> {
    if (gist.isEmpty()) return xtream
    if (xtream.isEmpty()) return gist

    val gistNames = gist.map { ChannelSearch.normalize(it.name) }.toSet()
    return gist + xtream.filter { ChannelSearch.normalize(it.name) !in gistNames }
}
