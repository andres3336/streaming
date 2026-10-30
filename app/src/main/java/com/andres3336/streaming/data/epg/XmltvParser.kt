package com.andres3336.streaming.data.epg

import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * Memory-friendly XMLTV parser using the platform SAX parser. Handles gzip
 * transparently only at the repository boundary; this parser receives XML.
 */
object XmltvParser {

    private const val DEFAULT_DURATION_MS = 60L * 60L * 1000L

    fun parse(input: InputStream, nowMs: Long = System.currentTimeMillis()): EpgGuide {
        val handler = Handler()
        val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = false }
        factory.newSAXParser().parse(input, handler)

        val byChannel = handler.programmes.groupBy(EpgProgramme::channelId)
        val filled = byChannel.mapValues { (_, list) ->
            val sorted = list.sortedBy(EpgProgramme::startMs)
            sorted.mapIndexed { index, programme ->
                if (programme.stopMs > programme.startMs) {
                    programme
                } else {
                    programme.copy(
                        stopMs = sorted.getOrNull(index + 1)?.startMs
                            ?: (programme.startMs + DEFAULT_DURATION_MS),
                    )
                }
            }
        }

        return EpgGuide(
            channelNames = handler.channelNames,
            programmes = filled,
        ).window(nowMs)
    }

    private class Handler : DefaultHandler() {

        val programmes = mutableListOf<EpgProgramme>()
        val channelNames = mutableMapOf<String, MutableList<String>>()

        private var channelId: String? = null
        private var channelBuilder: StringBuilder? = null
        private var programmeBuilder: ProgrammeBuilder? = null
        private var titleBuilder: StringBuilder? = null
        private var titleLanguage: String? = null

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?,
        ) {
            when (qName?.lowercase()) {
                "channel" -> {
                    channelId = attributes?.getValue("id")?.takeIf(String::isNotBlank)
                    channelBuilder = null
                }

                "display-name" -> {
                    if (channelId != null) channelBuilder = StringBuilder()
                }

                "programme" -> {
                    val channel = attributes?.getValue("channel")?.takeIf(String::isNotBlank)
                    if (channel != null) {
                        programmeBuilder = ProgrammeBuilder(
                            channelId = channel,
                            startMs = parseTime(attributes.getValue("start")),
                            stopMs = parseTime(attributes.getValue("stop")),
                        )
                    }
                }

                "title" -> {
                    if (programmeBuilder != null) {
                        titleBuilder = StringBuilder()
                        titleLanguage = attributes?.getValue("lang")
                    }
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            channelBuilder?.append(ch, start, length)
            titleBuilder?.append(ch, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            when (qName?.lowercase()) {
                "display-name" -> {
                    val name = channelBuilder?.toString()?.trim()?.takeIf(String::isNotBlank)
                    val id = channelId
                    if (name != null && id != null) {
                        channelNames.getOrPut(id) { mutableListOf() }.add(name)
                    }
                    channelBuilder = null
                }

                "channel" -> {
                    channelId = null
                    channelBuilder = null
                }

                "title" -> {
                    val title = titleBuilder?.toString()?.trim()?.takeIf(String::isNotBlank)
                    val builder = programmeBuilder
                    if (title != null && builder != null) {
                        if (builder.title.isBlank() || titleLanguage?.startsWith("es") == true) {
                            builder.title = title
                        }
                    }
                    titleBuilder = null
                    titleLanguage = null
                }

                "programme" -> {
                    programmeBuilder?.let { builder ->
                        if (builder.title.isNotBlank() && builder.startMs > 0L) {
                            programmes += EpgProgramme(
                                channelId = builder.channelId,
                                title = builder.title,
                                startMs = builder.startMs,
                                stopMs = builder.stopMs,
                            )
                        }
                    }
                    programmeBuilder = null
                }
            }
        }
    }

    private data class ProgrammeBuilder(
        val channelId: String,
        val startMs: Long,
        val stopMs: Long,
        var title: String = "",
    )

    private fun parseTime(value: String?): Long {
        val raw = value?.trim()?.takeIf(String::isNotBlank) ?: return 0L
        val patterns = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss z", "yyyyMMddHHmmss")

        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                format.parse(raw)?.let { return it.time }
            } catch (_: Exception) {
                // Try the next XMLTV variant.
            }
        }
        return 0L
    }
}
