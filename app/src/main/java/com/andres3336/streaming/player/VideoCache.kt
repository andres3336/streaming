package com.andres3336.streaming.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * One VOD-only Media3 cache per process. Live streams never use it.
 */
object VideoCache {

    private var cache: SimpleCache? = null
    private var currentSizeMb: Int = -1

    @Synchronized
    fun get(context: Context, sizeMb: Int): SimpleCache {
        if (cache != null && currentSizeMb == sizeMb) return cache!!

        cache?.release()
        cache = null

        val appContext = context.applicationContext
        val directory = File(appContext.filesDir, "video_cache").apply { mkdirs() }
        val evictor = LeastRecentlyUsedCacheEvictor(sizeMb.toLong() * 1024L * 1024L)
        val database = StandaloneDatabaseProvider(appContext)

        cache = SimpleCache(directory, evictor, database)
        currentSizeMb = sizeMb
        return cache!!
    }

    @Synchronized
    fun clear(context: Context) {
        cache?.release()
        cache = null
        currentSizeMb = -1
        File(context.applicationContext.filesDir, "video_cache").deleteRecursively()
    }
}
