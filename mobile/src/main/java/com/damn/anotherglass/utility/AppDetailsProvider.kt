package com.damn.anotherglass.utility

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppDetails(val appName: String, val icon: Drawable?)

interface AppDetailsProvider {
    fun getAppDetails(packageName: String): AppDetails
}

class AndroidAppDetailsProvider(private val context: Context) : AppDetailsProvider {
    override fun getAppDetails(packageName: String): AppDetails {
        val pm = context.packageManager
        return try {
            val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AppDetails(appName, icon)
        } catch (e: PackageManager.NameNotFoundException) {
            AppDetails(packageName, null) // Return package name if not found
        }
    }
}
