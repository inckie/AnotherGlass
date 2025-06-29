package com.damn.anotherglass.ui.notifications.filters

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.IFilterRepository
import com.damn.anotherglass.extensions.notifications.filter.NotificationFilter
import com.damn.anotherglass.extensions.notifications.filter.from
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import com.damn.anotherglass.utility.AppDetails
import com.damn.anotherglass.utility.AppDetailsProvider
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
    val action: FilterAction,
    val appDetails: AppDetails?
)

// todo: reorder, filter by app

class FilterListViewModel(
    private val appDetailsProvider: AppDetailsProvider,
    private val filterRepository: IFilterRepository) : ViewModel() {

    val filters: StateFlow<List<FilterListItemUI>> =
        filterRepository.getFiltersFlow()
            .map { userFilters ->
                userFilters.map { filter ->
                    val appDetails = filter.packageName?.let { appDetailsProvider.getAppDetails(it) }
                    FilterListItemUI(
                        id = filter.id,
                        name = filter.name,
                        isEnabled = filter.isEnabled,
                        description = formatFilterDescription(filter, appDetails),
                        action = filter.action, // New helper
                        appDetails = appDetails,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private fun formatFilterDescription(filter: NotificationFilter, appDetails: AppDetails?): String {
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
            filterRepository.deleteFilter(filterId)
        }
    }

    // Toggle enabled state directly without going to edit screen (optional feature)
    fun toggleFilterEnabled(filterId: String, currentEnabledState: Boolean) {
        viewModelScope.launch {
            val filtersList = filterRepository.getFiltersFlow().firstOrNull()
            filtersList?.find { it.id == filterId }?.let { filterToUpdate ->
                val updatedFilter = filterToUpdate.copy(isEnabled = !currentEnabledState)
                filterRepository.updateFilter(updatedFilter)
            }
        }
    }

    companion object {
        class Factory(private val context: Context) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FilterListViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FilterListViewModel(
                        AndroidAppDetailsProvider(context),
                        IFilterRepository.from(context)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
