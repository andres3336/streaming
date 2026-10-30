package com.andres3336.streaming.data.search

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchTest {

    private val index = SearchIndex(
        listOf(
            SearchItem("1", "Niño rico", "", SearchKind.MOVIE),
            SearchItem("2", "Películas de acción", "", SearchKind.MOVIE),
            SearchItem("3", "Canal Deportes", "Deportes", SearchKind.TV),
            SearchItem("4", "Fútbol en vivo", "", SearchKind.TV),
        ),
    )

    @Test
    fun ignoresAccentsAndNtilde() {
        assertTrue(index.search("nino").any { it.item.id == "1" })
        assertTrue(index.search("niño").any { it.item.id == "1" })
        assertTrue(index.search("peliculas").any { it.item.id == "2" })
        assertTrue(index.search("futbol").any { it.item.id == "4" })
    }

    @Test
    fun toleratesTypos() {
        assertTrue(index.search("deportez").any { it.item.id == "3" })
        assertTrue(index.search("peliculaz").any { it.item.id == "2" })
    }

    @Test
    fun filtersByKind() {
        val live = index.search("deportes", SearchKind.TV)
        assertEquals(listOf("3"), live.map { it.item.id })
        assertTrue(index.search("deportes", SearchKind.MOVIE).isEmpty())
    }

    @Test
    fun normalizesPunctuation() {
        assertEquals("canal deportes hd", SearchText.normalize("Canal  Deportes-HD!"))
    }

    @Test
    fun normalizesNtildeToN() {
        assertEquals("nino", SearchText.normalize("ñiño"))
    }

    @Test
    fun keepsResultsStableForBlankQuery() {
        assertTrue(index.search("").isEmpty())
    }

    @Test
    fun findsCategoriesWithTheirSection() {
        val categorized = SearchIndex(
            listOf(
                SearchItem(
                    id = "category:movies:10",
                    title = "Harry Potter",
                    subtitle = "",
                    kind = SearchKind.CATEGORY,
                    categoryId = "10",
                    categorySection = "movies",
                ),
            ),
        )

        val hit = categorized.search("harry").single()
        assertEquals(SearchKind.CATEGORY, hit.item.kind)
        assertEquals("10", hit.item.categoryId)
        assertEquals("movies", hit.item.categorySection)
    }
}
