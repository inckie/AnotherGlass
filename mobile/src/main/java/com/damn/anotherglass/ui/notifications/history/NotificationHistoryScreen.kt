package com.damn.anotherglass.ui.notifications.history

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import com.damn.anotherglass.utility.AppDetails


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(
    navController: NavController?,
    viewModel: NotificationHistoryViewModel = viewModel(factory = NotificationHistoryViewModel.Companion.Factory(LocalContext.current.applicationContext as Application))
) {
    val historyItems by viewModel.historyItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History (Last 100)") },
                actions = {
                    IconButton(onClick = { viewModel.loadHistory() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh History")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No notification history available.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(
                    historyItems,
                    key = { it.notification.id.toString() + it.notification.postedTime }) { item -> // Key needs to be unique enough
                    NotificationHistoryItemView(
                        notification = item.notification,
                        appDetails = item.appDetails,
                        formattedTime = viewModel.formatTimestamp(item.notification.postedTime),
                        onCreateFilter = {
                            // Pass the original notification ID (sbn.id)
                            viewModel.onCreateFilterFromNotification(item.notification.id, navController)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

fun createNotificationData(
    action: NotificationData.Action,
    id: Int,
    packageName: String, // Nullable to match potential usage
    postedTime: Long,
    isOngoing: Boolean,
    title: String,
    text: String,
    tickerText: String
) = NotificationData().apply {
    this.action = action
    this.id = id
    this.packageName = packageName
    this.postedTime = postedTime
    this.isOngoing = isOngoing
    this.title = title
    this.text = text
    this.tickerText = tickerText
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun NotificationHistoryScreenPreview() {
    // Mock NavController for preview
    val mockNavController = null // Or a PreviewNavController if you have one

    // Sample data for previewing
    val sampleNotifications = listOf(
        NotificationHistoryItem(
            createNotificationData(
                NotificationData.Action.Posted,
                1,
                "com.app.chat",
                System.currentTimeMillis() - 10000,
                false,
                "New Message from Alice",
                "Hey, are you free for dinner tonight? Let me know what you think!",
                "Alice: Hey there!"
            ),
            AppDetails("Chat App", null)
        ),
        NotificationHistoryItem(
            createNotificationData(
                NotificationData.Action.Posted,
                2,
                "com.app.work",
                System.currentTimeMillis() - 60000,
                true,
                "Project Update: Task Overdue",
                "Task 'Finalize Report' is now overdue. Please update its status or complete it as soon as possible.",
                "Work: Task Overdue"
            ),
            AppDetails("Work App", null)
        )
    )

    // A Preview ViewModel or direct data for the preview
    val previewViewModel = NotificationHistoryViewModel(AndroidAppDetailsProvider(LocalContext.current)) // In a real preview, you might mock its state
    previewViewModel.setHistoryItems(sampleNotifications) // Directly set for preview

    MaterialTheme { // Replace with your app's theme
        NotificationHistoryScreen(navController = mockNavController, viewModel = previewViewModel)
    }
}
