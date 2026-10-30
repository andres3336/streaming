package com.andres3336.streaming.data.settings

import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, per-device overrides. Only the keys the user changed are stored.
 */
data class DeviceSettings(
    val bufferMs: Int? = null,
    val preBufferMs: Int? = null,
    val qualityCap: String? = null,
    val qualityPreference: String? = null,
    val aspectMode: String? = null,
    val liveLowLatency: Boolean? = null,
    val cacheSizeMb: Int? = null,
    val clearOnStart: Boolean? = null,
    val wifiOnly: Boolean? = null,
    val maxBitrateKbps: Int? = null,
    val textScale: Float? = null,
    val uiScale: Float? = null,
    val navigation: String? = null,
    val playerBar: String? = null,
    val playerButtons: List<String>? = null,
    val autoDownloadSubtitles: Boolean? = null,
    val hideEmptyCategories: Boolean? = null,
    val minCategoryItems: Int? = null,
    val autoStartOnBoot: Boolean? = null,
    val autoResumeOnWake: Boolean? = null,
    val useAsLauncher: Boolean? = null,
) {

    val isEmpty: Boolean
        get() = bufferMs == null &&
            preBufferMs == null &&
            qualityCap == null &&
            qualityPreference == null &&
            aspectMode == null &&
            liveLowLatency == null &&
            cacheSizeMb == null &&
            clearOnStart == null &&
            wifiOnly == null &&
            maxBitrateKbps == null &&
            textScale == null &&
            uiScale == null &&
            navigation == null &&
            playerBar == null &&
            playerButtons == null &&
            autoDownloadSubtitles == null &&
            hideEmptyCategories == null &&
            minCategoryItems == null &&
            autoStartOnBoot == null &&
            autoResumeOnWake == null &&
            useAsLauncher == null

    fun toJson(): JSONObject = JSONObject().apply {
        bufferMs?.let { put("bufferMs", it) }
        preBufferMs?.let { put("preBufferMs", it) }
        qualityCap?.let { put("qualityCap", it) }
        qualityPreference?.let { put("qualityPreference", it) }
        aspectMode?.let { put("aspectMode", it) }
        liveLowLatency?.let { put("liveLowLatency", it) }
        cacheSizeMb?.let { put("cacheSizeMb", it) }
        clearOnStart?.let { put("clearOnStart", it) }
        wifiOnly?.let { put("wifiOnly", it) }
        maxBitrateKbps?.let { put("maxBitrateKbps", it) }
        textScale?.let { put("textScale", it) }
        uiScale?.let { put("uiScale", it) }
        navigation?.let { put("navigation", it) }
        playerBar?.let { put("playerBar", it) }
        playerButtons?.let { list ->
            put("playerButtons", JSONArray().apply { list.forEach(::put) })
        }
        autoDownloadSubtitles?.let { put("autoDownloadSubtitles", it) }
        hideEmptyCategories?.let { put("hideEmptyCategories", it) }
        minCategoryItems?.let { put("minCategoryItems", it) }
        autoStartOnBoot?.let { put("autoStartOnBoot", it) }
        autoResumeOnWake?.let { put("autoResumeOnWake", it) }
        useAsLauncher?.let { put("useAsLauncher", it) }
    }

    companion object {
        fun fromJson(json: JSONObject): DeviceSettings = DeviceSettings(
            bufferMs = json.intOrNull("bufferMs"),
            preBufferMs = json.intOrNull("preBufferMs"),
            qualityCap = json.stringOrNull("qualityCap"),
            qualityPreference = json.stringOrNull("qualityPreference"),
            aspectMode = json.stringOrNull("aspectMode"),
            liveLowLatency = json.booleanOrNull("liveLowLatency"),
            cacheSizeMb = json.intOrNull("cacheSizeMb"),
            clearOnStart = json.booleanOrNull("clearOnStart"),
            wifiOnly = json.booleanOrNull("wifiOnly"),
            maxBitrateKbps = json.intOrNull("maxBitrateKbps"),
            textScale = json.floatOrNull("textScale"),
            uiScale = json.floatOrNull("uiScale"),
            navigation = json.stringOrNull("navigation"),
            playerBar = json.stringOrNull("playerBar"),
            playerButtons = json.stringListOrNull("playerButtons"),
            autoDownloadSubtitles = json.booleanOrNull("autoDownloadSubtitles"),
            hideEmptyCategories = json.booleanOrNull("hideEmptyCategories"),
            minCategoryItems = json.intOrNull("minCategoryItems"),
            autoStartOnBoot = json.booleanOrNull("autoStartOnBoot"),
            autoResumeOnWake = json.booleanOrNull("autoResumeOnWake"),
            useAsLauncher = json.booleanOrNull("useAsLauncher"),
        )
    }
}

private fun JSONObject.intOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.floatOrNull(name: String): Float? =
    if (has(name) && !isNull(name)) optDouble(name).toFloat() else null

private fun JSONObject.stringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) {
        optString(name).takeIf(String::isNotBlank)
    } else {
        null
    }

private fun JSONObject.booleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null

private fun JSONObject.stringListOrNull(name: String): List<String>? =
    if (has(name) && !isNull(name)) {
        val array = optJSONArray(name) ?: return null
        (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf(String::isNotBlank)
        }
    } else {
        null
    }
