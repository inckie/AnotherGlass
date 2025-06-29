package com.damn.anotherglass.extensions.notifications.filter

import android.content.Context
import com.damn.anotherglass.shared.notifications.NotificationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


class NotificationFilterChecker(private val context: Context) {

    private var activeFilters: List<NotificationFilter> = emptyList()

    init {
        // In a real ViewModel, you'd collect the flow and update activeFilters
        // For now, let's imagine a way to load them initially or on demand
        CoroutineScope(Dispatchers.IO).launch {
            UserFilterRepository.getFiltersFlow(context).collect { filters ->
                activeFilters = filters.filter { it.isEnabled }
            }
        }
    }

    suspend fun filter(notification: NotificationData): FilterAction? {
        if (notification.action != NotificationData.Action.Posted) {
            // Send all non-Posted notifications by default, it won't hurt
            // later we can track if notification was sent or not, and ignore removals
            return null
        }

        val currentActiveFilters = UserFilterRepository.getFiltersFlow(context)
            .firstOrNull()?.filter { it.isEnabled } ?: emptyList()

        if (currentActiveFilters.isEmpty()) {
            return null
        }

        return currentActiveFilters.firstOrNull {
            matchesSingleFilter(notification, it)
        }?.action
    }

    private fun matchesSingleFilter(
        notification: NotificationData,
        filter: NotificationFilter
    ): Boolean {
        // If a package name is specified in the filter, it must match.
        if (!filter.packageName.isNullOrBlank()) {
            if (!notification.packageName.equals(filter.packageName, ignoreCase = true)) {
                return false // Package name does not match, so the filter does not apply.
            }
        }

        if (filter.conditions.isEmpty() && filter.isEnabled) {
            return true // Enabled filter with no conditions matches all (for the given package).
        }

        var overallMatch = filter.matchAllConditions

        for (condition in filter.conditions) {
            val conditionMet: Boolean = when (condition.type) {
                ConditionType.TITLE_CONTAINS ->
                    notification.title?.contains(condition.value, ignoreCase = true) ?: false
                ConditionType.TITLE_EQUALS ->
                    notification.title?.equals(condition.value, ignoreCase = true) ?: condition.value.isEmpty()
                ConditionType.TEXT_CONTAINS ->
                    notification.text?.contains(condition.value, ignoreCase = true) ?: false
                ConditionType.TEXT_EQUALS ->
                    notification.text?.equals(condition.value, ignoreCase = true) ?: condition.value.isEmpty()
                ConditionType.TICKER_TEXT_CONTAINS ->
                    notification.tickerText?.contains(condition.value, ignoreCase = true) ?: false
                ConditionType.TICKER_TEXT_EQUALS ->
                    notification.tickerText?.equals(condition.value, ignoreCase = true) ?: condition.value.isEmpty()
                ConditionType.IS_ONGOING_EQUALS ->
                    notification.isOngoing == condition.value.toBoolean()
            }

            if (filter.matchAllConditions) { // AND
                if (!conditionMet) return false
                overallMatch = true
            } else { // OR
                if (conditionMet) return true
                overallMatch = false
            }
        }
        return overallMatch
    }
}