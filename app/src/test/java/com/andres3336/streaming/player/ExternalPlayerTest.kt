package com.andres3336.streaming.player

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalPlayerTest {

    @Test
    fun picksMimeTypeFromExtension() {
        assertEquals("video/x-matroska", ExternalPlayer.mimeTypeFor("https://x/a.mkv"))
        assertEquals("video/mp4", ExternalPlayer.mimeTypeFor("https://x/a.mp4?token=1"))
        assertEquals("video/mp2t", ExternalPlayer.mimeTypeFor("https://x/a.ts"))
        assertEquals("video/x-msvideo", ExternalPlayer.mimeTypeFor("https://x/a.avi"))
    }
}
