package com.andres3336.streaming.data.cache

import android.content.Context
import com.andres3336.streaming.data.security.EncryptedFileStore
import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamLiveStream
import org.json.JSONArray
import org.json.JSONObject

data class XtreamCatalog(
    val categories: List<XtreamCategory> = emptyList(),
    val streams: Map<String, List<XtreamLiveStream>> = emptyMap(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("categories", JSONArray().apply {
            categories.forEach { category ->
                put(
                    JSONObject()
                        .put("id", category.id)
                        .put("name", category.name),
                )
            }
        })
        put("streams", JSONObject().apply {
            streams.forEach { (categoryId, list) ->
                put(
                    categoryId,
                    JSONArray().apply {
                        list.forEach { stream ->
                            put(
                                JSONObject()
                                    .put("id", stream.id)
                                    .put("name", stream.name)
                                    .put("categoryId", stream.categoryId)
                                    .apply {
                                        stream.logo?.let { put("logo", it) }
                                    },
                            )
                        }
                    },
                )
            }
        })
    }

    companion object {
        fun fromJson(json: JSONObject): XtreamCatalog {
            val categoriesJson = json.optJSONArray("categories") ?: JSONArray()
            val categories = (0 until categoriesJson.length()).mapNotNull { index ->
                categoriesJson.optJSONObject(index)?.let { item ->
                    XtreamCategory(
                        id = item.optString("id"),
                        name = item.optString("name").ifBlank { "Sin nombre" },
                    )
                }
            }

            val streamsJson = json.optJSONObject("streams") ?: JSONObject()
            val streams = streamsJson.keys().asSequence().associateWith { categoryId ->
                val array = streamsJson.optJSONArray(categoryId) ?: JSONArray()
                (0 until array.length()).mapNotNull { index ->
                    array.optJSONObject(index)?.let { item ->
                        XtreamLiveStream(
                            id = item.optString("id"),
                            name = item.optString("name").ifBlank { "Sin título" },
                            logo = item.optString("logo").takeIf(String::isNotBlank),
                            categoryId = item.optString("categoryId").ifBlank { categoryId },
                        )
                    }
                }
            }

            return XtreamCatalog(categories = categories, streams = streams)
        }
    }
}

class XtreamCatalogCache(context: Context) {

    private val store = EncryptedFileStore(
        context = context,
        fileName = "xtream-cache.bin",
        alias = "streaming_xtream_cache",
    )

    fun load(sourceKey: String): XtreamCatalog? {
        val raw = store.read() ?: return null
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        if (json.optString("sourceKey") != sourceKey) {
            store.delete()
            return null
        }

        return XtreamCatalog.fromJson(json)
    }

    suspend fun save(sourceKey: String, catalog: XtreamCatalog) {
        val payload = catalog.toJson().put("sourceKey", sourceKey)
        store.write(payload.toString())
    }

    fun clear() {
        store.delete()
    }
}
