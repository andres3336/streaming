package com.andres3336.streaming.data.search

import android.content.Context
import com.andres3336.streaming.data.security.EncryptedFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Persistent search catalogue.
 *
 * The network build (live streams + VOD + series) takes several large requests, so
 * the lean result list is kept in an encrypted, gzipped file. A cold start loads it
 * instead of downloading the catalogue again; it is rebuilt after [MAX_AGE_MS] or
 * whenever the source hash changes (config refresh).
 */
class SearchCatalogStore(context: Context) {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "search-catalog.bin",
        alias = "streaming_search_catalog",
    )

    suspend fun load(sourceKey: String): List<SearchItem>? = withContext(Dispatchers.IO) {
        val bytes = store.readBytes() ?: return@withContext null
        runCatching {
            BufferedReader(InputStreamReader(GZIPInputStream(BufferedInputStream(bytes.inputStream())), Charsets.UTF_8))
                .use { reader ->
                    if (reader.readLine() != HEADER) return@use null
                    if (reader.readLine() != sourceKey) return@use null
                    val savedAt = reader.readLine()?.toLongOrNull() ?: 0L
                    if (System.currentTimeMillis() - savedAt > MAX_AGE_MS) return@use null
                    val expected = reader.readLine()?.toIntOrNull() ?: 0
                    val items = ArrayList<SearchItem>(expected.coerceAtLeast(0))
                    while (true) {
                        val line = reader.readLine() ?: break
                        decode(line)?.let(items::add)
                    }
                    items.takeIf { it.isNotEmpty() }
                }
        }.getOrNull()
    }

    suspend fun save(sourceKey: String, items: List<SearchItem>) = withContext(Dispatchers.IO) {
        runCatching {
            val buffer = ByteArrayOutputStream()
            GZIPOutputStream(buffer).bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(HEADER)
                writer.newLine()
                writer.write(sourceKey)
                writer.newLine()
                writer.write(System.currentTimeMillis().toString())
                writer.newLine()
                writer.write(items.size.toString())
                writer.newLine()
                items.forEach { item ->
                    writer.write(encode(item))
                    writer.newLine()
                }
            }
            store.writeBytes(buffer.toByteArray())
        }
    }

    private fun encode(item: SearchItem): String = listOf(
        item.kind.name,
        item.id,
        item.title,
        item.subtitle,
        item.image,
        item.streamUrl,
        item.streamGroup,
        item.streamTvgId,
        item.streamLanguage,
        item.movieId,
        item.movieExtension,
        item.movieCategoryId,
        item.seriesId,
        item.seriesCategoryId,
        item.categoryId,
        item.categorySection,
    ).joinToString(SEPARATOR) { escape(it.orEmpty()) }

    private fun decode(line: String): SearchItem? {
        val fields = line.split(SEPARATOR)
        if (fields.size < 16) return null
        val kind = runCatching { SearchKind.valueOf(fields[0]) }.getOrNull() ?: return null
        fun at(index: Int): String? = fields[index].let(::unescape).takeIf(String::isNotEmpty)
        return SearchItem(
            id = fields[1],
            title = unescape(fields[2]),
            subtitle = unescape(fields[3]),
            kind = kind,
            image = at(4),
            streamUrl = at(5),
            streamGroup = at(6),
            streamTvgId = at(7),
            streamLanguage = at(8),
            movieId = at(9),
            movieExtension = at(10),
            movieCategoryId = at(11),
            seriesId = at(12),
            seriesCategoryId = at(13),
            categoryId = at(14),
            categorySection = at(15),
        )
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "")

    private fun unescape(value: String): String {
        if ('\\' !in value) return value
        val out = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    't' -> out.append('\t')
                    'n' -> out.append('\n')
                    '\\' -> out.append('\\')
                    else -> out.append(value[index + 1])
                }
                index += 2
            } else {
                out.append(char)
                index += 1
            }
        }
        return out.toString()
    }

    private companion object {
        const val HEADER = "search-catalog-v1"
        const val SEPARATOR = "\t"
        const val MAX_AGE_MS = 24L * 60L * 60L * 1000L
    }
}
