package com.andres3336.streaming.data.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface ConfigFetch {
    data object MissingUrl : ConfigFetch
    data object NotModified : ConfigFetch
    data class Success(val config: AppConfig, val etag: String?) : ConfigFetch
    data class Failure(val message: String) : ConfigFetch
}

class ConfigRepository(
    private val configUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
) {

    suspend fun fetch(etag: String? = null): ConfigFetch = withContext(Dispatchers.IO) {
        if (configUrl.isBlank()) return@withContext ConfigFetch.MissingUrl

        val request = Request.Builder()
            .url(configUrl)
            .header("Accept", "application/json")
            .apply { if (!etag.isNullOrBlank()) header("If-None-Match", etag) }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 304 -> ConfigFetch.NotModified
                    !response.isSuccessful -> ConfigFetch.Failure(
                        "No se pudo cargar la configuración (HTTP ${response.code})",
                    )

                    else -> {
                        val body = response.body?.string().orEmpty()
                        when (val parsed = ConfigParser.parse(body)) {
                            is ConfigResult.Valid -> ConfigFetch.Success(
                                config = parsed.config,
                                etag = response.header("ETag"),
                            )

                            is ConfigResult.Invalid -> ConfigFetch.Failure(
                                parsed.errors.joinToString("\n"),
                            )
                        }
                    }
                }
            }
        } catch (error: Exception) {
            ConfigFetch.Failure(
                "No se pudo conectar con la configuración: ${error.message ?: "error de red"}",
            )
        }
    }
}
