package com.andres3336.streaming.data.epg

import com.andres3336.streaming.data.Channel
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmltvParserTest {

    private val now = utc("20261029183000")

    @Test
    fun parsesProgrammesAndFillsMissingStops() {
        val guide = XmltvParser.parse(XML.byteInputStream(), now)

        assertEquals("Noticias", guide.nowNext(Channel("Canal Uno", "u", tvgId = "c1"), now)?.current?.title)
        assertEquals("Fútbol", guide.nowNext(Channel("Canal Uno", "u", tvgId = "c1"), now)?.next?.title)

        val noStop = guide.nowNext(Channel("Canal Dos", "u", tvgId = "c2"), now)?.current
        assertEquals("Sin fin", noStop?.title)
        assertEquals(utc("20261029193000"), noStop?.stopMs)
    }

    @Test
    fun matchesByTvgIdThenDisplayName() {
        val guide = XmltvParser.parse(XML.byteInputStream(), now)

        assertEquals(
            "Noticias",
            guide.nowNext(Channel("Cualquier nombre", "u", tvgId = "c1"), now)?.current?.title,
        )
        assertEquals(
            "Noticias",
            guide.nowNext(Channel("canal uno", "u"), now)?.current?.title,
        )
        assertNull(guide.nowNext(Channel("Desconocido", "u"), now))
    }

    @Test
    fun hidesWhenTheWindowHasNoData() {
        val guide = XmltvParser.parse(XML.byteInputStream(), now + 10L * 24L * 60L * 60L * 1000L)

        assertEquals(0, guide.programmes.size)
        assertNull(guide.nowNext(Channel("Canal Uno", "u", tvgId = "c1"), now))
    }

    @Test
    fun formatsTimeLeft() {
        assertEquals("<1 min", EpgGuide.formatTimeLeft(30_000))
        assertEquals("30 min", EpgGuide.formatTimeLeft(30L * 60_000))
        assertEquals("1 h 05 min", EpgGuide.formatTimeLeft(65L * 60_000))
        assertEquals("2 h", EpgGuide.formatTimeLeft(120L * 60_000))
        assertEquals("", EpgGuide.formatTimeLeft(0))
    }

    @Test
    fun roundTripsGuideJson() {
        val guide = XmltvParser.parse(XML.byteInputStream(), now)

        assertEquals(guide, EpgGuide.fromJson(guide.toJson()))
    }

    private fun utc(value: String): Long {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(value)!!.time
    }

    private companion object {
        val XML = """
            <tv>
              <channel id="c1"><display-name>Canal Uno</display-name></channel>
              <channel id="c2"><display-name>Canal Dos</display-name></channel>
              <programme start="20261029180000 +0000" stop="20261029190000 +0000" channel="c1">
                <title lang="en">News</title>
                <title lang="es">Noticias</title>
              </programme>
              <programme start="20261029190000 +0000" stop="20261029200000 +0000" channel="c1">
                <title>Fútbol</title>
              </programme>
              <programme start="20261029183000 +0000" channel="c2">
                <title>Sin fin</title>
              </programme>
            </tv>
        """.trimIndent()
    }
}
