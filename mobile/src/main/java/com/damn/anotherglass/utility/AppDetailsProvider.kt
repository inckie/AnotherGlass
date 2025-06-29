package com.damn.anotherglass.utility

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.collection.LruCache

data class AppDetails(val appName: String, val icon: Drawable?)

interface AppDetailsProvider {
    fun getAppDetails(packageName: String): AppDetails
}

class AndroidAppDetailsProvider(private val context: Context) : AppDetailsProvider {

    private val cache: LruCache<String, AppDetails> = object : LruCache<String, AppDetails>(64) {
        override fun create(key: String): AppDetails? = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(key, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            AppDetails(appName, icon)
        } catch (e: PackageManager.NameNotFoundException) {
            null // unlikely case of a package uninstalled after posting a notification
        }
    }

    override fun getAppDetails(packageName: String): AppDetails =
        cache[packageName] ?: AppDetails(packageName, null)
}