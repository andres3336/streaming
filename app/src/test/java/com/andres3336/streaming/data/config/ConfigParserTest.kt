package com.andres3336.streaming.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParserTest {

    private val gistConfig = """
        {
          "version": 2,
          "sources": {
            "liveMode": "gist",
            "m3uUrl": "https://example.com/list.m3u"
          },
          "player": {
            "userAgent": "Test/1.0",
            "headers": { "Referer": "https://example.com/" },
            "bufferMs": 5000,
            "qualityCap": "720p",
            "aspectMode": "fill"
          },
          "network": {
            "wifiOnly": true,
            "maxBitrateKbps": 8000
          },
          "cache": {
            "vodEnabled": true,
            "sizeMb": 500,
            "clearOnStart": true
          },
          "live": {
            "startLastChannel": false,
            "zapping": false
          },
          "epg": {
            "url": "https://example.com/epg.xml",
            "refreshHours": 24
          },
          "diagnostics": {
            "showStats": true,
            "logLevel": "debug"
          },
          "ui": {
            "theme": "light",
            "accentColor": "#FF0000",
            "fontScale": 1.2,
            "sections": ["live", "movies", "series"],
            "showChannelNumbers": false,
            "showLogos": false,
            "showNowNext": true
          },
          "forceUpdate": true
        }
    """.trimIndent()

    @Test
    fun parsesFullConfig() {
        val result = ConfigParser.parse(gistConfig)

        assertTrue(result is ConfigResult.Valid)
        val config = (result as ConfigResult.Valid).config
        assertEquals(2, config.version)
        assertEquals(LiveMode.GIST, config.sources.liveMode)
        assertEquals("https://example.com/list.m3u", config.sources.m3uUrl)

        assertEquals("Test/1.0", config.player.userAgent)
        assertEquals("https://example.com/", config.player.headers["Referer"])
        assertEquals(5000, config.player.bufferMs)
        assertEquals("720p", config.player.qualityCap)
        assertEquals("fill", config.player.aspectMode)

        assertTrue(config.network.wifiOnly)
        assertEquals(8000, config.network.maxBitrateKbps)
        assertEquals(500, config.cache.sizeMb)
        assertTrue(config.cache.clearOnStart)
        assertFalse(config.live.startLastChannel)
        assertFalse(config.live.zapping)
        assertEquals("https://example.com/epg.xml", config.epg.url)
        assertEquals(24, config.epg.refreshHours)
        assertTrue(config.diagnostics.showStats)
        assertEquals("debug", config.diagnostics.logLevel)
        assertEquals("light", config.ui.theme)
        assertEquals("#FF0000", config.ui.accentColor)
        assertEquals(1.2f, config.ui.fontScale)
        assertEquals(listOf("live", "movies", "series"), config.ui.sections)
        assertFalse(config.ui.showChannelNumbers)
        assertFalse(config.ui.showLogos)
        assertTrue(config.ui.showNowNext)
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
    fun validatesXtreamEvenInGistMode() {
        val json = """
            {
              "version": 1,
              "sources": {
                "liveMode": "gist",
                "m3uUrl": "https://example.com/a.m3u",
                "xtream": {
                  "baseUrl": "not-a-url",
                  "username": "",
                  "password": ""
                }
              }
            }
        """.trimIndent()

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("baseUrl") })
        assertTrue(errors.any { it.contains("username") })
        assertTrue(errors.any { it.contains("password") })
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
    fun rejectsInvalidV2Values() {
        val json = """
            {
              "version": 2,
              "sources": {"liveMode": "gist", "m3uUrl": "https://example.com/a.m3u"},
              "player": {"qualityCap": "4k", "aspectMode": "wide", "maxBufferMs": 1000},
              "network": {"maxBitrateKbps": -1},
              "cache": {"sizeMb": 999},
              "epg": {"refreshHours": 0},
              "diagnostics": {"logLevel": "trace"},
              "ui": {"accentColor": "red", "fontScale": 3.0, "theme": "neon"}
            }
        """.trimIndent()

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("qualityCap") })
        assertTrue(errors.any { it.contains("aspectMode") })
        assertTrue(errors.any { it.contains("maxBufferMs") })
        assertTrue(errors.any { it.contains("maxBitrateKbps") })
        assertTrue(errors.any { it.contains("sizeMb") })
        assertTrue(errors.any { it.contains("refreshHours") })
        assertTrue(errors.any { it.contains("logLevel") })
        assertTrue(errors.any { it.contains("accentColor") })
        assertTrue(errors.any { it.contains("fontScale") })
        assertTrue(errors.any { it.contains("theme") })
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
        assertEquals(15000, config.player.minBufferMs)
        assertEquals(50000, config.player.maxBufferMs)
        assertEquals(3, config.player.retryCount)
        assertEquals(1000, config.player.retryDelayMs)
        assertTrue(config.player.reconnect)
        assertEquals("auto", config.player.qualityCap)
        assertEquals("fit", config.player.aspectMode)
        assertFalse(config.player.audioOnly)
        assertEquals(0, config.player.sleepTimerMinutes)

        assertFalse(config.network.wifiOnly)
        assertEquals(0, config.network.maxBitrateKbps)
        assertEquals(15000, config.network.connectTimeoutMs)
        assertEquals(20000, config.network.readTimeoutMs)

        assertTrue(config.cache.vodEnabled)
        assertEquals(250, config.cache.sizeMb)
        assertFalse(config.cache.clearOnStart)

        assertTrue(config.live.startLastChannel)
        assertTrue(config.live.zapping)
        assertNull(config.epg.url)
        assertEquals(12, config.epg.refreshHours)
        assertFalse(config.diagnostics.showStats)
        assertEquals("warn", config.diagnostics.logLevel)

        assertEquals("dark", config.ui.theme)
        assertEquals("#3B82F6", config.ui.accentColor)
        assertEquals(1.0f, config.ui.fontScale)
        assertEquals(listOf("live"), config.ui.sections)
        assertTrue(config.ui.showChannelNumbers)
        assertTrue(config.ui.showLogos)
        assertFalse(config.ui.showNowNext)
    }

    @Test
    fun supportsLegacyEpgUrl() {
        val json = """
            {
              "version": 1,
              "sources": {"liveMode": "gist", "m3uUrl": "https://example.com/a.m3u"},
              "epgUrl": "https://example.com/epg.xml"
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertEquals("https://example.com/epg.xml", config.epg.url)
    }
}
