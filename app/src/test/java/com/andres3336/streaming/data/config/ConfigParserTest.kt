package com.andres3336.streaming.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParserTest {

    private val gistConfig = """
        {
          "version": 1,
          "sources": {
            "liveMode": "gist",
            "m3uUrl": "https://example.com/list.m3u"
          },
          "player": {
            "userAgent": "Test/1.0",
            "headers": { "Referer": "https://example.com/" },
            "bufferMs": 5000
          },
          "epgUrl": "https://example.com/epg.xml",
          "ui": {
            "simpleMode": true,
            "pin": "1234",
            "sections": ["live"]
          },
          "forceUpdate": true
        }
    """.trimIndent()

    @Test
    fun parsesGistConfig() {
        val result = ConfigParser.parse(gistConfig)

        assertTrue(result is ConfigResult.Valid)
        val config = (result as ConfigResult.Valid).config
        assertEquals(1, config.version)
        assertEquals(LiveMode.GIST, config.sources.liveMode)
        assertEquals("https://example.com/list.m3u", config.sources.m3uUrl)
        assertEquals("Test/1.0", config.player.userAgent)
        assertEquals("https://example.com/", config.player.headers["Referer"])
        assertEquals(5000, config.player.bufferMs)
        assertEquals("https://example.com/epg.xml", config.epgUrl)
        assertTrue(config.ui.simpleMode)
        assertEquals("1234", config.ui.pin)
        assertEquals(listOf("live"), config.ui.sections)
        assertTrue(config.forceUpdate)
    }

    @Test
    fun parsesXtreamConfig() {
        val json = """
            {
              "version": 1,
              "sources": {
                "liveMode": "xtream",
                "xtream": {
                  "baseUrl": "http://tv.example.com:8080",
                  "username": "user",
                  "password": "pass"
                }
              }
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertEquals(LiveMode.XTREAM, config.sources.liveMode)
        assertEquals("http://tv.example.com:8080", config.sources.xtream?.baseUrl)
        assertEquals("user", config.sources.xtream?.username)
    }

    @Test
    fun mergeRequiresM3uAndXtream() {
        val json = """{"version":1,"sources":{"liveMode":"merge"}}"""

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("m3uUrl") })
        assertTrue(errors.any { it.contains("xtream") })
    }

    @Test
    fun rejectsInvalidJson() {
        val result = ConfigParser.parse("{not json")

        assertTrue(result is ConfigResult.Invalid)
        assertTrue((result as ConfigResult.Invalid).errors.single().startsWith("JSON inválido"))
    }

    @Test
    fun rejectsUnknownLiveMode() {
        val json = """{"version":1,"sources":{"liveMode":"otro","m3uUrl":"https://example.com/a.m3u"}}"""

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("liveMode") })
    }

    @Test
    fun rejectsNonHttpUrls() {
        val json = """{"version":1,"sources":{"liveMode":"gist","m3uUrl":"ftp://example.com/a.m3u"}}"""

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("m3uUrl") })
    }

    @Test
    fun rejectsBadPin() {
        val json = """
            {
              "version": 1,
              "sources": {"liveMode": "gist", "m3uUrl": "https://example.com/a.m3u"},
              "ui": {"pin": "12ab"}
            }
        """.trimIndent()

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("pin") })
    }

    @Test
    fun rejectsUnknownSections() {
        val json = """
            {
              "version": 1,
              "sources": {"liveMode": "gist", "m3uUrl": "https://example.com/a.m3u"},
              "ui": {"sections": ["live", "radio"]}
            }
        """.trimIndent()

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("sections") })
    }

    @Test
    fun fillsDefaults() {
        val json = """
            {
              "version": 1,
              "sources": {"liveMode": "gist", "m3uUrl": "https://example.com/a.m3u"}
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertEquals(3000, config.player.bufferMs)
        assertFalse(config.ui.simpleMode)
        assertEquals(listOf("live"), config.ui.sections)
        assertFalse(config.forceUpdate)
        assertEquals(null, config.sources.xtream)
    }
}
