package com.andres3336.streaming.data.config

import com.andres3336.streaming.data.settings.DeviceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigMergerTest {

    private val central = AppConfig(
        version = 2,
        sources = SourcesConfig(LiveMode.GIST),
        player = PlayerConfig(bufferMs = 3000, qualityCap = "auto", aspectMode = "fit"),
        cache = CacheConfig(sizeMb = 250),
        network = NetworkConfig(wifiOnly = false, maxBitrateKbps = 0),
    )

    @Test
    fun localOverridesWin() {
        val merged = ConfigMerger.apply(
            central,
            DeviceSettings(
                bufferMs = 5000,
                qualityCap = "720p",
                aspectMode = "fill",
                cacheSizeMb = 500,
                wifiOnly = true,
                maxBitrateKbps = 8000,
            ),
        )

        assertEquals(5000, merged.player.bufferMs)
        assertEquals("720p", merged.player.qualityCap)
        assertEquals("fill", merged.player.aspectMode)
        assertEquals(500, merged.cache.sizeMb)
        assertTrue(merged.network.wifiOnly)
        assertEquals(8000, merged.network.maxBitrateKbps)
    }

    @Test
    fun unsetKeysKeepCentralValues() {
        val merged = ConfigMerger.apply(central, DeviceSettings())

        assertEquals(3000, merged.player.bufferMs)
        assertEquals("auto", merged.player.qualityCap)
        assertEquals("fit", merged.player.aspectMode)
        assertEquals(250, merged.cache.sizeMb)
        assertFalse(merged.network.wifiOnly)
        assertEquals(0, merged.network.maxBitrateKbps)
    }

    @Test
    fun localFalseStillOverridesCentralTrue() {
        val centralWifiOnly = central.copy(network = central.network.copy(wifiOnly = true))

        val merged = ConfigMerger.apply(centralWifiOnly, DeviceSettings(wifiOnly = false))

        assertFalse(merged.network.wifiOnly)
    }

    @Test
    fun invalidLocalValuesAreIgnored() {
        val merged = ConfigMerger.apply(
            central,
            DeviceSettings(
                bufferMs = 999_999,
                qualityCap = "4k",
                aspectMode = "wide",
                cacheSizeMb = 999,
                maxBitrateKbps = -1,
            ),
        )

        assertEquals(3000, merged.player.bufferMs)
        assertEquals("auto", merged.player.qualityCap)
        assertEquals("fit", merged.player.aspectMode)
        assertEquals(250, merged.cache.sizeMb)
        assertEquals(0, merged.network.maxBitrateKbps)
    }
}
