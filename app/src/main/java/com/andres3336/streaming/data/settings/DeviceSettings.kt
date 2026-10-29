package com.andres3336.streaming.data.settings

import org.json.JSONObject

/**
 * Local, per-device overrides. Only the keys the user changed are stored.
 */
data class DeviceSettings(
    val bufferMs: Int? = null,
    val qualityCap: String? = null,
    val aspectMode: String? = null,
    val cacheSizeMb: Int? = null,
    val wifiOnly: Boolean? = null,
    val maxBitrateKbps: Int? = null,
) {

    val isEmpty: Boolean
        get() = bufferMs == null &&
            qualityCap == null &&
            aspectMode == null &&
            cacheSizeMb == null &&
            wifiOnly == null &&
            maxBitrateKbps == null

    fun toJson(): JSONObject = JSONObject().apply {
        bufferMs?.let { put("bufferMs", it) }
        qualityCap?.let { put("qualityCap", it) }
        aspectMode?.let { put("aspectMode", it) }
        cacheSizeMb?.let { put("cacheSizeMb", it) }
        wifiOnly?.let { put("wifiOnly", it) }
        maxBitrateKbps?.let { put("maxBitrateKbps", it) }
    }

    companion object {
        fun fromJson(json: JSONObject): DeviceSettings = DeviceSettings(
            bufferMs = json.intOrNull("bufferMs"),
            qualityCap = json.stringOrNull("qualityCap"),
            aspectMode = json.stringOrNull("aspectMode"),
            cacheSizeMb = json.intOrNull("cacheSizeMb"),
            wifiOnly = json.booleanOrNull("wifiOnly"),
            maxBitrateKbps = json.intOrNull("maxBitrateKbps"),
        )
    }
}

private fun JSONObject.intOrNull(name: String): Int? =
    if (has(name) && !isNull(name)) optInt(name) else null

private fun JSONObject.stringOrNull(name: String): String? =
    if (has(name) && !isNull(name)) {
        optString(name).takeIf(String::isNotBlank)
    } else {
        null
    }

private fun JSONObject.booleanOrNull(name: String): Boolean? =
    if (has(name) && !isNull(name)) optBoolean(name) else null
