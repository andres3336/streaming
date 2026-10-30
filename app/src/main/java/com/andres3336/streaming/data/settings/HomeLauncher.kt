package com.andres3336.streaming.data.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Enables the CATEGORY_HOME activity alias. This is the reliable auto-start path
 * on Android TV when background activity launches are blocked by the OS.
 */
object HomeLauncher {

    private const val ALIAS = ".HomeActivity"

    fun setEnabled(context: Context, enabled: Boolean) {
        val component = ComponentName(context.packageName, context.packageName + ALIAS)
        context.packageManager.setComponentEnabledSetting(
            component,
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP,
        )
    }

    fun isEnabled(context: Context): Boolean {
        val component = ComponentName(context.packageName, context.packageName + ALIAS)
        return context.packageManager.getComponentEnabledSetting(component) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
}
