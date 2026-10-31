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
    fun remembersTheLastChannel() {
        var state = LiveState()
        state = state.remember("channel-1")
        state = state.remember("channel-2")

        assertEquals("channel-2", state.lastChannel)
    }

    @Test
    fun keepsNamespacedVodFavoritesAndIgnoresThemWhenResolvingChannels() {
        val state = LiveState()
            .toggleFavorite("movie:42")
            .toggleFavorite("series:7")
            .toggleFavorite("canal-1")

        val restored = LiveState.fromJson(state.toJson())

        assertEquals(listOf("canal-1", "series:7", "movie:42"), restored.favorites)
        assertEquals(
            listOf("Canal"),
            LiveState.resolve(restored.favorites, listOf(channel("Canal", tvgId = "canal-1")))
                .map(Channel::name),
        )
    }

    @Test
    fun roundTripsJson() {
        val state = LiveState(
            favorites = listOf("id-1", "id-2"),
            lastChannel = "id-3",
        )

        assertEquals(state, LiveState.fromJson(state.toJson()))
    }

    @Test
    fun readsIncompleteJsonAndIgnoresLegacyRecents() {
        assertEquals(LiveState(), LiveState.fromJson(JSONObject("{}")))
        assertEquals(
            LiveState(),
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
