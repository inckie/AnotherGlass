package com.damn.anotherglass.ui.notifications.filters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.UserFilter
import com.damn.anotherglass.extensions.notifications.filter.UserFilterRepository
import com.damn.anotherglass.utility.AppDetails
import com.damn.anotherglass.utility.AppDetailsProvider
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilterListItemUI( // A UI-specific model for the list
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val description: String,
    val actionDisplay: String,
    val appDetails: AppDetails?
)

class FilterListViewModel(
    application: Application,
    private val appDetailsProvider: AppDetailsProvider) : AndroidViewModel(application) {

    val filters: StateFlow<List<FilterListItemUI>> =
        UserFilterRepository.getFiltersFlow(application)
            .map { userFilters ->
                userFilters.map { filter ->
                    val appDetails = filter.packageName?.let { appDetailsProvider.getAppDetails(it) }
                    FilterListItemUI(
                        id = filter.id,
                        name = filter.name,
                        isEnabled = filter.isEnabled,
                        description = formatFilterDescription(filter, appDetails),
                        actionDisplay = filter.action.toDisplayStringList(), // New helper
                        appDetails = appDetails,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private fun formatFilterDescription(filter: UserFilter, appDetails: AppDetails?): String {
        val parts = mutableListOf<String>()

        appDetails?.let {
            parts.add("App: ${it.appName}")
        }

        val conditionCount = filter.conditions.size
        if (conditionCount > 0) {
            val matchType = if (filter.matchAllConditions) "All" else "Any"
            val plural = if (conditionCount == 1) "condition" else "conditions"
            parts.add("$conditionCount $plural ($matchType)")
        } else if (null == appDetails) {
            parts.add("No conditions")
        }

        return parts.joinToString(", ")
    }

    fun deleteFilter(filterId: String) {
        viewModelScope.launch {
            UserFilterRepository.deleteFilter(getApplication(), filterId)
        }
    }

    // Toggle enabled state directly without going to edit screen (optional feature)
    fun toggleFilterEnabled(filterId: String, currentEnabledState: Boolean) {
        viewModelScope.launch {
            val filtersList = UserFilterRepository.getFiltersFlow(getApplication()).firstOrNull()
            filtersList?.find { it.id == filterId }?.let { filterToUpdate ->
                val updatedFilter = filterToUpdate.copy(isEnabled = !currentEnabledState)
                UserFilterRepository.updateFilter(getApplication(), updatedFilter)
            }
        }
    }

    companion object {
        class Factory(private val application: Application) : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FilterListViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FilterListViewModel(application, AndroidAppDetailsProvider(application)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}

fun FilterAction?.toDisplayStringList(): String = when (this) {
    null -> "Action: Not Set"
    FilterAction.BLOCK -> "Action: Block"
    FilterAction.ALLOW -> "Action: Allow"
    FilterAction.ALLOW_WITH_NOTIFICATION -> "Action: Allow & Notify"
    FilterAction.ALLOW_SILENTLY -> "Action: Allow Silently"
}