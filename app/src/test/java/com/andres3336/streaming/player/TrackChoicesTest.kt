package com.andres3336.streaming.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackChoicesTest {

    @Test
    fun labelsAudioTracksWithLanguageLayoutAndCodec() {
        val info = TrackLabelInfo(
            language = "es",
            channelCount = 2,
            sampleMimeType = "audio/mp4a-latm",
        )

        assertEquals("español · Estéreo · AAC", TrackChoices.audioLabel(info, 0))
    }

    @Test
    fun translatesThreeLetterLanguageCodes() {
        val info = TrackLabelInfo(
            language = "spa",
            channelCount = 6,
            sampleMimeType = "audio/ac3",
        )

        assertEquals("español · 5.1 · AC-3", TrackChoices.audioLabel(info, 0))
    }

    @Test
    fun fallsBackToTrackNumberWithoutMetadata() {
        assertEquals("Pista 2", TrackChoices.audioLabel(TrackLabelInfo(), 1))
        assertEquals("Subtítulo 1", TrackChoices.subtitleLabel(TrackLabelInfo(), 0))
    }

    @Test
    fun labelsForcedSubtitles() {
        val info = TrackLabelInfo(language = "en", forced = true)

        assertEquals("inglés · Forzados", TrackChoices.subtitleLabel(info, 0))
    }

    @Test
    fun keepsTheProvidedTrackLabel() {
        val info = TrackLabelInfo(language = "es", label = "Comentarios")

        assertEquals("español · Comentarios", TrackChoices.audioLabel(info, 0))
    }
}
