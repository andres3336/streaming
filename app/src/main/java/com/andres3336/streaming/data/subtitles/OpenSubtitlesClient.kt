package com.andres3336.streaming.data.subtitles

import com.andres3336.streaming.BuildConfig
import com.andres3336.streaming.data.config.SubtitlesConfig
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

sealed interface SubtitleResult<out T> {
    data class Success<T>(val value: T) : SubtitleResult<T>
    data class Failure(val message: String) : SubtitleResult<Nothing>
}

/**
 * Minimal OpenSubtitles.com REST client. The API key and account live in the
 * central config; nothing is logged and downloaded files are cached locally.
 */
class OpenSubtitlesClient(
    private val config: SubtitlesConfig,
    private val client: OkHttpClient = OkHttpClient(),
    private val userAgent: String = "Streaming/${BuildConfig.VERSION_NAME}",
) {

    @Volatile
    private var token: String? = null

    suspend fun search(
        media: MediaDescriptor,
        languages: List<String>,
    ): SubtitleResult<List<SubtitleCandidate>> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext SubtitleResult.Failure(
                "OpenSubtitles no está configurado.",
            )
        }

        val url = baseUrl("subtitles").newBuilder().apply {
            addQueryParameter("query", media.title)
            addQueryParameter("languages", languages.joinToString(","))
            addQueryParameter("order_by", "download_count")
            addQueryParameter("order_direction", "desc")
            if (media.isEpisode) {
                addQueryParameter("type", "episode")
                media.season?.let { addQueryParameter("season_number", it.toString()) }
                media.episode?.let { addQueryParameter("episode_number", it.toString()) }
            } else {
                addQueryParameter("type", "movie")
                media.year?.takeIf { it.isNotBlank() }?.let { addQueryParameter("year", it) }
            }
            media.imdbId?.takeIf { it.startsWith("tt") }?.let { addQueryParameter("imdb_id", it) }
            media.tmdbId?.takeIf { it.isNotBlank() }?.let { addQueryParameter("tmdb_id", it) }
        }.build()

        runCatching {
            val body = execute(url)
            val data = JSONObject(body).optJSONArray("data") ?: return@runCatching emptyList()
            (0 until data.length()).mapNotNull { index ->
                parseCandidate(data.optJSONObject(index))
            }.sortedWith(
                compareBy<SubtitleCandidate> { languages.indexOf(it.language).takeIf { i -> i >= 0 } ?: 99 }
                    .thenByDescending { it.downloads }
                    .thenByDescending { it.rating },
            ).take(12)
        }.fold(
            onSuccess = { candidates ->
                if (candidates.isEmpty()) {
                    SubtitleResult.Failure("No se encontraron subtítulos.")
                } else {
                    SubtitleResult.Success(candidates)
                }
            },
            onFailure = {
                SubtitleResult.Failure("No se pudo buscar en OpenSubtitles.")
            },
        )
    }

    suspend fun download(candidate: SubtitleCandidate): SubtitleResult<SubtitleFile> =
        withContext(Dispatchers.IO) {
            val apiKey = config.apiKey.trim()
            if (apiKey.isEmpty()) {
                return@withContext SubtitleResult.Failure(
                    "OpenSubtitles no está configurado.",
                )
            }

            runCatching {
                ensureToken()
                val payload = JSONObject().put("file_id", candidate.fileId).toString()
                val request = Request.Builder()
                    .url(baseUrl("download"))
                    .headers(apiHeaders())
                    .apply { token?.let { header("Authorization", "Bearer $it") } }
                    .post(payload.toRequestBody(JSON_MEDIA))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use SubtitleResult.Failure(downloadError(response.code))
                    }
                    val json = JSONObject(response.body?.string().orEmpty())
                    val link = json.optString("link").takeIf(String::isNotBlank)
                        ?: return@use SubtitleResult.Failure("OpenSubtitles no devolvió el archivo.")
                    val bytes = fetchBytes(link)
                    val content = extractSubtitle(bytes)
                        ?: return@use SubtitleResult.Failure("El archivo de subtítulos no es válido.")
                    SubtitleResult.Success(
                        SubtitleFile(
                            language = candidate.language,
                            content = content,
                            candidate = candidate,
                        ),
                    )
                }
            }.getOrElse {
                SubtitleResult.Failure("No se pudo descargar el subtítulo.")
            }
        }

    private suspend fun ensureToken() {
        if (token != null) return
        if (config.username.isBlank() || config.password.isBlank()) return

        runCatching {
            val payload = JSONObject()
                .put("username", config.username)
                .put("password", config.password)
                .toString()
            val request = Request.Builder()
                .url(baseUrl("login"))
                .headers(apiHeaders())
                .post(payload.toRequestBody(JSON_MEDIA))
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string().orEmpty())
                    token = json.optString("token").takeIf(String::isNotBlank)
                }
            }
        }
    }

    private fun execute(url: HttpUrl): String {
        val request = Request.Builder()
            .url(url)
            .headers(apiHeaders())
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun fetchBytes(link: String): ByteArray {
        val request = Request.Builder()
            .url(link)
            .header("User-Agent", userAgent)
            .header("Accept", "*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    private fun extractSubtitle(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null

        if (bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()) {
            val inflated = GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            return SrtCodec.decode(inflated)
        }

        if (bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".srt", ignoreCase = true)) {
                        return SrtCodec.decode(zip.readBytes())
                    }
                    entry = zip.nextEntry
                }
            }
            return null
        }

        return SrtCodec.decode(bytes)
    }

    private fun parseCandidate(json: JSONObject?): SubtitleCandidate? {
        if (json == null) return null
        val attributes = json.optJSONObject("attributes") ?: return null
        val file = attributes.optJSONArray("files")?.optJSONObject(0) ?: return null
        val fileId = file.optString("file_id").takeIf(String::isNotBlank) ?: return null
        val language = attributes.optString("language").lowercase(Locale.ROOT)
        if (language !in setOf("es", "en")) return null

        return SubtitleCandidate(
            fileId = fileId,
            title = attributes.optJSONObject("feature_details")
                ?.optString("title")
                ?.takeIf(String::isNotBlank)
                ?: file.optString("file_name"),
            language = language,
            downloads = attributes.optInt("download_count", 0),
            rating = attributes.optDouble("ratings", 0.0),
            release = attributes.optString("release").takeIf(String::isNotBlank),
            hearingImpaired = attributes.optBoolean("hearing_impaired", false),
        )
    }

    private fun downloadError(code: Int): String = when (code) {
        401, 403 -> "OpenSubtitles requiere usuario y contraseña en la configuración."
        406 -> "Se alcanzó el límite diario de OpenSubtitles."
        429 -> "OpenSubtitles está limitando las descargas. Inténtalo más tarde."
        else -> "OpenSubtitles respondió HTTP $code."
    }

    private fun apiHeaders(): okhttp3.Headers = okhttp3.Headers.Builder()
        .add("Api-Key", config.apiKey.trim())
        .add("User-Agent", userAgent)
        .add("Accept", "application/json")
        .build()

    private fun baseUrl(action: String): HttpUrl =
        "https://api.opensubtitles.com/api/v1/$action".toHttpUrl()

    private companion object {
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
