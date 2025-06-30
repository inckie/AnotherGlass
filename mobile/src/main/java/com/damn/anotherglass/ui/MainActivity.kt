package com.damn.anotherglass.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.damn.anotherglass.extensions.notifications.filter.IImportExportController
import com.damn.anotherglass.extensions.notifications.filter.fromActivity
import com.damn.anotherglass.ui.mainscreen.ServiceController
import com.damn.anotherglass.ui.theme.AnotherGlassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // can also just put to context
        val importExportController = IImportExportController.Companion.fromActivity(this)
        val serviceController = ServiceController(this)

        setContent {
            AnotherGlassTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.MainScreen.route,
                    modifier = Modifier.Companion.fillMaxSize() // NavHost should typically fill the space
                ) {
                    navGraph(
                        builder = this,
                        navController = navController,
                        importExportController = importExportController,
                        coreController = serviceController
                    )
                }
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}