package com.andres3336.streaming.data.cache

import com.andres3336.streaming.data.xtream.XtreamCategory
import com.andres3336.streaming.data.xtream.XtreamLiveStream
import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamCatalogTest {

    @Test
    fun roundTripsCategoriesAndLazyStreamPayloads() {
        val catalog = XtreamCatalog(
            categories = listOf(XtreamCategory("1", "Deportes")),
            streams = mapOf(
                "1" to listOf(
                    XtreamLiveStream(
                        id = "10",
                        name = "Canal",
                        logo = "https://example.com/logo.png",
                        categoryId = "1",
                        epgChannelId = "canal.tv",
                    ),
                ),
            ),
        )

        assertEquals(catalog, XtreamCatalog.fromJson(catalog.toJson()))
    }
}
