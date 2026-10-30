package com.andres3336.streaming.data.cache

import com.andres3336.streaming.data.config.AppConfig
import com.andres3336.streaming.data.config.LiveMode
import com.andres3336.streaming.data.config.PlayerConfig
import com.andres3336.streaming.data.config.SourcesConfig
import com.andres3336.streaming.data.config.XtreamConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CacheIdentityTest {

    private fun config(
        url: String = "https://example.com/list.m3u",
        headers: Map<String, String> = emptyMap(),
        user: String = "user",
        pass: String = "pass",
    ) = AppConfig(
        version = 2,
        sources = SourcesConfig(
            liveMode = LiveMode.MERGE,
            m3uUrl = url,
            xtream = XtreamConfig("https://example.com", user, pass),
        ),
        player = PlayerConfig(headers = headers),
    )

    @Test
    fun m3uIdentityChangesWithUrlOrHeaders() {
        val base = CacheIdentity.m3u(config())
        assertEquals(base, CacheIdentity.m3u(config()))
        assertNotEquals(base, CacheIdentity.m3u(config(url = "https://example.com/other.m3u")))
        assertNotEquals(
            base,
            CacheIdentity.m3u(config(headers = mapOf("Referer" to "https://example.com"))),
        )
        assertNull(CacheIdentity.m3u(config().copy(sources = SourcesConfig(LiveMode.XTREAM))))
    }

    @Test
    fun xtreamIdentityChangesWithAccount() {
        val base = CacheIdentity.xtream(config())
        assertEquals(base, CacheIdentity.xtream(config()))
        assertNotEquals(base, CacheIdentity.xtream(config(user = "other")))
        assertNotEquals(base, CacheIdentity.xtream(config(pass = "other")))
        assertNull(CacheIdentity.xtream(config().copy(sources = SourcesConfig(LiveMode.GIST))))
    }

    @Test
    fun sha256IsStableAndHidesTheInput() {
        val hash = CacheIdentity.sha256("secret")
        assertEquals(64, hash.length)
        assertEquals(hash, CacheIdentity.sha256("secret"))
        assertNotEquals(hash, CacheIdentity.sha256("secret2"))
    }
}
