package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel
import org.json.JSONObject

data class PlaybackEntry(
    val url: String,
    val name: String,
    val logo: String? = null,
    val group: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis(),
) {

    val isResumable: Boolean
        get() = positionMs > 10_000L &&
            durationMs > 0L &&
            positionMs < durationMs - 30_000L

    fun toChannel(): Channel = Channel(
        name = name,
        url = url,
        group = group,
        logo = logo,
        isVod = true,
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("url", url)
        put("name", name)
        logo?.let { put("logo", it) }
        group?.let { put("group", it) }
        put("positionMs", positionMs)
        put("durationMs", durationMs)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): PlaybackEntry? {
            val url = json.optString("url").takeIf(String::isNotBlank) ?: return null
            val name = json.optString("name").takeIf(String::isNotBlank) ?: return null
            val position = json.optLong("positionMs", 0L)
            val duration = json.optLong("durationMs", 0L)
            if (position <= 0L || duration <= 0L) return null

            return PlaybackEntry(
                url = url,
                name = name,
                logo = json.optString("logo").takeIf(String::isNotBlank),
                group = json.optString("group").takeIf(String::isNotBlank),
                positionMs = position,
                durationMs = duration,
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            )
        }

        fun fromProgress(
            channel: Channel,
            positionMs: Long,
            durationMs: Long,
        ): PlaybackEntry? {
            if (!channel.isVod || positionMs <= 0L || durationMs <= 0L) return null

            return PlaybackEntry(
                url = channel.url,
                name = channel.name,
                logo = channel.logo,
                group = channel.group,
                positionMs = positionMs,
                durationMs = durationMs,
            )
        }
    }
}
