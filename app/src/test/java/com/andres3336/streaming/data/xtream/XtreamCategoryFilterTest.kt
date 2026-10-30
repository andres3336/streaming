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
    fun filtersDividerStreams() {
        val streams = listOf(stream("-----"), stream("Canal 1"), stream("•••"))
        val usable = XtreamCategoryFilter.usableStreams(streams)
        assertTrue(usable.size == 1)
        assertTrue(usable.single().name == "Canal 1")
    }

    @Test
    fun hidesPlaceholderAndHiddenCategories() {
        val divider = XtreamCategory(id = "1", name = "-----")
        val real = XtreamCategory(id = "2", name = "Canales")

        assertFalse(XtreamCategoryFilter.isVisibleCategory(divider, emptySet()))
        assertFalse(XtreamCategoryFilter.isVisibleCategory(real, setOf("2")))
        assertTrue(XtreamCategoryFilter.isVisibleCategory(real, emptySet()))
    }
}
