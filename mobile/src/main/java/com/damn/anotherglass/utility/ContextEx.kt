package com.damn.anotherglass.utility

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission

fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

// Context getSystemService(Class<T>) is not available on API 21
inline fun <reified T> Context.getService(name: String): T =
    getSystemService(name) as T

// did not use reified type here because it is not supported in Java
fun <T : Service> Context.isServiceRunning(serviceClass: Class<T>): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    val services = activityManager.getRunningServices(Int.MAX_VALUE)

    val pkgname = packageName
    val srvname = serviceClass.name

    for (info in services) {
        if (pkgname == info.service.packageName)
            if (srvname == info.service.className)
                if (info.started)
                    return true
    }

    return false
}