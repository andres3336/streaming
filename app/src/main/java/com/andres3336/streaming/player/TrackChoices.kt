package com.andres3336.streaming.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import java.util.Locale

data class TrackChoice(
    val id: String,
    val label: String,
    val selected: Boolean,
    val group: TrackGroup,
    val trackIndex: Int,
)

/** Player-independent metadata used to build a track label. */
data class TrackLabelInfo(
    val language: String? = null,
    val label: String? = null,
    val channelCount: Int = 0,
    val sampleMimeType: String? = null,
    val forced: Boolean = false,
)

/**
 * Builds the Spanish labels and selection state for the player track pickers.
 * The label functions are pure so they can be unit tested without a player.
 */
object TrackChoices {

    fun audio(tracks: Tracks): List<TrackChoice> = build(tracks, C.TRACK_TYPE_AUDIO, ::audioLabel)

    fun subtitles(tracks: Tracks): List<TrackChoice> =
        build(tracks, C.TRACK_TYPE_TEXT, ::subtitleLabel)

    fun hasSelectedSubtitles(tracks: Tracks): Boolean = tracks.groups.any { group ->
        group.type == C.TRACK_TYPE_TEXT && group.isSelected
    }

    fun audioLabel(info: TrackLabelInfo, index: Int): String {
        val parts = listOfNotNull(
            languageName(info.language),
            info.label?.takeIf(String::isNotBlank),
            channelLayout(info.channelCount),
            codecName(info.sampleMimeType),
        ).distinct()

        return parts.joinToString(" · ").ifBlank { "Pista ${index + 1}" }
    }

    fun subtitleLabel(info: TrackLabelInfo, index: Int): String {
        val parts = listOfNotNull(
            languageName(info.language),
            info.label?.takeIf(String::isNotBlank),
            if (info.forced) "Forzados" else null,
        ).distinct()

        return parts.joinToString(" · ").ifBlank { "Subtítulo ${index + 1}" }
    }

    private fun build(
        tracks: Tracks,
        type: Int,
        label: (TrackLabelInfo, Int) -> String,
    ): List<TrackChoice> {
        val choices = mutableListOf<TrackChoice>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != type) return@forEachIndexed

            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue

                choices += TrackChoice(
                    id = "$groupIndex:$trackIndex",
                    label = label(group.getTrackFormat(trackIndex).toLabelInfo(), trackIndex),
                    selected = group.isTrackSelected(trackIndex),
                    group = group.mediaTrackGroup,
                    trackIndex = trackIndex,
                )
            }
        }

        return choices
    }

    private fun Format.toLabelInfo() = TrackLabelInfo(
        language = language,
        label = label,
        channelCount = channelCount,
        sampleMimeType = sampleMimeType,
        forced = selectionFlags and C.SELECTION_FLAG_FORCED != 0,
    )

    private fun languageName(code: String?): String? {
        val raw = code?.trim()?.takeIf { it.isNotEmpty() && it != "und" } ?: return null
        val tag = THREE_LETTER_CODES[raw.lowercase()] ?: raw

        return runCatching {
            val display = Locale.forLanguageTag(tag).getDisplayLanguage(Locale("es"))
            display.takeIf { it.isNotBlank() && it != tag }
        }.getOrNull() ?: raw.uppercase()
    }

    private fun channelLayout(count: Int): String? = when {
        count <= 0 -> null
        count == 1 -> "Mono"
        count == 2 -> "Estéreo"
        count == 6 -> "5.1"
        count == 8 -> "7.1"
        else -> "$count canales"
    }

    private fun codecName(mimeType: String?): String? = when (mimeType) {
        "audio/mp4a-latm" -> "AAC"
        "audio/ac3" -> "AC-3"
        "audio/eac3" -> "E-AC-3"
        "audio/opus" -> "Opus"
        "audio/vorbis" -> "Vorbis"
        "audio/mpeg" -> "MP3"
        "audio/flac" -> "FLAC"
        else -> null
    }

    private val THREE_LETTER_CODES = mapOf(
        "spa" to "es",
        "eng" to "en",
        "por" to "pt",
        "fra" to "fr",
        "fre" to "fr",
        "deu" to "de",
        "ger" to "de",
        "ita" to "it",
        "jpn" to "ja",
        "kor" to "ko",
        "zho" to "zh",
        "chi" to "zh",
        "rus" to "ru",
    )
}
