package com.andres3336.streaming.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualitySelectionTest {

    @Test
    fun mapsPreferencesToHeights() {
        assertEquals(720, QualitySelection.preferenceHeight("720p"))
        assertEquals(1080, QualitySelection.preferenceHeight("1080p"))
        assertEquals(576, QualitySelection.preferenceHeight("576p"))
        assertNull(QualitySelection.preferenceHeight("auto"))
    }

    @Test
    fun mapsCapsToHeights() {
        assertEquals(480, QualitySelection.capHeight("480p"))
        assertNull(QualitySelection.capHeight("auto"))
    }

    @Test
    fun formatsBitrate() {
        assertEquals("4.5 Mbps", QualitySelection.formatBitrate(4_500_000))
        assertEquals("800 kbps", QualitySelection.formatBitrate(800_000))
        assertEquals("auto", QualitySelection.formatBitrate(0))
    }
}
