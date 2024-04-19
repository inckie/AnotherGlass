package com.damn.anotherglass.glass.ee.host.utility

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.checkSelfPermission

// partial copy of mobile project file,
// can't put it to the shared module due to API levels and dependencies on Explorer Edition

fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.locationManager() =
    getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

inline fun <reified T : Service> Context.isRunning(): Boolean {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val services = activityManager.getRunningServices(Int.MAX_VALUE)
    val pkgname = packageName // in reality we will only see our own services anyway on Android 8+
    val srvname: String = T::class.java.getName()
    return services.any { pkgname == it.service.packageName && srvname == it.service.className && it.started }
}
