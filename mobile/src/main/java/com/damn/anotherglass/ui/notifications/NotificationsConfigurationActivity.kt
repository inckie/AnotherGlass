package com.damn.anotherglass.ui.notifications

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
import com.damn.anotherglass.ui.theme.AnotherGlassTheme
import java.net.URLEncoder

fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

class NotificationsConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // can also just put to context
        val importExportController = IImportExportController.fromActivity(this)

        setContent {
            AnotherGlassTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.FilterList.route,
                    modifier = Modifier.fillMaxSize() // NavHost should typically fill the space
                ) {
                    navGraph(this, navController, importExportController)
                }
            }
        }
    }
}
