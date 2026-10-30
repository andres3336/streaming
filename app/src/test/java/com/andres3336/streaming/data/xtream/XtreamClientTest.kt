package com.andres3336.streaming.data.xtream

import com.andres3336.streaming.data.config.XtreamConfig
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class XtreamClientTest {

    private lateinit var server: MockWebServer

    private val client
        get() = XtreamClient(
            config = XtreamConfig(
                baseUrl = server.url("/").toString().trimEnd('/'),
                username = "user",
                password = "pass",
            ),
            client = okhttp3.OkHttpClient(),
        )

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
    fun readsAccountStatus() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":1,"status":"Active"}}"""))

        val result = client.accountStatus()

        assertTrue(result is XtreamResult.Success)
        assertTrue((result as XtreamResult.Success).value.authenticated)
        assertEquals("Active", result.value.status)
    }

    @Test
    fun reportsInactiveAccount() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"user_info":{"auth":0,"status":"Expired"}}"""))

        val result = client.accountStatus()

        assertTrue(result is XtreamResult.Success)
        assertFalse((result as XtreamResult.Success).value.authenticated)
        assertEquals("Expired", result.value.status)
    }

    @Test
    fun parsesLiveCategoriesAndStreams() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[{"category_id":"1","category_name":"Deportes"}]""",
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """[{"stream_id":10,"name":"Canal","stream_icon":"logo.png","category_id":"1","epg_channel_id":"canal.tv"}]""",
            ),
        )

        val categories = client.liveCategories()
        val streams = client.liveStreams("1")

        assertTrue(categories is XtreamResult.Success)
        assertEquals("Deportes", (categories as XtreamResult.Success).value.single().name)

        assertTrue(streams is XtreamResult.Success)
        val stream = (streams as XtreamResult.Success).value.single()
        assertEquals("Canal", stream.name)
        assertEquals("canal.tv", stream.epgChannelId)
    }

    @Test
    fun buildsLiveUrlsWithHlsAndTsFallback() {
        val stream = XtreamLiveStream(
            id = "10",
            name = "Canal",
            logo = null,
            categoryId = "1",
            epgChannelId = null,
        )

        assertEquals(
            "${server.url("/").toString().trimEnd('/')}/live/user/pass/10.m3u8",
            client.liveUrl(stream),
        )
        assertEquals(
            "${server.url("/").toString().trimEnd('/')}/live/user/pass/10.ts",
            client.liveFallbackUrl(stream),
        )
    }

    @Test
    fun convertsAStreamToAChannelWithAStableKey() {
        val stream = XtreamLiveStream("10", "Canal", "logo.png", "1", epgChannelId = null)

        val channel = client.liveChannel(stream, "Deportes", providerLabel = "Xtream")

        assertEquals("Canal", channel.name)
        assertEquals("Deportes", channel.group)
        assertEquals("xtream:10", channel.tvgId)
        assertEquals("Xtream", channel.provider)
        assertTrue(channel.fallbackUrl!!.endsWith(".ts"))
    }
}
