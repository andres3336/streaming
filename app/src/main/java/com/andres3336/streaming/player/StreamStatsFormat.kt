package com.andres3336.streaming.player

import java.net.URI
import java.util.Locale

/**
 * Pure formatting helpers for the stream diagnostics panel. Keeping them here
 * makes the numbers testable and keeps credentials out of the UI.
 */
object StreamStatsFormat {

    fun bitrate(bitsPerSecond: Long): String = when {
        bitsPerSecond <= 0L -> "—"
        bitsPerSecond >= 1_000_000L ->
            String.format(Locale.US, "%.1f Mbps", bitsPerSecond / 1_000_000.0)

        bitsPerSecond >= 1_000L ->
            String.format(Locale.US, "%.0f kbps", bitsPerSecond / 1_000.0)

        else -> "$bitsPerSecond bps"
    }

    fun resolution(width: Int, height: Int): String =
        if (width > 0 && height > 0) "$width × $height" else "—"

    fun frameRate(fps: Float): String =
        if (fps > 0f) String.format(Locale.US, "%.1f fps", fps) else "—"

    fun buffer(durationMs: Long): String =
        if (durationMs > 0L) {
            String.format(Locale.US, "%.1f s", durationMs / 1000.0)
        } else {
            "—"
        }

    fun droppedFrames(count: Int): String = if (count >= 0) count.toString() else "—"

    /** Only the host is shown so URL tokens, paths and credentials never leak. */
    fun maskedSource(url: String): String = runCatching {
        URI(url).host?.takeIf(String::isNotBlank) ?: "oculta"
    }.getOrDefault("oculta")

    fun codec(mimeType: String?, codecs: String?): String {
        val name = when (mimeType) {
            "video/avc" -> "H.264"
            "video/hevc" -> "H.265"
            "video/x-vnd.on2.vp9" -> "VP9"
            "video/av01" -> "AV1"
            "video/mp4v-es" -> "MPEG-4"
            "video/mpeg2" -> "MPEG-2"
            else -> null
        }
        val detail = codecs?.substringBefore(',')?.trim()?.takeIf(String::isNotBlank)

        return when {
            name != null && detail != null -> "$name · $detail"
            name != null -> name
            detail != null -> detail
            else -> "—"
        }
    }
}
