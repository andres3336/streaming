package com.andres3336.streaming.player

import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

data class QualityOption(
    val height: Int,
    val bitrate: Int,
) {
    val label: String
        get() = buildString {
            append("${height}p")
            if (bitrate > 0) {
                append(" · ")
                append(QualitySelection.formatBitrate(bitrate))
            }
        }
}

/**
 * Manual HLS variant selection is expressed as a video size cap. ExoPlayer then
 * keeps its normal adaptive selection within the chosen height.
 */
object QualitySelection {

    fun options(tracks: Tracks): List<QualityOption> {
        val options = mutableListOf<QualityOption>()
        tracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_VIDEO) return@forEach
            for (index in 0 until group.length) {
                val format = group.getTrackFormat(index)
                if (format.height > 0) {
                    options += QualityOption(height = format.height, bitrate = format.bitrate)
                }
            }
        }
        return options
            .distinctBy { it.height }
            .sortedByDescending { it.height }
    }

    fun apply(
        player: ExoPlayer,
        preference: String,
        qualityCap: String,
        maxBitrateKbps: Int,
    ) {
        val height = preferenceHeight(preference) ?: capHeight(qualityCap)
        val builder = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)

        if (height != null) {
            builder.setMaxVideoSize(4_096, height)
        } else {
            builder.setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        }

        // A manual pick wins over the configured cap, but the network bitrate
        // limit still applies when the user set one.
        builder.setMaxVideoBitrate(maxBitrateKbps * 1_000)
        player.trackSelectionParameters = builder.build()
    }

    fun preferenceHeight(preference: String): Int? = when (preference) {
        "1080p" -> 1_080
        "720p" -> 720
        "480p" -> 480
        "360p" -> 360
        else -> preference.removeSuffix("p").toIntOrNull()
    }

    fun capHeight(cap: String): Int? = when (cap) {
        "1080p" -> 1_080
        "720p" -> 720
        "480p" -> 480
        "360p" -> 360
        else -> null
    }

    fun formatBitrate(bitrate: Int): String {
        if (bitrate <= 0) return "auto"
        val mbps = bitrate / 1_000_000.0
        return if (mbps >= 1.0) {
            String.format(java.util.Locale.US, "%.1f Mbps", mbps)
        } else {
            "${bitrate / 1_000} kbps"
        }
    }
}
