package com.andres3336.streaming.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleVariantsTest {

    @Test
    fun mapsTwoLetterTagsToThreeLetters() {
        assertEquals("SUB", TitleVariants.parse("SU- Velocidad y Deseo").tag)
        assertEquals("CAM", TitleVariants.parse("CA- Velocidad y Deseo").tag)
        assertEquals("ESP", TitleVariants.parse("ES- Velocidad y Deseo").tag)
        assertEquals("ENG", TitleVariants.parse("EN- Something").tag)
        assertEquals("LAT", TitleVariants.parse("LA- Algo").tag)
    }

    @Test
    fun stripsTheTagFromTheBaseTitle() {
        assertEquals("Velocidad y Deseo", TitleVariants.parse("SU- Velocidad y Deseo").baseTitle)
        assertEquals("Verity: La sombra", TitleVariants.parse("CA- Verity: La sombra").baseTitle)
    }

    @Test
    fun understandsSpacedAndCastellanoPrefixes() {
        assertEquals("SUB", TitleVariants.parse("Sub 100 dias de engaño").tag)
        assertEquals("100 dias de engaño", TitleVariants.parse("Sub 100 dias de engaño").baseTitle)
        assertEquals("ESP", TitleVariants.parse("Cas- La lista terminal").tag)
        assertEquals("ESP", TitleVariants.parse("esp Jericho").tag)
        assertEquals("ESP", TitleVariants.parse("SPA- Una película").tag)
    }

    @Test
    fun ordersSpanishFirstAndSubEngLast() {
        val tags = listOf("ENG", "SUB", "VO", "ESP", "CAM", "LAT")

        val ordered = tags.sortedBy { TitleVariants.priority(it) }

        assertEquals(listOf("ESP", "LAT", "SUB", "CAM", "VO", "ENG"), ordered)
    }

    @Test
    fun doesNotEatShortSpanishWords() {
        assertEquals("VO", TitleVariants.parse("Es por ti").tag)
        assertEquals("Es por ti", TitleVariants.parse("Es por ti").baseTitle)
    }

    @Test
    fun leavesOrdinaryTitlesAlone() {
        val parsed = TitleVariants.parse("Harry Potter y la piedra filosofal")
        assertEquals(TitleVariants.VO, parsed.tag)
        assertEquals("Harry Potter y la piedra filosofal", parsed.baseTitle)
    }

    @Test
    fun groupsTheSameTitleAcrossVariants() {
        val keys = listOf(
            "SU- La isla del tesoro" to "2026",
            "CA- La isla del tesoro" to "2026",
            "ES- La isla del tesoro" to "2026",
        ).map { (name, year) -> TitleVariants.groupKey(name, year) }

        assertEquals(1, keys.distinct().size)
    }

    @Test
    fun groupsSpacedAndHyphenTagsTogether() {
        assertEquals(
            TitleVariants.groupKey("Sub 100 dias de engaño", null),
            TitleVariants.groupKey("100 Días de engaño", null),
        )
    }

    @Test
    fun keepsDifferentYearsApart() {
        assertEquals(
            false,
            TitleVariants.groupKey("SU- Corina", "2024") ==
                TitleVariants.groupKey("CA- Corina", "2025"),
        )
    }

    @Test
    fun ignoresAccentsWhenGrouping() {
        assertEquals(
            TitleVariants.groupKey("ES- Canción", null),
            TitleVariants.groupKey("SU- Cancion", null),
        )
    }
}
