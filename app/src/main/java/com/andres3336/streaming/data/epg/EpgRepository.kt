package com.andres3336.streaming.data.epg

import com.andres3336.streaming.data.config.EpgConfig
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Loads XMLTV only when configured and when the cache is older than
 * `epg.refreshHours`. A failed download always falls back to the cached guide.
 */
class EpgRepository(
    private val client: OkHttpClient,
) {

    suspend fun load(
        config: EpgConfig,
        sourceKey: String,
        store: EpgStore,
        nowMs: Long = System.currentTimeMillis(),
    ): EpgGuide? {
        val url = config.url?.takeIf(String::isNotBlank) ?: return null
        val cached = store.load(sourceKey)
        val maxAgeMs = config.refreshHours.coerceAtLeast(1) * 60L * 60L * 1000L
        if (cached != null && nowMs - cached.fetchedAt in 0 until maxAgeMs) {
            return cached.guide.window(nowMs)
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/xml, application/gzip, */*")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use cached?.guide?.window(nowMs)
                    }
                    val bytes = response.body?.bytes()
                        ?: return@use cached?.guide?.window(nowMs)

                    val stream = if (bytes.size >= 2 &&
                        bytes[0] == 0x1f.toByte() &&
                        bytes[1] == 0x8b.toByte()
                    ) {
                        GZIPInputStream(ByteArrayInputStream(bytes))
                    } else {
                        ByteArrayInputStream(bytes)
                    }

                    val guide = stream.use { XmltvParser.parse(it, nowMs) }.window(nowMs)
                    store.save(sourceKey, EpgCacheEntry(fetchedAt = nowMs, guide = guide))
                    guide
                }
            } catch (_: Exception) {
                cached?.guide?.window(nowMs)
            }
        }
    }
}
