package com.andres3336.streaming.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelJsonTest {

    @Test
    fun roundTripsAllChannelFields() {
        val channel = Channel(
            name = "Canal",
            url = "https://example.com/live.m3u8",
            group = "TV",
            logo = "https://example.com/logo.png",
            tvgId = "canal-1",
            userAgent = "Agent",
            referer = "https://example.com",
            isVod = true,
            fallbackUrl = "https://example.com/live.ts",
            provider = "Xtream",
        )

        val decoded = decodeChannels(encodeChannels(listOf(channel))).single()

        assertEquals(channel, decoded)
    }

    @Test
    fun rejectsIncompleteEntries() {
        assertNull(channelFromJson(org.json.JSONObject("""{"name":"x"}""")))
        assertNull(channelFromJson(org.json.JSONObject("""{"url":"u"}""")))
        assertEquals(1, decodeChannels(encodeChannels(listOf(Channel("n", "u")))).size)
    }
}
