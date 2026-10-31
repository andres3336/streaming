package com.andres3336.streaming.data.xtream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamCategoryFilterTest {

    private fun stream(name: String) = XtreamLiveStream(
        id = "1",
        name = name,
        logo = null,
        categoryId = "c",
    )

    @Test
    fun hidesDividerNames() {
        assertTrue(XtreamCategoryFilter.isPlaceholderName("-----"))
        assertTrue(XtreamCategoryFilter.isPlaceholderName("•••••"))
        assertTrue(XtreamCategoryFilter.isPlaceholderName("=== ==="))
        assertTrue(XtreamCategoryFilter.isPlaceholderName("   "))
        assertFalse(XtreamCategoryFilter.isPlaceholderName("Canales"))
        assertFalse(XtreamCategoryFilter.isPlaceholderName("-----España-----"))
    }

    @Test
    fun hidesBlockWrappedSectionDividers() {
        assertTrue(XtreamCategoryFilter.isDividerName("▆▆▆ＤＥＰＯＲＴＥＳ ▆▆▆"))
        assertTrue(XtreamCategoryFilter.isDividerName("▆▆▆２４／７▆▆▆"))
        assertTrue(XtreamCategoryFilter.isDividerName("▆▆▆ＰＰＶ　ＥＶＥＮＴＳ▆▆▆"))
        assertFalse(XtreamCategoryFilter.isDividerName("📺 Entretenimiento"))
        assertFalse(XtreamCategoryFilter.isDividerName("🏈 Deportes"))
        assertFalse(XtreamCategoryFilter.isDividerName("-----España-----"))
    }

    @Test
    fun filtersDividerStreams() {
        val streams = listOf(stream("-----"), stream("Canal 1"), stream("•••"))
        val usable = XtreamCategoryFilter.usableStreams(streams)
        assertTrue(usable.size == 1)
        assertTrue(usable.single().name == "Canal 1")
    }

    @Test
    fun hidesPlaceholderAndHiddenCategories() {
        val divider = XtreamCategory(id = "1", name = "-----")
        val blockDivider = XtreamCategory(id = "3", name = "▆▆▆ＤＥＰＯＲＴＥＳ ▆▆▆")
        val real = XtreamCategory(id = "2", name = "Canales")

        assertFalse(XtreamCategoryFilter.isVisibleCategory(divider, emptySet()))
        assertFalse(XtreamCategoryFilter.isVisibleCategory(blockDivider, emptySet()))
        assertFalse(XtreamCategoryFilter.isVisibleCategory(real, setOf("2")))
        assertTrue(XtreamCategoryFilter.isVisibleCategory(real, emptySet()))
    }

    @Test
    fun remoteFilterIgnoresEmojiAccentsCaseAndPunctuation() {
        assertTrue(XtreamCategoryFilter.isHiddenByConfig("🇨🇷 Costa Rica", listOf("Costa Rica")))
        assertTrue(XtreamCategoryFilter.isHiddenByConfig("🔞 XXX Adult", listOf("xxx")))
        assertTrue(XtreamCategoryFilter.isHiddenByConfig("📼 24 7 Cinema", listOf("24/7")))
        assertTrue(XtreamCategoryFilter.isHiddenByConfig("▆▆▆２４／７▆▆▆", listOf("24/7")))
        assertTrue(XtreamCategoryFilter.isHiddenByConfig("🏈 Deportes Premium", listOf("DEPORTES")))
        assertFalse(XtreamCategoryFilter.isHiddenByConfig("🇵🇷 Puerto Rico", listOf("Costa Rica")))
        assertFalse(XtreamCategoryFilter.isHiddenByConfig("🏈 Deportes", emptyList()))
    }

    @Test
    fun remoteFilterDropsSymbolOnlyEntries() {
        assertFalse(XtreamCategoryFilter.isHiddenByConfig("🏈 Deportes", listOf("▆", "  ")))
        assertTrue(
            XtreamCategoryFilter.isVisibleCategory(
                XtreamCategory(id = "1", name = "🏈 Deportes"),
                emptySet(),
                listOf("▆"),
            ),
        )
    }
}
