package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel
import org.json.JSONObject

/**
 * The last media started by the user. It is used by the boot/wake auto-start
 * path to restore playback. Encrypted because the URL can carry credentials.
 */
data class LastPlayback(
    val isLive: Boolean,
    val key: String,
    val url: String,
    val name: String,
    val logo: String? = null,
    val group: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {

    fun toChannel(): Channel = Channel(
        name = name,
        url = url,
        group = group,
        logo = logo,
        tvgId = key.takeIf { !it.startsWith("http", ignoreCase = true) },
        isVod = !isLive,
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("isLive", isLive)
        put("key", key)
        put("url", url)
        put("name", name)
        logo?.let { put("logo", it) }
        group?.let { put("group", it) }
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromChannel(channel: Channel): LastPlayback = LastPlayback(
            isLive = !channel.isVod,
            key = channel.tvgId?.takeIf(String::isNotBlank) ?: channel.url,
            url = channel.url,
            name = channel.name,
            logo = channel.logo,
            group = channel.group,
        )

        fun fromJson(json: JSONObject): LastPlayback? {
            val url = json.optString("url").takeIf(String::isNotBlank) ?: return null
            val name = json.optString("name").takeIf(String::isNotBlank) ?: return null
            return LastPlayback(
                isLive = json.optBoolean("isLive", false),
                key = json.optString("key").takeIf(String::isNotBlank) ?: url,
                url = url,
                name = name,
                logo = json.optString("logo").takeIf(String::isNotBlank),
                group = json.optString("group").takeIf(String::isNotBlank),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            )
        }
    }
}
