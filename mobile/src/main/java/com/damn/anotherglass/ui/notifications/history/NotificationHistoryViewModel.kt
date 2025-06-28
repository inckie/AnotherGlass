package com.damn.anotherglass.ui.notifications.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.damn.anotherglass.extensions.notifications.filter.NotificationHistoryRepository
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.ui.notifications.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHistoryViewModel : ViewModel() {

    private val _historyItems = MutableStateFlow<List<NotificationData>>(emptyList())

    val historyItems: StateFlow<List<NotificationData>> = _historyItems.asStateFlow()

    private val timeFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

    init {
        loadHistory()
    }

    fun setHistoryItems(items: List<NotificationData>) {
        _historyItems.value = items
    }

    fun loadHistory() {
        viewModelScope.launch { // Though getHistory is sync, keeping pattern for potential async
            // Filter for 'Posted' actions here if not already done in the repository's getHistory()
            _historyItems.value = NotificationHistoryRepository.getHistory()
                .filter { it.action == NotificationData.Action.Posted }
                .sortedByDescending { it.postedTime } // Show newest first
        }
    }

    fun formatTimestamp(timestamp: Long) = timeFormat.format(Date(timestamp))

    fun onCreateFilterFromNotification(notificationId: Int, navController: NavController?) {
        val notification = _historyItems.value.find { it.id == notificationId }
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
