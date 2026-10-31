package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelMergeTest {

    @Test
    fun curatedListWinsAndDuplicatesAreDropped() {
        val gist = listOf(Channel("Canal Sur", "gist-1"))
        val xtream = listOf(
            Channel("canal sur", "xtream-1", provider = "Xtream"),
            Channel("Cine", "xtream-2", provider = "Xtream"),
        )

        val merged = mergeLiveChannels(gist, xtream)

        assertEquals(listOf("Canal Sur", "Cine"), merged.map(Channel::name))
        assertEquals("gist-1", merged[0].url)
    }

    @Test
    fun emptyInputsAreHandled() {
        val channel = Channel("A", "u")
        assertEquals(listOf(channel), mergeLiveChannels(emptyList(), listOf(channel)))
        assertEquals(listOf(channel), mergeLiveChannels(listOf(channel), emptyList()))
        assertEquals(emptyList<Channel>(), mergeLiveChannels(emptyList(), emptyList()))
    }
}
