package com.andres3336.streaming.data.config

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConfigRepositoryTest {

    private lateinit var server: MockWebServer

    private val repository
        get() = ConfigRepository(server.url("/config.json").toString())

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
    fun fetchesConfigAndEtag() = runBlocking {
        server.enqueue(
            MockResponse()
                .setBody(VALID_CONFIG)
                .setHeader("ETag", "\"v1\""),
        )

        val result = repository.fetch()

        assertTrue(result is ConfigFetch.Success)
        val success = result as ConfigFetch.Success
        assertEquals(1, success.config.version)
        assertEquals("\"v1\"", success.etag)
        assertTrue(success.raw.contains("\"version\""))
    }

    @Test
    fun sendsIfNoneMatchAndHandles304() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(304))

        val result = repository.fetch(etag = "\"v1\"")

        assertTrue(result is ConfigFetch.NotModified)
        assertEquals("\"v1\"", server.takeRequest().getHeader("If-None-Match"))
    }

    @Test
    fun reportsInvalidConfig() = runBlocking {
        server.enqueue(MockResponse().setBody("{}"))

        val result = repository.fetch()

        assertTrue(result is ConfigFetch.Failure)
        assertTrue((result as ConfigFetch.Failure).message.contains("sources"))
    }

    @Test
    fun reportsHttpErrors() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = repository.fetch()

        assertTrue(result is ConfigFetch.Failure)
        assertTrue((result as ConfigFetch.Failure).message.contains("500"))
    }

    @Test
    fun reportsMissingUrlWithoutNetwork() = runBlocking {
        val result = ConfigRepository("").fetch()

        assertTrue(result is ConfigFetch.MissingUrl)
    }

    private companion object {
        val VALID_CONFIG = """
            {
              "version": 1,
              "sources": {
                "liveMode": "gist",
                "m3uUrl": "https://example.com/list.m3u"
              }
            }
        """.trimIndent()
    }
}
