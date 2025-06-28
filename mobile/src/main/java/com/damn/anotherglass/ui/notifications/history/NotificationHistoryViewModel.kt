package com.damn.anotherglass.ui.notifications.history

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.damn.anotherglass.extensions.notifications.filter.NotificationHistoryRepository
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.ui.notifications.AppRoute
import com.damn.anotherglass.utility.AppDetails
import com.damn.anotherglass.utility.getAppDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationHistoryItem(
    val notification: NotificationData,
    val appDetails: AppDetails?
)
class NotificationHistoryViewModel(private val context: Context) : ViewModel() {

    private val _historyItems = MutableStateFlow<List<NotificationHistoryItem>>(emptyList())

    val historyItems: StateFlow<List<NotificationHistoryItem>> = _historyItems.asStateFlow()

    private val timeFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    init {
        loadHistory()
    }

    fun setHistoryItems(items: List<NotificationHistoryItem>) {
        _historyItems.value = items
    }

    fun loadHistory() {
        viewModelScope.launch { // Though getHistory is sync, keeping pattern for potential async
            _historyItems.value = NotificationHistoryRepository.getHistory()
                .filter { it.action == NotificationData.Action.Posted }
                .sortedByDescending { it.postedTime } // Show newest first
                .map { notification ->
                    val appDetails = notification.packageName?.let { getAppDetails(context, it) }
                    NotificationHistoryItem(notification, appDetails)
                }
        }
    }

    fun formatTimestamp(timestamp: Long) = timeFormat.format(Date(timestamp))

    fun onCreateFilterFromNotification(notificationId: Int, navController: NavController?) {
        val notification = _historyItems.value.find { it.notification.id == notificationId }?.notification
        notification?.let {
            val route = AppRoute.FilterEditScreen.buildFilterEditRoute(
                title = it.title,
                text = it.text,
                packageName = it.packageName,
                tickerText = it.tickerText,
                isOngoing = it.isOngoing
            )
            navController?.navigate(route)
        }
    }
}
