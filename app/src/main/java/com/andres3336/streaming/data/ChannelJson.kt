package com.andres3336.streaming.data

import org.json.JSONArray
import org.json.JSONObject

fun Channel.toJson(): JSONObject = JSONObject().apply {
    put("name", name)
    put("url", url)
    group?.let { put("group", it) }
    logo?.let { put("logo", it) }
    tvgId?.let { put("tvgId", it) }
    userAgent?.let { put("userAgent", it) }
    referer?.let { put("referer", it) }
    put("isVod", isVod)
    fallbackUrl?.let { put("fallbackUrl", it) }
    provider?.let { put("provider", it) }
    year?.let { put("year", it) }
    season?.let { put("season", it) }
    episode?.let { put("episode", it) }
    seriesTitle?.let { put("seriesTitle", it) }
    imdbId?.let { put("imdbId", it) }
    tmdbId?.let { put("tmdbId", it) }
}

fun channelFromJson(json: JSONObject): Channel? {
    val name = json.optString("name").takeIf(String::isNotBlank) ?: return null
    val url = json.optString("url").takeIf(String::isNotBlank) ?: return null

    return Channel(
        name = name,
        url = url,
        group = json.optString("group").takeIf(String::isNotBlank),
        logo = json.optString("logo").takeIf(String::isNotBlank),
        tvgId = json.optString("tvgId").takeIf(String::isNotBlank),
        userAgent = json.optString("userAgent").takeIf(String::isNotBlank),
        referer = json.optString("referer").takeIf(String::isNotBlank),
        isVod = json.optBoolean("isVod", false),
        fallbackUrl = json.optString("fallbackUrl").takeIf(String::isNotBlank),
        provider = json.optString("provider").takeIf(String::isNotBlank),
        year = json.optString("year").takeIf(String::isNotBlank),
        season = json.optInt("season", 0).takeIf { it > 0 },
        episode = json.optInt("episode", 0).takeIf { it > 0 },
        seriesTitle = json.optString("seriesTitle").takeIf(String::isNotBlank),
        imdbId = json.optString("imdbId").takeIf(String::isNotBlank),
        tmdbId = json.optString("tmdbId").takeIf(String::isNotBlank),
    )
}

fun encodeChannels(channels: List<Channel>): JSONArray = JSONArray().apply {
    channels.forEach { put(it.toJson()) }
}

fun decodeChannels(array: JSONArray?): List<Channel> {
    if (array == null) return emptyList()
    return (0 until array.length())
        .mapNotNull { index -> array.optJSONObject(index)?.let(::channelFromJson) }
}
