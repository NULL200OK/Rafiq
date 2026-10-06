package com.rafiq.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

object NetworkMonitor {

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    fun observe(context: Context, onChange: (Boolean) -> Unit): Watcher {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { onChange(true) }
            override fun onLost(network: Network) { onChange(isOnline(context)) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback)
        } else {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }
        return Watcher(cm, callback)
    }

    class Watcher internal constructor(
        private val cm: ConnectivityManager,
        private val callback: ConnectivityManager.NetworkCallback
    ) {
        fun stop() { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
}