package com.andres3336.streaming.data.xtream

import com.andres3336.streaming.BuildConfig
import com.andres3336.streaming.data.config.XtreamConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

sealed interface XtreamResult<out T> {
    data class Success<T>(val value: T) : XtreamResult<T>
    data class Failure(val message: String) : XtreamResult<Nothing>
}

class XtreamClient(
    private val config: XtreamConfig,
    private val client: OkHttpClient = OkHttpClient(),
    userAgent: String? = null,
) {

    private val base = config.baseUrl.trimEnd('/')
    private val userAgent = userAgent?.takeIf(String::isNotBlank)
        ?: "Streaming/${BuildConfig.VERSION_NAME}"

    suspend fun vodCategories(): XtreamResult<List<XtreamCategory>> =
        jsonArray("get_vod_categories").mapArray { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { json ->
                    XtreamCategory(
                        id = json.optString("category_id"),
                        name = json.optString("category_name").ifBlank { "Sin nombre" },
                    )
                }
            }
        }

    suspend fun vodStreams(categoryId: String): XtreamResult<List<XtreamMovie>> =
        jsonArray("get_vod_streams", mapOf("category_id" to categoryId)).mapArray { array ->
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                XtreamMovie(
                    id = json.optString("stream_id"),
                    name = json.optString("name").ifBlank { "Sin título" },
                    logo = json.optString("stream_icon").takeIf(String::isNotBlank),
                    categoryId = json.optString("category_id"),
                    extension = json.optString("container_extension").ifBlank { "mp4" },
                )
            }
        }

    suspend fun seriesCategories(): XtreamResult<List<XtreamCategory>> =
        jsonArray("get_series_categories").mapArray { array ->
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { json ->
                    XtreamCategory(
                        id = json.optString("category_id"),
                        name = json.optString("category_name").ifBlank { "Sin nombre" },
                    )
                }
            }
        }

    suspend fun series(categoryId: String): XtreamResult<List<XtreamSeries>> =
        jsonArray("get_series", mapOf("category_id" to categoryId)).mapArray { array ->
            (0 until array.length()).mapNotNull { index ->
                val json = array.optJSONObject(index) ?: return@mapNotNull null
                XtreamSeries(
                    id = json.optString("series_id"),
                    name = json.optString("name").ifBlank { "Sin título" },
                    cover = json.optString("cover").takeIf(String::isNotBlank),
                    categoryId = json.optString("category_id"),
                    plot = json.optString("plot").takeIf(String::isNotBlank),
                )
            }
        }

    suspend fun seriesInfo(seriesId: String): XtreamResult<XtreamSeriesInfo> =
        jsonObject("get_series_info", mapOf("series_id" to seriesId)).mapObject { json ->
            val episodesJson = json.optJSONObject("episodes") ?: JSONObject()
            val seasons = episodesJson.keys().asSequence().mapNotNull { key ->
                val array = episodesJson.optJSONArray(key) ?: return@mapNotNull null
                val number = key.toIntOrNull() ?: return@mapNotNull null
                val episodes = (0 until array.length()).mapNotNull { index ->
                    val episode = array.optJSONObject(index) ?: return@mapNotNull null
                    XtreamEpisode(
                        id = episode.optString("id"),
                        title = episode.optString("title").ifBlank {
                            "Episodio ${episode.optInt("episode_num", index + 1)}"
                        },
                        season = episode.optInt("season", number),
                        episode = episode.optInt("episode_num", index + 1),
                        extension = episode.optString("container_extension").ifBlank { "mp4" },
                    )
                }
                XtreamSeason(number = number, episodes = episodes)
            }.sortedBy(XtreamSeason::number).toList()

            XtreamSeriesInfo(seasons = seasons)
        }

    fun movieUrl(movie: XtreamMovie): String =
        mediaUrl("movie", movie.id, movie.extension)

    fun episodeUrl(episode: XtreamEpisode): String =
        mediaUrl("series", episode.id, episode.extension)

    private fun mediaUrl(kind: String, id: String, extension: String): String =
        base.toHttpUrl().newBuilder()
            .addPathSegment(kind)
            .addPathSegment(config.username)
            .addPathSegment(config.password)
            .addPathSegment("$id.${extension.ifBlank { "mp4" }}")
            .build()
            .toString()

    private fun apiUrl(action: String, extra: Map<String, String>): HttpUrl =
        base.toHttpUrl().newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", config.username)
            .addQueryParameter("password", config.password)
            .addQueryParameter("action", action)
            .apply { extra.forEach { (key, value) -> addQueryParameter(key, value) } }
            .build()

    private suspend fun jsonArray(
        action: String,
        extra: Map<String, String> = emptyMap(),
    ): XtreamResult<JSONArray> {
        val body = when (val result = execute(apiUrl(action, extra))) {
            is XtreamResult.Success -> result.value
            is XtreamResult.Failure -> return result
        }
        return try {
            XtreamResult.Success(JSONArray(body))
        } catch (_: Exception) {
            XtreamResult.Failure(errorMessage(body))
        }
    }

    private suspend fun jsonObject(
        action: String,
        extra: Map<String, String> = emptyMap(),
    ): XtreamResult<JSONObject> {
        val body = when (val result = execute(apiUrl(action, extra))) {
            is XtreamResult.Success -> result.value
            is XtreamResult.Failure -> return result
        }
        return try {
            XtreamResult.Success(JSONObject(body))
        } catch (_: Exception) {
            XtreamResult.Failure(errorMessage(body))
        }
    }

    private suspend fun execute(url: HttpUrl): XtreamResult<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext XtreamResult.Failure(
                            "Xtream respondió HTTP ${response.code}",
                        )
                    }
                    XtreamResult.Success(response.body?.string().orEmpty())
                }
            } catch (error: Exception) {
                XtreamResult.Failure(
                    "No se pudo conectar con Xtream: ${error.message ?: "error de red"}",
                )
            }
        }

    private fun errorMessage(body: String): String =
        runCatching { JSONObject(body).optString("message") }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
            ?: "Respuesta inválida de Xtream"

    private fun <T> XtreamResult<JSONArray>.mapArray(transform: (JSONArray) -> T): XtreamResult<T> =
        when (this) {
            is XtreamResult.Success -> XtreamResult.Success(transform(value))
            is XtreamResult.Failure -> this
        }

    private fun <T> XtreamResult<JSONObject>.mapObject(transform: (JSONObject) -> T): XtreamResult<T> =
        when (this) {
            is XtreamResult.Success -> XtreamResult.Success(transform(value))
            is XtreamResult.Failure -> this
        }
}
