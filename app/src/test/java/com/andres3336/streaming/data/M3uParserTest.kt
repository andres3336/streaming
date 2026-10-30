package com.andres3336.streaming.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun parsesExtInfAttributes() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="canal1" tvg-name="Uno" tvg-logo="https://x/logo.png" group-title="Noticias",Canal Uno
            https://example.com/uno.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        val channel = channels.single()
        assertEquals("Canal Uno", channel.name)
        assertEquals("https://example.com/uno.m3u8", channel.url)
        assertEquals("Noticias", channel.group)
        assertEquals("https://x/logo.png", channel.logo)
        assertEquals("canal1", channel.tvgId)
    }

    @Test
    fun handlesBomAndCrlf() {
        val playlist = "\uFEFF#EXTM3U\r\n#EXTINF:-1,Canal\r\nhttps://example.com/a.ts\r\n"

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Canal", channels.single().name)
        assertEquals("https://example.com/a.ts", channels.single().url)
    }

    @Test
    fun usesExtGrpWhenGroupTitleIsMissing() {
        val playlist = """
            #EXTINF:-1,Canal
            #EXTGRP:Deportes
            https://example.com/a.ts
        """.trimIndent()

        assertEquals("Deportes", M3uParser.parse(playlist).single().group)
    }

    @Test
    fun readsVlcHeaders() {
        val playlist = """
            #EXTINF:-1,Canal
            #EXTVLCOPT:http-user-agent=MiPlayer/1.0
            #EXTVLCOPT:http-referrer=https://example.com/
            https://example.com/a.ts
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()
        assertEquals("MiPlayer/1.0", channel.userAgent)
        assertEquals("https://example.com/", channel.referer)
    }

    @Test
    fun skipsEntriesWithoutUrl() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Sin URL
            #EXTINF:-1,Con URL
            https://example.com/a.ts
        """.trimIndent()

        val channels = M3uParser.parse(playlist)

        assertEquals(1, channels.size)
        assertEquals("Con URL", channels.single().name)
    }

    @Test
    fun commaInsideQuotedAttributeDoesNotSplitTheName() {
        val playlist = """
            #EXTINF:-1 group-title="Noticias, deportes",Canal
            https://example.com/a.ts
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()
        assertEquals("Noticias, deportes", channel.group)
        assertEquals("Canal", channel.name)
    }

    @Test
    fun keepsCommasInDisplayName() {
        val playlist = """
            #EXTINF:-1,Canal, en directo
            https://example.com/a.ts
        """.trimIndent()

        assertEquals("Canal, en directo", M3uParser.parse(playlist).single().name)
    }

    @Test
    fun fallsBackToTvgNameWhenDisplayNameIsEmpty() {
        val playlist = """
            #EXTINF:-1 tvg-name="Respaldo",
            https://example.com/a.ts
        """.trimIndent()

        assertEquals("Respaldo", M3uParser.parse(playlist).single().name)
    }

    @Test
    fun returnsEmptyListForEmptyInput() {
        assertTrue(M3uParser.parse("").isEmpty())
        assertTrue(M3uParser.parse("#EXTM3U\n#EXTINF:-1,Sin URL\n").isEmpty())
    }

    @Test
    fun missingOptionalFieldsAreNull() {
        val playlist = """
            #EXTINF:-1,Canal
            https://example.com/a.ts
        """.trimIndent()

        val channel = M3uParser.parse(playlist).single()
        assertNull(channel.group)
        assertNull(channel.logo)
        assertNull(channel.tvgId)
        assertNull(channel.userAgent)
        assertNull(channel.referer)
    }

    @Test
    fun parsesSingleQuotedAttributes() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id='canal1' group-title='Canales' tvg-logo='https://example.com/logo.png',Canal 1
            https://example.com/1.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(text).single()

        assertEquals("canal1", channel.tvgId)
        assertEquals("Canales", channel.group)
        assertEquals("https://example.com/logo.png", channel.logo)
    }

    @Test
    fun acceptsExtgrpBeforeExtinf() {
        val text = """
            #EXTM3U
            #EXTGRP:Canales
            #EXTINF:-1,Canal 1
            https://example.com/1.m3u8
        """.trimIndent()

        assertEquals("Canales", M3uParser.parse(text).single().group)
    }

    @Test
    fun acceptsUnquotedAttributes() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id=canal1 group-title=Canales,Canal 1
            https://example.com/1.m3u8
        """.trimIndent()

        val channel = M3uParser.parse(text).single()

        assertEquals("canal1", channel.tvgId)
        assertEquals("Canales", channel.group)
    }
}
