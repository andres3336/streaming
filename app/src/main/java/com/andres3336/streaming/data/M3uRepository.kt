package com.andres3336.streaming.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface M3uFetch {
    data class Success(val channels: List<Channel>) : M3uFetch
    data class Failure(val message: String) : M3uFetch
}

class M3uRepository(
    private val client: OkHttpClient = OkHttpClient(),
) {

    suspend fun load(
        url: String,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): M3uFetch = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .apply {
                userAgent?.takeIf(String::isNotBlank)?.let { header("User-Agent", it) }
                headers.forEach { (key, value) ->
                    if (key.isNotBlank()) header(key, value)
                }
            }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext M3uFetch.Failure(
                        "No se pudo cargar la lista de canales (HTTP ${response.code})",
                    )
                }

                val body = response.body?.string().orEmpty()
                val channels = M3uParser.parse(body)
                if (channels.isEmpty()) {
                    M3uFetch.Failure("La lista de canales está vacía")
                } else {
                    M3uFetch.Success(channels)
                }
            }
        } catch (error: Exception) {
            M3uFetch.Failure(
                "No se pudo cargar la lista de canales: ${error.message ?: "error de red"}",
            )
        }
    }
}
