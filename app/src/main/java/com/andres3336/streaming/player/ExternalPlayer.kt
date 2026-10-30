package com.andres3336.streaming.player

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.andres3336.streaming.data.Channel
import com.andres3336.streaming.data.config.PlayerConfig

/**
 * Opens a movie or episode in another installed player. Live TV is intentionally
 * excluded: the internal player keeps zapping, HUD and recovery behavior.
 */
object ExternalPlayer {

    fun buildIntent(channel: Channel, config: PlayerConfig): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(channel.url), mimeTypeFor(channel.url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val headers = buildMap {
            putAll(config.headers)
            channel.userAgent?.let { put("User-Agent", it) }
            channel.referer?.let { put("Referer", it) }
        }
        if (headers.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val bundle = Bundle().apply {
                headers.forEach { (key, value) -> putString(key, value) }
            }
            // Different players honour different extra names; provide both.
            intent.putExtra("android.intent.extra.HTTP_HEADERS", bundle)
            intent.putExtra("headers", bundle)
        }
        return intent
    }

    fun mimeTypeFor(url: String): String {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".mkv") -> "video/x-matroska"
            path.endsWith(".avi") -> "video/x-msvideo"
            path.endsWith(".mov") -> "video/quicktime"
            path.endsWith(".webm") -> "video/webm"
            path.endsWith(".ts") -> "video/mp2t"
            path.endsWith(".m3u8") -> "application/x-mpegURL"
            else -> "video/mp4"
        }
    }
}
