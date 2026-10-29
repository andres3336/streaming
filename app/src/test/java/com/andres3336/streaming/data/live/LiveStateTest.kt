package com.andres3336.streaming.data.live

import com.andres3336.streaming.data.Channel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStateTest {

    @Test
    fun togglesFavoritesAndKeepsNewestFirst() {
        val state = LiveState()
            .toggleFavorite("a")
            .toggleFavorite("b")

        assertEquals(listOf("b", "a"), state.favorites)
        assertTrue(state.isFavorite("a"))

        val removed = state.toggleFavorite("a")
        assertEquals(listOf("b"), removed.favorites)
        assertFalse(removed.isFavorite("a"))
    }

    @Test
    fun ignoresBlankKeys() {
        val state = LiveState().toggleFavorite("").remember("  ")

        assertEquals(LiveState(), state)
    }

    @Test
    fun remembersRecentsWithoutDuplicatesAndCapsTheList() {
        var state = LiveState()
        repeat(RECENT_CHANNEL_LIMIT + 3) { index ->
            state = state.remember("channel-$index")
        }

        assertEquals(RECENT_CHANNEL_LIMIT, state.recents.size)
        assertEquals("channel-12", state.recents.first())
        assertEquals("channel-12", state.lastChannel)

        state = state.remember("channel-5")
        assertEquals("channel-5", state.recents.first())
        assertEquals(1, state.recents.count { it == "channel-5" })
    }

    @Test
    fun roundTripsJson() {
        val state = LiveState(
            favorites = listOf("id-1", "id-2"),
            recents = listOf("id-2", "id-3"),
            lastChannel = "id-3",
        )

        assertEquals(state, LiveState.fromJson(state.toJson()))
    }

    @Test
    fun readsIncompleteJson() {
        assertEquals(LiveState(), LiveState.fromJson(JSONObject("{}")))
        assertEquals(
            LiveState(recents = listOf("a")),
            LiveState.fromJson(JSONObject("""{"favorites":[],"recents":["a","a"," "]}""")),
        )
    }

    @Test
    fun resolvePreservesStoredOrderAndDropsMissingChannels() {
        val channels = listOf(channel("A", tvgId = "a"), channel("B", tvgId = "b"))

        val resolved = LiveState.resolve(listOf("b", "missing", "a"), channels)

        assertEquals(listOf("B", "A"), resolved.map(Channel::name))
    }

    @Test
    fun liveKeyPrefersTvgIdAndFallsBackToUrl() {
        assertEquals("id", channel("A", tvgId = "id").liveKey())
        assertEquals("https://example.com/a", channel("A", tvgId = " ").liveKey())
    }

    private fun channel(name: String, tvgId: String?) = Channel(
        name = name,
        url = "https://example.com/${name.lowercase()}",
        tvgId = tvgId,
    )
}
