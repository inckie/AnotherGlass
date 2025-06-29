package com.damn.anotherglass.ui.notifications.editfilter

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.damn.anotherglass.extensions.notifications.filter.ConditionType
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.FilterConditionItem
import com.damn.anotherglass.extensions.notifications.filter.IFilterRepository
import com.damn.anotherglass.extensions.notifications.filter.NotificationFilter
import com.damn.anotherglass.extensions.notifications.filter.from
import com.damn.anotherglass.ui.notifications.AppRoute
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FilterEditViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val filterRepository: IFilterRepository
) : ViewModel() {

    // --- State for the filter being edited ---
    val filterId = mutableStateOf<String?>(null)
    val filterName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val isFilterEnabled = mutableStateOf(true)
    val matchAllConditions = mutableStateOf(true) // true for AND, false for OR
    val conditions = mutableStateListOf<FilterConditionItem>()
    val filterAction =
        mutableStateOf(FilterAction.BLOCK) // Add state for the action, default to BLOCK

    val availableConditionTypes: List<ConditionType> = ConditionType.entries
    val availableFilterActions: List<FilterAction> = FilterAction.entries

    private var isNewFilter = true

    init {
        viewModelScope.launch {
            val existingFilterId: String? = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_FILTER_ID)?.urlDecode()
            if (existingFilterId != null) {
                isNewFilter = false
                filterId.value = existingFilterId
                loadFilter(existingFilterId)
            } else {
                isNewFilter = true

                // Set package name from navigation arguments
                savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_PACKAGE_NAME)
                    ?.urlDecode()
                    ?.let { value ->
                        if (value.isNotBlank()) {
                            packageName.value = value
                        }
                    }

                ARG_TO_CONDITION_MAP.forEach { (argKey, conditionType) ->
                    savedStateHandle.get<String>(argKey)
                        ?.urlDecode()
                        ?.let { value ->
                            if (value.isNotBlank()) {
                                conditions.add(
                                    FilterConditionItem(
                                        type = conditionType,
                                        value = value
                                    )
                                )
                            }
                        }
                }

                if (savedStateHandle.contains(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING)) {
                    val isOngoingFromNav: Boolean =
                        savedStateHandle[AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING]
                            ?: false
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
        }
    }

    private suspend fun loadFilter(id: String) {
        filterRepository.getFiltersFlow()
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
            when {
                // If changing from IS_ONGOING_EQUALS, reset value to empty string
                currentItem.type == ConditionType.IS_ONGOING_EQUALS && newType != ConditionType.IS_ONGOING_EQUALS ->
                    conditions[index] = currentItem.copy(type = newType, value = "")
                // If changing to IS_ONGOING_EQUALS, reset value to false
                currentItem.type != ConditionType.IS_ONGOING_EQUALS && newType == ConditionType.IS_ONGOING_EQUALS -> {
                    conditions[index] = currentItem.copy(type = newType, value = "false")
                }
                else -> conditions[index] = currentItem.copy(type = newType)
            }
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
            val filterToSave = NotificationFilter( // Assuming UserFilter is available
                id = filterId.value ?: UUID.randomUUID().toString(),
                name = filterName.value.ifBlank { "Untitled Filter" },
                packageName = packageName.value.ifBlank { null },
                isEnabled = isFilterEnabled.value,
                matchAllConditions = matchAllConditions.value,
                // todo: filter empty conditions
                conditions = ArrayList(conditions),
                action = filterAction.value
            )

            if (isNewFilter) {
                filterRepository.addFilter(filterToSave)
            } else {
                filterRepository.updateFilter(filterToSave)
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

        @OptIn(ExperimentalEncodingApi::class)
        fun String?.urlDecode(): String? =
            this?.let {
                Base64.Default.decode(it).decodeToString()
            }

        // URLEncoder has issues with % (it assumes its already encoded), so we use Base64 for URL encoding
        @OptIn(ExperimentalEncodingApi::class)
        fun String.urlEncode(): String = Base64.Default.encode(encodeToByteArray())

        class Factory(
            private val context: Context,
            private val savedStateHandle: SavedStateHandle
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(FilterEditViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return FilterEditViewModel(
                        savedStateHandle,
                        IFilterRepository.from(context)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
