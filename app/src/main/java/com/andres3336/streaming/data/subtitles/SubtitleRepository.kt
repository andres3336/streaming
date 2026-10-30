package com.andres3336.streaming.data.subtitles

import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.SubtitlesConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubtitleRepository(
    private val config: SubtitlesConfig,
    private val cache: SubtitleCache,
    private val client: OpenSubtitlesClient,
) {

    suspend fun autoFetch(channel: Channel, offsetMs: Long = 0L): File? = withContext(Dispatchers.IO) {
        if (!config.enabled || !config.autoDownload || config.apiKey.isBlank() || !channel.isVod) {
            return@withContext null
        }

        val media = MediaDescriptor.fromChannel(channel)
        cache.findAny(media.key, config.languages, offsetMs)?.let { return@withContext it }

        for (language in config.languages) {
            val candidates = when (val result = client.search(media, listOf(language))) {
                is SubtitleResult.Success -> result.value
                is SubtitleResult.Failure -> continue
            }
            val best = candidates
                .filter { it.language == language }
                .sortedWith(
                    compareBy<SubtitleCandidate> { it.hearingImpaired }
                        .thenByDescending { it.downloads }
                        .thenByDescending { it.rating },
                )
                .firstOrNull() ?: continue

            val file = download(media, best, offsetMs)
            if (file != null) return@withContext file
        }

        null
    }

    suspend fun search(
        channel: Channel,
        languages: List<String> = config.languages,
    ): SubtitleResult<List<SubtitleCandidate>> {
        if (!config.enabled || config.apiKey.isBlank()) {
            return SubtitleResult.Failure("OpenSubtitles no está configurado.")
        }
        return client.search(MediaDescriptor.fromChannel(channel), languages)
    }

    suspend fun download(
        channel: Channel,
        candidate: SubtitleCandidate,
        offsetMs: Long = 0L,
    ): File? = download(MediaDescriptor.fromChannel(channel), candidate, offsetMs)

    suspend fun download(
        media: MediaDescriptor,
        candidate: SubtitleCandidate,
        offsetMs: Long = 0L,
    ): File? = withContext(Dispatchers.IO) {
        val result = client.download(candidate)
        if (result !is SubtitleResult.Success) return@withContext null

        val content = result.value.content
        cache.clearFor(media.key, candidate.language)
        cache.write(media.key, candidate.language, content, offsetMs = 0L)
        if (offsetMs != 0L) {
            val shifted = SrtCodec.shift(content, offsetMs)
            cache.write(media.key, candidate.language, shifted, offsetMs)
        } else {
            cache.find(media.key, candidate.language, 0L)
        }
    }

    fun applyOffset(
        mediaKey: String,
        language: String,
        offsetMs: Long,
    ): File? {
        val original = cache.find(mediaKey, language, 0L) ?: return null
        if (offsetMs == 0L) return original
        val shifted = SrtCodec.shift(original.readText(Charsets.UTF_8), offsetMs)
        return cache.write(mediaKey, language, shifted, offsetMs)
    }

    fun importLocal(
        mediaKey: String,
        language: String,
        content: String,
        offsetMs: Long,
    ): File {
        cache.clearFor(mediaKey, language)
        cache.write(mediaKey, language, content, offsetMs = 0L)
        return if (offsetMs != 0L) {
            cache.write(mediaKey, language, SrtCodec.shift(content, offsetMs), offsetMs)
        } else {
            cache.find(mediaKey, language, 0L)!!
        }
    }

    fun cached(
        mediaKey: String,
        language: String,
        offsetMs: Long,
    ): File? = cache.find(mediaKey, language, offsetMs)

    fun cachedAny(mediaKey: String, offsetMs: Long): File? =
        cache.findAny(mediaKey, config.languages, offsetMs)

    fun languageOf(file: File): String =
        config.languages.firstOrNull { file.name.contains("-$it-") } ?: "es"

    fun cache(): SubtitleCache = cache
}
