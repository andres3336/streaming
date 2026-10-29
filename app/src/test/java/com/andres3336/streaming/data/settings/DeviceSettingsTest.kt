package com.andres3336.streaming.data.settings

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceSettingsTest {

    @Test
    fun roundTripsJson() {
        val settings = DeviceSettings(
            bufferMs = 5000,
            qualityCap = "720p",
            aspectMode = "fill",
            cacheSizeMb = 500,
            wifiOnly = true,
            maxBitrateKbps = 8000,
        )

        assertEquals(settings, DeviceSettings.fromJson(settings.toJson()))
    }

    @Test
    fun emptySettingsHaveNoKeys() {
        val settings = DeviceSettings()

        assertTrue(settings.isEmpty)
        assertEquals(0, settings.toJson().length())
        assertEquals(settings, DeviceSettings.fromJson(JSONObject("{}")))
    }

    @Test
    fun nonEmptySettingsReportNotEmpty() {
        assertFalse(DeviceSettings(bufferMs = 3000).isEmpty)
        assertFalse(DeviceSettings(wifiOnly = false).isEmpty)
    }

    @Test
    fun ignoresNullAndBlankValues() {
        val json = JSONObject(
            """{"bufferMs":null,"qualityCap":"","aspectMode":null,"cacheSizeMb":null}""",
        )

        assertEquals(DeviceSettings(), DeviceSettings.fromJson(json))
    }
}
