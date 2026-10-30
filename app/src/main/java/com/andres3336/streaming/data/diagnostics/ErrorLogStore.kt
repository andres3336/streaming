package com.andres3336.streaming.data.diagnostics

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ErrorLogEntry(
    val timestamp: Long,
    val channel: String,
    val code: String,
    val message: String,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestamp", timestamp)
        put("channel", channel)
        put("code", code)
        put("message", message)
    }

    companion object {
        fun fromJson(json: JSONObject): ErrorLogEntry? {
            val channel = json.optString("channel").takeIf(String::isNotBlank) ?: return null
            val message = json.optString("message").takeIf(String::isNotBlank) ?: return null
            return ErrorLogEntry(
                timestamp = json.optLong("timestamp", 0L),
                channel = channel,
                code = json.optString("code").takeIf(String::isNotBlank).orEmpty(),
                message = message,
            )
        }
    }
}

/**
 * Bounded, sanitized error history. Only the channel name, Media3 error code
 * and the Spanish user-facing message are stored; URLs and credentials never
 * reach this file.
 */
class ErrorLogStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "error-log.json")
    private val lock = Any()

    fun load(): List<ErrorLogEntry> = synchronized(lock) {
        runCatching {
            if (!file.exists()) return emptyList()
            val array = JSONArray(file.readText())
            (0 until array.length())
                .mapNotNull { index -> array.optJSONObject(index)?.let(ErrorLogEntry::fromJson) }
                .takeLast(MAX_ENTRIES)
        }.getOrDefault(emptyList())
    }

    suspend fun record(entry: ErrorLogEntry) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            runCatching {
                val existing = load()
                val previous = existing.lastOrNull()
                if (previous != null &&
                    previous.channel == entry.channel &&
                    previous.code == entry.code &&
                    previous.message == entry.message
                ) {
                    return@runCatching
                }

                val entries = existing.takeLast(MAX_ENTRIES - 1) + entry
                val array = JSONArray().apply { entries.forEach { put(it.toJson()) } }
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(array.toString())
                if (!temp.renameTo(file)) {
                    file.writeText(temp.readText())
                    temp.delete()
                }
            }
        }
    }

    fun clear() = synchronized(lock) {
        file.delete()
    }

    private companion object {
        const val MAX_ENTRIES = 20
    }
}
