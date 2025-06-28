package com.damn.anotherglass.extensions.notifications.filter

import java.util.UUID

enum class FilterAction {
    BLOCK,
    ALLOW,
    ALLOW_WITH_NOTIFICATION,
    ALLOW_SILENTLY
}

// For storing in DataStore (likely serialized to JSON)
data class UserFilter(
    val id: String = UUID.randomUUID().toString(), // Unique ID for the filter
    var name: String,
    var isEnabled: Boolean = true,
    var packageName: String? = null,
    var matchAllConditions: Boolean = true, // true for AND, false for OR
    val conditions: List<FilterConditionItem>,
    val action: FilterAction = FilterAction.BLOCK // Default action
)

data class FilterConditionItem(
    val type: ConditionType, // Enum defined previously (TITLE_CONTAINS, etc.)
    val value: String,
    val ignoreCase: Boolean = true // Good default for user filters
)

// Enum for condition types (same as before)
enum class ConditionType {
    TITLE_CONTAINS,
    TITLE_EQUALS,
    TEXT_CONTAINS,
    TEXT_EQUALS,
    TICKER_TEXT_CONTAINS,
    TICKER_TEXT_EQUALS,
    IS_ONGOING_EQUALS
}