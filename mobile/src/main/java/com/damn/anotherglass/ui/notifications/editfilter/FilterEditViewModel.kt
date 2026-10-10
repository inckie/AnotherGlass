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
import com.damn.anotherglass.ui.AppRoute
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import com.damn.anotherglass.utility.AppDetailsProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// wrapper to have stable ID for each condition item to preserve focus
data class FilterConditionVM(
    val id: String,
    val condition: FilterConditionItem,
)

class FilterEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val filterRepository: IFilterRepository,
    private val appDetailsProvider: AppDetailsProvider? = null
) : ViewModel() {

    // --- State for the filter being edited ---
    val filterId = mutableStateOf<String?>(null)
    val filterName = mutableStateOf("")
    val packageName = mutableStateOf("")
    val isFilterEnabled = mutableStateOf(true)
    val matchAllConditions = mutableStateOf(true) // true for AND, false for OR
    val conditions = mutableStateListOf<FilterConditionVM>()
    val filterAction = mutableStateOf(FilterAction.BLOCK) // Add state for the action, default to BLOCK

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
                val navPackageName = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_PACKAGE_NAME)
                    ?.urlDecode()
                if (!navPackageName.isNullOrBlank()) {
                    packageName.value = navPackageName
                }

                val navAppName = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_APP_NAME)
                    ?.urlDecode()

                val navTitle = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TITLE)
                    ?.urlDecode()

                val navText = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TEXT)
                    ?.urlDecode()

                val navTickerText = savedStateHandle.get<String>(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_TICKER_TEXT)
                    ?.urlDecode()

                ARG_TO_CONDITION_MAP.forEach { (argKey, conditionType) ->
                    savedStateHandle.get<String>(argKey)
                        ?.urlDecode()
                        ?.let { value ->
                            if (value.isNotBlank()) {
                                conditions.add(FilterConditionVM(
                                    id = UUID.randomUUID().toString(),
                                    FilterConditionItem(
                                        type = conditionType,
                                        value = value
                                    )
                                ))
                            }
                        }
                }

                val isOngoingFromNav: Boolean? =
                    if (savedStateHandle.contains(AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING)) {
                        val isOngoing: Boolean =
                            savedStateHandle[AppRoute.FilterEditScreen.FILTER_EDIT_ARG_IS_ONGOING]
                                ?: false
                        conditions.add(
                            FilterConditionVM(
                                id = UUID.randomUUID().toString(),
                                FilterConditionItem(
                                    type = ConditionType.IS_ONGOING_EQUALS,
                                    value = isOngoing.toString()
                                )
                            )
                        )
                        isOngoing
                    } else null

                val isFromNotification = conditions.isNotEmpty() || packageName.value.isNotEmpty() || !navAppName.isNullOrBlank()

                if (filterName.value.isBlank()) {
                    if (isFromNotification) {
                        filterName.value = suggestFilterName(
                            appName = navAppName,
                            packageName = packageName.value.takeIf { it.isNotBlank() },
                            title = navTitle
                        )
                    } else {
                        filterName.value = "New Filter"
                    }
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
                conditions.addAll(loadedFilter.conditions.mapIndexed {
                    index, condition ->
                    FilterConditionVM(
                        id = UUID.randomUUID().toString(),
                        condition = condition
                    )
                })
                filterAction.value = loadedFilter.action // Load the action
            }
    }

    fun addCondition() {
        conditions.add(
            FilterConditionVM(
                id = UUID.randomUUID().toString(),
                FilterConditionItem(type = ConditionType.TITLE_CONTAINS, value = "")
            )
        )
    }

    fun removeCondition(item: FilterConditionVM) {
        conditions.remove(item)
    }

    fun updateConditionType(index: Int, newType: ConditionType) {
        if (index in conditions.indices) {
            val item = conditions[index]
            when {
                // If changing from IS_ONGOING_EQUALS, reset value to empty string
                item.condition.type == ConditionType.IS_ONGOING_EQUALS && newType != ConditionType.IS_ONGOING_EQUALS ->
                    conditions[index] = item.copy(condition = item.condition.copy(type = newType, value = ""))
                // If changing to IS_ONGOING_EQUALS, reset value to false
                item.condition.type != ConditionType.IS_ONGOING_EQUALS && newType == ConditionType.IS_ONGOING_EQUALS -> {
                    conditions[index] = item.copy(condition = item.condition.copy(type = newType, value = "false"))
                }
                else -> conditions[index] = item.copy(condition = item.condition.copy(type = newType))
            }
        }
    }

    fun updateConditionValue(index: Int, newValue: String) {
        if (index in conditions.indices) {
            val currentItem = conditions[index]
            conditions[index] = currentItem.copy(condition = currentItem.condition.copy(value = newValue))
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
                conditions = conditions.map { it.condition },
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

    fun suggestFilterName(
        appName: String? = null,
        packageName: String? = null,
        title: String? = null
    ): String {
        val resolvedAppName = resolveAppName(appName, packageName)
        val subject = title?.let { cleanTitle(it, resolvedAppName) }

        val rawName = when {
            resolvedAppName != null && subject != null -> "$resolvedAppName: $subject"
            resolvedAppName != null -> resolvedAppName
            subject != null -> subject
            else -> "New Filter"
        }

        return truncateToLength(rawName, maxLength = 20)
    }

    private fun resolveAppName(appName: String?, packageName: String?): String? {
        val providerName = packageName?.let { appDetailsProvider?.getAppDetails(it)?.appName?.trim() }
        val candidates = listOfNotNull(
            appName?.trim()?.takeIf { it.isNotBlank() },
            providerName?.takeIf { it.isNotBlank() }
        )

        // 1. Highest priority: candidate that does not equal packageName and does not contain '.'
        val friendly = candidates.firstOrNull { it != packageName && !it.contains('.') }
        if (friendly != null) return friendly

        // 2. Candidate that differs from packageName
        val distinct = candidates.firstOrNull { it != packageName }
        if (distinct != null) return distinct

        // 3. Fallback: format package name into a friendly title-cased name (e.g. com.whatsapp -> Whatsapp)
        val rawPkg = packageName ?: appName
        return rawPkg?.let { formatPackageNameFallback(it) }
    }

    private fun cleanTitle(title: String, appName: String?): String? {
        val firstLine = title.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null
        if (appName.isNullOrBlank()) return firstLine
        if (firstLine.equals(appName, ignoreCase = true)) return null

        val stripped = when {
            firstLine.startsWith("$appName: ", ignoreCase = true) ->
                firstLine.substring(appName.length + 2).trim()
            firstLine.startsWith("$appName - ", ignoreCase = true) ->
                firstLine.substring(appName.length + 3).trim()
            firstLine.startsWith("$appName ", ignoreCase = true) ->
                firstLine.substring(appName.length + 1).trim()
            else -> firstLine
        }
        return stripped.takeIf { it.isNotBlank() }
    }

    private fun truncateToLength(text: String, maxLength: Int = 20): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 1).trimEnd() + "…"
        } else {
            text
        }
    }

    private fun formatPackageNameFallback(pkg: String): String {
        val segment = pkg.substringAfterLast('.').takeIf { it.isNotBlank() } ?: pkg
        return segment.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
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
                        IFilterRepository.from(context),
                        AndroidAppDetailsProvider(context)
                    ) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
