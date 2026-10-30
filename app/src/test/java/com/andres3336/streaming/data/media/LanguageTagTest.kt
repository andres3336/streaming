package com.andres3336.streaming.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageTagTest {

    @Test
    fun normalizesSpanishHints() {
        listOf("es", "spa", "Spanish", "Castellano", "Latino", "español").forEach {
            assertEquals(it, "ESP", LanguageTag.normalize(it))
        }
    }

    @Test
    fun normalizesEnglishAndDualHints() {
        listOf("en", "eng", "English", "ingles", "inglés").forEach {
            assertEquals(it, "ENG", LanguageTag.normalize(it))
        }
        assertEquals("DUAL", LanguageTag.normalize("Dual Audio"))
        assertEquals("DUAL", LanguageTag.normalize("multi"))
    }

    @Test
    fun keepsOtherIsoCodesAndHidesEmptyData() {
        assertEquals("POR", LanguageTag.normalize("Portuguese"))
        assertNull(LanguageTag.normalize(null))
        assertNull(LanguageTag.normalize("  "))
        assertNull(LanguageTag.normalize("audio"))
        assertNull(LanguageTag.normalize("unknown"))
    }
}
