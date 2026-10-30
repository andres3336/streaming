package com.andres3336.streaming.data.cache

import android.content.Context
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.decodeChannels
import com.andres3336.streaming.data.encodeChannels
import com.andres3336.streaming.data.security.EncryptedFileStore
import org.json.JSONObject

/**
 * Last successful M3U channel list, encrypted because stream URLs can contain
 * playlist tokens. The source identity prevents using a stale list after the
 * playlist URL or headers change.
 */
class ChannelCache(context: Context) {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "channel-cache.bin",
        alias = "streaming_channel_cache",
    )

    fun load(sourceKey: String): List<Channel>? {
        val raw = store.read() ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        if (json.optString("sourceKey") != sourceKey) {
            store.delete()
            return null
        }

        return decodeChannels(json.optJSONArray("channels")).takeIf(List<Channel>::isNotEmpty)
    }

    suspend fun save(sourceKey: String, channels: List<Channel>) {
        val payload = JSONObject().apply {
            put("sourceKey", sourceKey)
            put("channels", encodeChannels(channels))
        }
        store.write(payload.toString())
    }

    fun clear() {
        store.delete()
    }
}
