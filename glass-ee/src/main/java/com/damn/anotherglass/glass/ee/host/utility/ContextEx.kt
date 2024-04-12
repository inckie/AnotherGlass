package com.damn.anotherglass.glass.ee.host.utility

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat.checkSelfPermission

// copy of mobile project file,
// can't put it to the shared module due to API levels and dependencies on Explorer Edition

fun Context.hasPermission(permission: String): Boolean =
    checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
