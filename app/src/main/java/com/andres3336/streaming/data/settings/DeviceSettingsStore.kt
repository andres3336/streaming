package com.andres3336.streaming.data.settings

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DeviceSettingsStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "device-settings.json")

    fun load(): DeviceSettings = runCatching {
        if (!file.exists()) DeviceSettings()
        else DeviceSettings.fromJson(JSONObject(file.readText()))
    }.getOrDefault(DeviceSettings())

    suspend fun save(settings: DeviceSettings) = withContext(Dispatchers.IO) {
        runCatching {
            if (settings.isEmpty) {
                file.delete()
                return@runCatching
            }

            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(settings.toJson().toString())
            if (!temp.renameTo(file)) {
                file.writeText(temp.readText())
                temp.delete()
            }
        }
    }

    fun clear() {
        file.delete()
    }
}
