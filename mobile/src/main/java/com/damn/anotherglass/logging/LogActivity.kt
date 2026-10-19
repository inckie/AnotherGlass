package com.damn.anotherglass.logging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.damn.anotherglass.App
import com.damn.anotherglass.ui.theme.AnotherGlassTheme
import com.damn.xray.core.LogLevel
import com.damn.xray.ui.XRayLogScreen

class LogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnotherGlassTheme {
                XRayLogScreen(
                    sinkName = App.memory_sink_name,
                    defaultLevel = LogLevel.INFO,
                    onBack = { finish() }
                )
            }
        }
    }
}