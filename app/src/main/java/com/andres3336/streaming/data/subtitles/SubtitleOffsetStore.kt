package com.andres3336.streaming.data.subtitles

import android.content.Context
import com.andres3336.streaming.data.security.EncryptedFileStore
import org.json.JSONObject

/**
 * Per-media subtitle timing offsets. Media keys can contain credentials, so the
 * file is encrypted like the other playback stores.
 */
class SubtitleOffsetStore(context: Context) {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "subtitle-offsets.bin",
        alias = "streaming_subtitle_offsets",
    )

    fun load(): Map<String, Long> {
        val raw = store.read() ?: return emptyMap()
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
        return buildMap {
            json.keys().forEach { key ->
                val value = json.optLong(key, 0L)
                if (value != 0L) put(key, value)
            }
        }
    }

    suspend fun save(offsetByKey: Map<String, Long>) {
        val json = JSONObject()
        offsetByKey.forEach { (key, value) -> if (value != 0L) json.put(key, value) }
        if (json.length() == 0) {
            store.delete()
        } else {
            store.write(json.toString())
        }
    }
}
