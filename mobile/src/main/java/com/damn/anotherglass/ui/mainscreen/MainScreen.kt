package com.damn.anotherglass.ui.mainscreen

import android.content.Intent
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.anotherglass.R
import com.damn.anotherglass.core.Settings
import com.damn.anotherglass.debug.DbgNotifications
import com.damn.anotherglass.logging.LogActivity
import com.damn.anotherglass.ui.theme.AnotherGlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settings: SettingsController) {
    val context = LocalContext.current
    val isServiceRunning by settings.isServiceRunning.observeAsState()
    val hostMode by settings.hostMode.observeAsState()

    // Use function to make sure it won't build if enum is updated
    @DrawableRes
    fun resolveIcon(mode: Settings.HostMode) = when (mode) {
        Settings.HostMode.WiFi -> R.drawable.ic_wifi_24
        Settings.HostMode.Bluetooth -> R.drawable.ic_bluetooth_24
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AnotherGlass") },
                colors = TopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    scrolledContainerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                actions = {
                    TopAppBarDropdownMenu(listOf(
                        DropDownMenuItem("X-Ray") {
                            context.startActivity(Intent(context, LogActivity::class.java))
                        }
                    ))
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween, // Distribute space between items
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.lbl_connection_type))
                MultiChoiceSegmentedButtonRow {
                    Settings.HostMode.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            enabled = !isServiceRunning!!,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = Settings.HostMode.entries.size
                            ),
                            checked = hostMode == option,
                            onCheckedChange = { settings.setHostMode(option) },
                            label = { Text("") },
                            icon = {
                                Icon(
                                    painter = painterResource(id = resolveIcon(option)),
                                    contentDescription = option.name
                                )
                            }
                        )
                    }
                }
            }

            // Service Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween, // Distribute space between items
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.lbl_enable_service))
                Switch(
                    checked = isServiceRunning!!,
                    onCheckedChange = { isChecked ->
                        settings.setServiceRunning(isChecked)
                    }
                )
            }

            if (isServiceRunning!!) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(weight = 1f, fill = false),
                ) {
                    OptionToggles(settings)
                }
            }
        }
    }
}

@Composable
private fun OptionToggles(
    settings: SettingsController
) {
    val context = LocalContext.current

    val isGPSEnabled by settings.isGPSEnabled.observeAsState()
    val isNotificationsEnabled by settings.notificationsEnabled.observeAsState()

    val removedNotificationEnabled =
        remember { mutableStateOf(DbgNotifications.notificationId > 0) }

    // GPS Toggle
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(id = R.string.lbl_enable_gps))
        Switch(
            checked = isGPSEnabled!!,
            onCheckedChange = {
                settings.setGPSEnabled(it)
            }
        )
    }

    // Notifications Toggle
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.lbl_enable_notifications))
            Switch(
                checked = isNotificationsEnabled!!,
                onCheckedChange = { settings.setNotificationsEnabled(it) }
            )
        }
    }

    // WiFi Connect Button
    Button(
        onClick = { (context as MainActivity).connectWiFi() }
    ) {
        Text(stringResource(id = R.string.btn_connect_wifi))
    }

    // Debug Buttons
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            stringResource(id = R.string.lbl_dbg_notifications),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = {
                removedNotificationEnabled.value = DbgNotifications.removeNotification(context)
            },
            enabled = removedNotificationEnabled.value,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_remove_24),
                    contentDescription = "Remove Notification"
                )
            }
        )
        IconButton(
            onClick = {
                DbgNotifications.postNotification(context)
                removedNotificationEnabled.value = true
            },
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_add_24),
                    contentDescription = "Post Notification"
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AnotherGlassTheme {
        MainScreen(object : SettingsController() {
            override fun setServiceRunning(checked: Boolean) = Unit
            override fun setHostMode(mode: Settings.HostMode) = Unit
            override fun setGPSEnabled(enabled: Boolean) = Unit
            override fun setNotificationsEnabled(enabled: Boolean) = Unit
        })
    }
}