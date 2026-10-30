package com.andres3336.streaming.data.epg

import android.content.Context
import com.andres3336.streaming.data.security.EncryptedFileStore
import org.json.JSONObject

data class EpgCacheEntry(
    val fetchedAt: Long,
    val guide: EpgGuide,
)

interface EpgStore {
    fun load(sourceKey: String): EpgCacheEntry?
    suspend fun save(sourceKey: String, entry: EpgCacheEntry)
    fun clear()
}

/** Encrypted, source-keyed EPG cache. Channel names/IDs can contain provider data. */
class EpgCache(context: Context) : EpgStore {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "epg-cache.bin",
        alias = "streaming_epg_cache",
    )

    override fun load(sourceKey: String): EpgCacheEntry? {
        val raw = store.read() ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (json.optString("sourceKey") != sourceKey) {
            store.delete()
            return null
        }

        return EpgCacheEntry(
            fetchedAt = json.optLong("fetchedAt", 0L),
            guide = EpgGuide.fromJson(json.optJSONObject("guide") ?: JSONObject()),
        )
    }

    override suspend fun save(sourceKey: String, entry: EpgCacheEntry) {
        val payload = JSONObject().apply {
            put("sourceKey", sourceKey)
            put("fetchedAt", entry.fetchedAt)
            put("guide", entry.guide.toJson())
        }
        store.write(payload.toString())
    }

    override fun clear() {
        store.delete()
    }
}
