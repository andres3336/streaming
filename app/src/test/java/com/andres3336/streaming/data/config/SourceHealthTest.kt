package com.andres3336.streaming.data.config

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SourceHealthTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun healthySummaryListsCheckedSources() {
        val health = SourceHealth(
            liveChecked = true,
            liveHealthy = true,
            xtreamChecked = true,
            xtreamHealthy = true,
        )

        assertTrue(health.isHealthy)
        assertEquals("En vivo: OK · Xtream: OK", health.summary())
    }

    @Test
    fun failureSummaryNamesTheSource() {
        val health = SourceHealth(
            liveChecked = true,
            liveHealthy = false,
            liveMessage = "No se pudo cargar la lista de canales.",
            xtreamChecked = true,
            xtreamHealthy = false,
            xtreamMessage = "La cuenta Xtream no está activa (Expired).",
        )

        assertFalse(health.isHealthy)
        assertEquals("En vivo: fallo · Xtream: fallo", health.summary())
        assertEquals("La cuenta Xtream no está activa (Expired).", health.xtreamMessage)
    }

    @Test
    fun noSourcesReportedWhenNothingIsEnabled() {
        assertEquals("No hay fuentes que comprobar", SourceHealth().summary())
    }

    @Test
    fun checksM3uAndXtreamOnlyForEnabledSources() = runBlocking {
        server.enqueue(MockResponse().setBody("#EXTM3U\n#EXTINF:-1,Canal\nhttps://example.com/live.m3u8"))
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":1,"status":"Active"}}"""))

        val checker = SourceHealthChecker(
            client = okhttp3.OkHttpClient(),
            m3uRepository = com.andres3336.streaming.data.M3uRepository(),
        )
        val config = AppConfig(
            version = 2,
            sources = SourcesConfig(
                liveMode = LiveMode.MERGE,
                m3uUrl = server.url("/list.m3u").toString(),
                xtream = XtreamConfig(
                    baseUrl = server.url("/").toString().trimEnd('/'),
                    username = "user",
                    password = "pass",
                ),
            ),
        )

        val health = checker.check(config)

        assertTrue(health.liveChecked)
        assertTrue(health.liveHealthy)
        assertTrue(health.xtreamChecked)
        assertTrue(health.xtreamHealthy)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun skipsM3uInXtreamOnlyMode() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":0,"status":"Expired"}}"""))

        val checker = SourceHealthChecker(
            client = okhttp3.OkHttpClient(),
            m3uRepository = com.andres3336.streaming.data.M3uRepository(),
        )
        val config = AppConfig(
            version = 2,
            sources = SourcesConfig(
                liveMode = LiveMode.XTREAM,
                m3uUrl = server.url("/list.m3u").toString(),
                xtream = XtreamConfig(
                    baseUrl = server.url("/").toString().trimEnd('/'),
                    username = "user",
                    password = "pass",
                ),
            ),
        )

        val health = checker.check(config)

        assertFalse(health.liveChecked)
        assertTrue(health.xtreamChecked)
        assertFalse(health.xtreamHealthy)
        assertEquals(1, server.requestCount)
    }
}
