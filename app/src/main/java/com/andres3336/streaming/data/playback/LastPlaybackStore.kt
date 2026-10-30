package com.andres3336.streaming.data.playback

import android.content.Context
import com.andres3336.streaming.data.security.EncryptedFileStore
import org.json.JSONObject

class LastPlaybackStore(context: Context) {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "last-playback.bin",
        alias = "streaming_last_playback",
    )

    fun load(): LastPlayback? {
        val raw = store.read() ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return LastPlayback.fromJson(json)
    }

    suspend fun save(playback: LastPlayback) {
        store.write(playback.toJson().toString())
    }

    fun clear() {
        store.delete()
    }
}
