package com.damn.anotherglass.utility

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission

fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

inline fun <reified T> Context.getService(name: String): T =
    getSystemService(name) as T
