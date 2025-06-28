package com.damn.anotherglass.ui.notifications.editfilter

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.damn.anotherglass.extensions.notifications.filter.ConditionType
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.FilterConditionItem
import com.damn.anotherglass.extensions.notifications.filter.UserFilter
import com.damn.anotherglass.extensions.notifications.filter.UserFilterRepository
import androidx.lifecycle.ViewModelProvider
import com.damn.anotherglass.ui.notifications.AppRoute
import com.damn.anotherglass.utility.AppDetails
import com.damn.anotherglass.utility.AppDetailsProvider
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.UUID

class FilterEditViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val appDetailsProvider: AppDetailsProvider
) : AndroidViewModel(application) {

    // --- State for the filter being edited ---
    val filterId = mutableStateOf<String?>(null)
    val filterName = mutableStateOf("")
    val packageName = mutableStateOf("")
    private val _appDetails = MutableStateFlow<AppDetails?>(null)
    val appDetails: StateFlow<AppDetails?> = _appDetails.asStateFlow()
    val isFilterEnabled = mutableStateOf(true)
    val matchAllConditions = mutableStateOf(true) // true for AND, false for OR
    val conditions = mutableStateListOf<FilterConditionItem>()
    val filterAction = mutableStateOf(FilterAction.BLOCK) // Add state for the action, default to BLOCK

    val availableConditionTypes: List<ConditionType> = ConditionType.entries
    val availableFilterActions: List<FilterAction> = FilterAction.entries

    private var isNewFilter = true

    init {
        viewModelScope.launch {
            val existingFilterId: String? =
                savedStateHandle[AppRoute.FilterEditScreen.FILTER_EDIT_ARG_FILTER_ID]
            if (existingFilterId != null) {
                isNewFilter = false
                filterId.value = existingFilterId
                loadFilter(existingFilterId)
            } else {
                isNewFilter = true

                // Set package name from navigation arguments
                savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_PACKAGE_NAME)
                    ?.urlDecodeFix()
                    ?.let { value ->
                        if (value.isNotBlank()) {
                            packageName.value = value
                        }
                    }

                ARG_TO_CONDITION_MAP.forEach { (argKey, conditionType) ->
                    savedStateHandle.get<String>(argKey)
                        ?.urlDecodeFix()
                        ?.let { value ->
                            if (value.isNotBlank()) {
                                conditions.add(FilterConditionItem(
                                        type = conditionType,
                                        value = value
                                    ))
                            }
                        }
                }

                if (savedStateHandle.contains(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING)) {
                    val isOngoingFromNav: Boolean = savedStateHandle[AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING] ?: false
                    conditions.add(
                        FilterConditionItem(
                            type = ConditionType.IS_ONGOING_EQUALS,
                            value = isOngoingFromNav.toString()
                        )
                    )
                }

                if ((conditions.isNotEmpty() || packageName.value.isNotEmpty()) && filterName.value.isBlank()) {
                    filterName.value = "New Filter from Notification"
                } else if (filterName.value.isBlank()) {
                    filterName.value = "New Filter"
                }
                filterAction.value = FilterAction.BLOCK // Or UserFilter().action
            }

            _appDetails.value = if (packageName.value.isNotBlank()) {
                appDetailsProvider.getAppDetails(packageName.value)
            } else {
                null
            }
        }
    }

    private suspend fun loadFilter(id: String) {
        UserFilterRepository.getFiltersFlow(getApplication())
            .firstOrNull()
            ?.find { it.id == id }
            ?.let { loadedFilter ->
                filterName.value = loadedFilter.name
                packageName.value = loadedFilter.packageName ?: ""
                isFilterEnabled.value = loadedFilter.isEnabled
                matchAllConditions.value = loadedFilter.matchAllConditions
                conditions.clear()
                conditions.addAll(loadedFilter.conditions)
                filterAction.value = loadedFilter.action // Load the action
            }
    }

    fun addCondition() {
        conditions.add(FilterConditionItem(type = ConditionType.TITLE_CONTAINS, value = ""))
    }

    fun removeCondition(item: FilterConditionItem) {
        conditions.remove(item)
    }

    fun updateConditionType(index: Int, newType: ConditionType) {
        if (index in conditions.indices) {
            val currentItem = conditions[index]
            conditions[index] = currentItem.copy(type = newType)
        }
    }

    fun updateConditionValue(index: Int, newValue: String) {
        if (index in conditions.indices) {
            val currentItem = conditions[index]
            conditions[index] = currentItem.copy(value = newValue)
        }
    }

    fun saveFilter(onSaved: () -> Unit) {
        viewModelScope.launch {
            val filterToSave = UserFilter( // Assuming UserFilter is available
                id = filterId.value ?: UUID.randomUUID().toString(),
                name = filterName.value.ifBlank { "Untitled Filter" },
                packageName = packageName.value.ifBlank { null },
                isEnabled = isFilterEnabled.value,
                matchAllConditions = matchAllConditions.value,
                conditions = ArrayList(conditions),
                action = filterAction.value
            )

            if (isNewFilter) {
                UserFilterRepository.addFilter(getApplication(), filterToSave)
            } else {
                UserFilterRepository.updateFilter(getApplication(), filterToSave)
            }
            onSaved()
        }
    }

    companion object {
        private val ARG_TO_CONDITION_MAP = mapOf(
            AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TITLE to ConditionType.TITLE_CONTAINS,
            AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TEXT to ConditionType.TEXT_CONTAINS,
            AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TICKER_TEXT to ConditionType.TICKER_TEXT_CONTAINS
            // AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING is handled separately due to Boolean type
        )

        private fun String?.urlDecodeFix(): String? =
            this?.let { URLDecoder.decode(it, "UTF-8").replace("+", " ") }

        class Factory(private val application: Application, private val savedStateHandle: SavedStateHandle) : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FilterEditViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FilterEditViewModel(application, savedStateHandle, AndroidAppDetailsProvider(application)) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}