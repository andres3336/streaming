package com.andres3336.streaming.data.subtitles

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * SRT decoding and timestamp shifting. Subtitles downloaded from providers are
 * often UTF-8, but Windows-1252 is still common. Offsets are applied by writing
 * a shifted copy, which avoids depending on ExoPlayer internals.
 */
object SrtCodec {

    private val timestamp = Regex(
        """(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})[,.](\d{3})""",
    )

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
        }

        val utf8 = runCatching {
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        }.getOrNull()
        if (utf8 != null && !utf8.contains('\uFFFD')) return utf8

        return String(bytes, Charset.forName("windows-1252"))
    }

    fun shift(text: String, offsetMs: Long): String {
        if (offsetMs == 0L) return text
        return text.lineSequence().joinToString("\n") { line ->
            if (!line.contains("-->")) return@joinToString line
            timestamp.replace(line) { match ->
                val values = match.groupValues
                val start = toMs(values[1], values[2], values[3], values[4])
                val end = toMs(values[5], values[6], values[7], values[8])
                val shiftedStart = (start + offsetMs).coerceAtLeast(0L)
                val shiftedEnd = (end + offsetMs).coerceAtLeast(shiftedStart)
                "${format(shiftedStart)} --> ${format(shiftedEnd)}"
            }
        }
    }

    private fun toMs(h: String, m: String, s: String, ms: String): Long =
        h.toLong() * 3_600_000L +
            m.toLong() * 60_000L +
            s.toLong() * 1_000L +
            ms.toLong()

    private fun format(ms: Long): String {
        val total = ms.coerceAtLeast(0L)
        val hours = total / 3_600_000L
        val minutes = (total % 3_600_000L) / 60_000L
        val seconds = (total % 60_000L) / 1_000L
        val millis = total % 1_000L
        return String.format(
            Locale.US,
            "%02d:%02d:%02d,%03d",
            hours,
            minutes,
            seconds,
            millis,
        )
    }
}
