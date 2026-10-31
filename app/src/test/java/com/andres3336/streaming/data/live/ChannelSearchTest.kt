package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelSearchTest {

    private val channels = listOf(
        Channel(name = "Canal Sur", url = "u1"),
        Channel(name = "Televisión Española", url = "u2"),
        Channel(name = "Cine Acción", url = "u3"),
    )

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(channels, ChannelSearch.filter(channels, ""))
        assertEquals(channels, ChannelSearch.filter(channels, "   "))
    }

    @Test
    fun matchesChannelNameIgnoringAccentsAndCase() {
        assertEquals(listOf("Televisión Española"), names("television espanola"))
        assertEquals(listOf("Canal Sur"), names("CANAL SUR"))
    }

    @Test
    fun ignoresCategoryNames() {
        assertTrue(ChannelSearch.filter(channels, "peliculas").isEmpty())
        assertTrue(ChannelSearch.filter(channels, "andalucia").isEmpty())
    }

    @Test
    fun returnsEmptyWhenNothingMatches() {
        assertTrue(ChannelSearch.filter(channels, "deportes").isEmpty())
    }

    @Test
    fun matchesReportsIndividualChannels() {
        assertTrue(ChannelSearch.matches(channels[0], "sur"))
        assertFalse(ChannelSearch.matches(channels[0], "espanola"))
    }

    private fun names(query: String): List<String> =
        ChannelSearch.filter(channels, query).map(Channel::name)
}
