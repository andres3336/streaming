package com.andres3336.streaming.data.epg

import com.andres3336.streaming.data.config.EpgConfig
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class EpgRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var store: FakeStore
    private val now = utc("20261029183000")

    private val repository get() = EpgRepository(okhttp3.OkHttpClient())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        store = FakeStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun downloadsGzipXmlAndUsesFreshCache() = runBlocking {
        server.enqueue(MockResponse().setBody(okio.Buffer().write(gzip(XML))))

        val config = EpgConfig(url = server.url("/guide.xml.gz").toString(), refreshHours = 12)
        val first = repository.load(config, "epg-key", store, now)
        val second = repository.load(config, "epg-key", store, now + 60_000)

        assertEquals("Noticias", first?.programmes?.get("c1")?.first()?.title)
        assertEquals(1, server.requestCount)
        assertEquals("Noticias", second?.programmes?.get("c1")?.first()?.title)
        assertEquals("epg-key", store.sourceKey)
    }

    @Test
    fun fallsBackToCacheWhenDownloadFails() = runBlocking {
        server.enqueue(MockResponse().setBody(okio.Buffer().write(gzip(XML))))
        server.enqueue(MockResponse().setResponseCode(500))
        val config = EpgConfig(url = server.url("/guide.xml.gz").toString(), refreshHours = 1)

        repository.load(config, "epg-key", store, now)
        val stale = repository.load(config, "epg-key", store, now + 2L * 60L * 60L * 1000L)

        assertEquals("Noticias", stale?.programmes?.get("c1")?.first()?.title)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun doesNothingWithoutUrl() = runBlocking {
        val result = repository.load(EpgConfig(url = null), "epg-key", store, now)

        assertNull(result)
        assertEquals(0, server.requestCount)
        assertNull(store.entry)
    }

    private fun gzip(text: String): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(text.toByteArray()) }
        return output.toByteArray()
    }

    private fun utc(value: String): Long {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(value)!!.time
    }

    private class FakeStore : EpgStore {
        var entry: EpgCacheEntry? = null
        var sourceKey: String? = null

        override fun load(sourceKey: String): EpgCacheEntry? =
            entry.takeIf { sourceKey == this.sourceKey }

        override suspend fun save(sourceKey: String, entry: EpgCacheEntry) {
            this.sourceKey = sourceKey
            this.entry = entry
        }

        override fun clear() {
            entry = null
            sourceKey = null
        }
    }

    private companion object {
        val XML = """
            <tv>
              <channel id="c1"><display-name>Canal Uno</display-name></channel>
              <programme start="20261029180000 +0000" stop="20261029190000 +0000" channel="c1">
                <title>Noticias</title>
              </programme>
            </tv>
        """.trimIndent()
    }
}
