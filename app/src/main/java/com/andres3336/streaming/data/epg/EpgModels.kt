package com.andres3336.streaming.data.epg

import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.live.ChannelSearch
import org.json.JSONArray
import org.json.JSONObject

data class EpgProgramme(
    val channelId: String,
    val title: String,
    val startMs: Long,
    val stopMs: Long,
)

data class EpgNowNext(
    val current: EpgProgramme?,
    val next: EpgProgramme?,
)

/**
 * Parsed XMLTV data for the rolling window. Channel display names provide a
 * fallback when the playlist has no usable tvg-id.
 */
data class EpgGuide(
    val channelNames: Map<String, List<String>> = emptyMap(),
    val programmes: Map<String, List<EpgProgramme>> = emptyMap(),
) {

    private val displayNameIndex: Map<String, String> by lazy {
        channelNames.entries
            .flatMap { (id, names) ->
                names.map { name -> ChannelSearch.normalize(name) to id }
            }
            .toMap()
    }

    fun programmesFor(channel: Channel): List<EpgProgramme> {
        channel.tvgId?.takeIf(String::isNotBlank)?.let { id ->
            programmes[id]?.takeIf(List<EpgProgramme>::isNotEmpty)?.let { return it }
        }

        val byName = displayNameIndex[ChannelSearch.normalize(channel.name)] ?: return emptyList()
        return programmes[byName].orEmpty()
    }

    fun nowNext(channel: Channel, nowMs: Long): EpgNowNext? {
        val list = programmesFor(channel)
        if (list.isEmpty()) return null

        val currentIndex = list.indexOfLast { it.startMs <= nowMs && nowMs < it.stopMs }
        val current = list.getOrNull(currentIndex)
        val next = if (currentIndex >= 0) {
            list.getOrNull(currentIndex + 1)
        } else {
            list.firstOrNull { it.startMs > nowMs }
        }

        if (current == null && next == null) return null
        return EpgNowNext(current = current, next = next)
    }

    fun window(nowMs: Long): EpgGuide {
        val from = nowMs - WINDOW_BEFORE_MS
        val to = nowMs + WINDOW_AFTER_MS
        val active = programmes
            .mapValues { (_, list) ->
                list.filter { it.stopMs > from && it.startMs < to }
            }
            .filterValues(List<EpgProgramme>::isNotEmpty)

        return copy(programmes = active)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("channels", JSONObject().apply {
            channelNames.forEach { (id, names) ->
                put(id, JSONArray().apply { names.forEach(::put) })
            }
        })
        put("programmes", JSONArray().apply {
            programmes.values.flatten().forEach { programme ->
                put(
                    JSONObject()
                        .put("channel", programme.channelId)
                        .put("title", programme.title)
                        .put("start", programme.startMs)
                        .put("stop", programme.stopMs),
                )
            }
        })
    }

    companion object {
        private const val WINDOW_BEFORE_MS = 6L * 60L * 60L * 1000L
        private const val WINDOW_AFTER_MS = 48L * 60L * 60L * 1000L

        fun formatTimeLeft(milliseconds: Long): String {
            if (milliseconds <= 0L) return ""
            val minutes = milliseconds / 60_000L
            if (minutes < 1L) return "<1 min"
            if (minutes < 60L) return "$minutes min"

            val hours = minutes / 60L
            val rest = minutes % 60L
            return if (rest == 0L) "$hours h" else "$hours h ${rest.toString().padStart(2, '0')} min"
        }

        /** Wall clock for programme start/stop times, e.g. `05:20`. */
        fun formatClock(milliseconds: Long): String {
            val calendar = java.util.Calendar.getInstance().apply { timeInMillis = milliseconds }
            return "%02d:%02d".format(
                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                calendar.get(java.util.Calendar.MINUTE),
            )
        }

        fun fromJson(json: JSONObject): EpgGuide {
            val channels = mutableMapOf<String, List<String>>()
            val channelsJson = json.optJSONObject("channels") ?: JSONObject()
            channelsJson.keys().asSequence().forEach { id ->
                val array = channelsJson.optJSONArray(id) ?: JSONArray()
                channels[id]  = (0 until array.length())
                    .mapNotNull { index -> array.optString(index).takeIf(String::isNotBlank) }
                    .distinct()
            }

            val programmes = mutableMapOf<String, MutableList<EpgProgramme>>()
            val array = json.optJSONArray("programmes") ?: JSONArray()
            (0 until array.length()).forEach { index ->
                val item = array.optJSONObject(index) ?: return@forEach
                val channelId = item.optString("channel").takeIf(String::isNotBlank) ?: return@forEach
                val title = item.optString("title").takeIf(String::isNotBlank) ?: return@forEach
                val start = item.optLong("start", 0L)
                val stop = item.optLong("stop", 0L)
                if (start <= 0L) return@forEach

                programmes.getOrPut(channelId) { mutableListOf() }
                    .add(EpgProgramme(channelId, title, start, stop))
            }

            return EpgGuide(
                channelNames = channels,
                programmes = programmes.mapValues { (_, list) -> list.sortedBy(EpgProgramme::startMs) },
            )
        }
    }
}
