package com.andres3336.streaming.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkPolicy {

    /**
     * WiFi-only is interpreted as "not mobile": WiFi and Ethernet are allowed,
     * which matches a TV that may be wired.
     */
    fun isAllowed(context: Context, wifiOnly: Boolean): Boolean {
        if (!wifiOnly) return true

        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
