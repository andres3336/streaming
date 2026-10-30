package com.andres3336.streaming.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorLogEntryTest {

    @Test
    fun roundTripsJson() {
        val entry = ErrorLogEntry(
            timestamp = 1_234L,
            channel = "Canal",
            code = "ERROR_CODE_IO_BAD_HTTP_STATUS",
            message = "El canal no está disponible en este momento.",
        )

        assertEquals(entry, ErrorLogEntry.fromJson(entry.toJson()))
    }

    @Test
    fun rejectsIncompleteEntries() {
        assertNull(ErrorLogEntry.fromJson(org.json.JSONObject("{}")))
        assertNull(
            ErrorLogEntry.fromJson(
                org.json.JSONObject("""{"channel":"Canal","code":"x"}"""),
            ),
        )
    }
}
