package com.andres3336.streaming.data.subtitles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrtCodecTest {

    @Test
    fun shiftsTimestampsForward() {
        val input = """
            1
            00:00:01,000 --> 00:00:02,500
            Hola
        """.trimIndent()

        val shifted = SrtCodec.shift(input, 1_500L)

        assertTrue(shifted.contains("00:00:02,500 --> 00:00:04,000"))
    }

    @Test
    fun shiftsBackwardsWithoutNegativeTimes() {
        val input = """
            1
            00:00:01,000 --> 00:00:02,000
            Hola
        """.trimIndent()

        val shifted = SrtCodec.shift(input, -5_000L)

        assertTrue(shifted.contains("00:00:00,000 --> 00:00:00,000"))
    }

    @Test
    fun decodesUtf8WithBom() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "Hola ñ".toByteArray(Charsets.UTF_8)

        assertEquals("Hola ñ", SrtCodec.decode(bytes))
    }

    @Test
    fun decodesWindows1252Fallback() {
        val bytes = byteArrayOf(0xF1.toByte()) // ñ in windows-1252

        assertEquals("ñ", SrtCodec.decode(bytes))
    }
}
