package com.andres3336.streaming.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigV3Test {

    @Test
    fun v3SeparatesSourcesAndAddsChannels() {
        val json = """
            {
              "version": 3,
              "sources": {
                "m3uUrl": "https://example.com/list.m3u",
                "xtream": {
                  "baseUrl": "http://tv.example.com:8080",
                  "username": "user",
                  "password": "pass",
                  "minCategoryItems": 3,
                  "hideEmptyCategories": false
                }
              },
              "player": {"preBufferMs": 2000, "qualityPreference": "720p", "liveLowLatency": true},
              "subtitles": {
                "apiKey": "key",
                "languages": ["es", "en"],
                "autoDownload": false
              },
              "ui": {
                "sections": ["live", "channels", "movies", "series"],
                "uiScale": 1.1,
                "navigation": "rail",
                "playerBar": "compact",
                "playerButtons": ["info", "quality"]
              }
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertEquals(listOf("live", "channels", "movies", "series"), config.ui.sections)
        assertEquals(3, config.sources.xtream?.minCategoryItems)
        assertFalse(config.sources.xtream?.hideEmptyCategories ?: true)
        assertEquals(2000, config.player.preBufferMs)
        assertEquals("720p", config.player.qualityPreference)
        assertTrue(config.player.liveLowLatency)
        assertEquals("rail", config.ui.navigation)
        assertEquals("compact", config.ui.playerBar)
        assertEquals(listOf("info", "quality"), config.ui.playerButtons)
        assertEquals(1.1f, config.ui.uiScale)
        assertEquals("key", config.subtitles.apiKey)
        assertFalse(config.subtitles.autoDownload)
    }

    @Test
    fun v2WithXtreamMigratesChannelsTab() {
        val json = """
            {
              "version": 2,
              "sources": {
                "liveMode": "gist",
                "m3uUrl": "https://example.com/list.m3u",
                "xtream": {
                  "baseUrl": "http://tv.example.com:8080",
                  "username": "user",
                  "password": "pass"
                }
              },
              "ui": {"sections": ["live", "movies", "series"]}
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertTrue("channels" in config.ui.sections)
    }

    @Test
    fun v3AllowsXtreamOnly() {
        val json = """
            {
              "version": 3,
              "sources": {
                "xtream": {
                  "baseUrl": "http://tv.example.com:8080",
                  "username": "user",
                  "password": "pass"
                }
              }
            }
        """.trimIndent()

        val config = (ConfigParser.parse(json) as ConfigResult.Valid).config

        assertEquals(null, config.sources.m3uUrl)
        assertTrue("channels" in config.ui.sections)
        assertTrue("movies" in config.ui.sections)
        assertTrue("series" in config.ui.sections)
        assertFalse("live" in config.ui.sections)
    }

    @Test
    fun rejectsInvalidPlayerButtons() {
        val json = """
            {
              "version": 3,
              "sources": {"m3uUrl": "https://example.com/list.m3u"},
              "ui": {"playerButtons": ["info", "nope"]}
            }
        """.trimIndent()

        val errors = (ConfigParser.parse(json) as ConfigResult.Invalid).errors

        assertTrue(errors.any { it.contains("playerButtons") })
    }
}
