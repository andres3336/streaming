package com.andres3336.streaming.data.subtitles

import android.content.Context
import com.andres3336.streaming.data.cache.CacheIdentity
import java.io.File

/**
 * Local subtitle cache. Files are keyed by a non-reversible media identity so
 * Xtream URLs and credentials never appear as file names.
 */
class SubtitleCache(context: Context) {

    private val dir = File(context.applicationContext.filesDir, "subtitles")

    fun write(
        mediaKey: String,
        language: String,
        content: String,
        offsetMs: Long = 0L,
    ): File {
        dir.mkdirs()
        val file = fileFor(mediaKey, language, offsetMs)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    fun find(mediaKey: String, language: String, offsetMs: Long = 0L): File? =
        fileFor(mediaKey, language, offsetMs).takeIf { it.exists() }

    fun findAny(
        mediaKey: String,
        languages: List<String>,
        offsetMs: Long = 0L,
    ): File? = languages.firstNotNullOfOrNull { language ->
        find(mediaKey, language, offsetMs)
    }

    fun clearFor(mediaKey: String, language: String) {
        if (!dir.exists()) return
        val prefix = CacheIdentity.sha256("$mediaKey|$language").take(24)
        dir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("$prefix-") }
            ?.forEach(File::delete)
    }

    fun clear() {
        dir.deleteRecursively()
    }

    fun sizeBytes(): Long = dir.walkTopDown()
        .filter(File::isFile)
        .sumOf(File::length)

    private fun fileFor(mediaKey: String, language: String, offsetMs: Long): File {
        val hash = CacheIdentity.sha256("$mediaKey|$language").take(24)
        return File(dir, "$hash-$language-$offsetMs.srt")
    }
}
