package com.andres3336.streaming.data.playback

import com.andres3336.streaming.data.Channel

data class PlaybackRequest(
    val channel: Channel,
    val queue: List<Channel> = emptyList(),
    val index: Int = -1,
    val startPositionMs: Long = 0L,
) {

    val next: Channel?
        get() = queue.getOrNull(index + 1)

    fun nextRequest(): PlaybackRequest? {
        val nextChannel = next ?: return null
        return PlaybackRequest(
            channel = nextChannel,
            queue = queue,
            index = index + 1,
            startPositionMs = 0L,
        )
    }
}
