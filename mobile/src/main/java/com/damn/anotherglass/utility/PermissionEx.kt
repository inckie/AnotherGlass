package com.damn.anotherglass.utility

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

// I wonder is there is less idiotic way to write it modern Android
@SuppressLint("InlinedApi")
fun ComponentActivity.createGPSPermissionLauncher(handler: (Boolean) -> Unit)
        : ActivityResultLauncher<String> {

    val requestBG = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> null
        else -> registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { handler(it) }
    }

    return registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (!it || null == requestBG) handler(it)
        else requestBG.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}
