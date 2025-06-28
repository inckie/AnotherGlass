package com.damn.anotherglass.extensions.notifications.filter

import com.google.gson.annotations.SerializedName
import java.util.UUID


enum class ConditionType {
    @SerializedName("title_contains")
    TITLE_CONTAINS,

    @SerializedName("title_equals")
    TITLE_EQUALS,

    @SerializedName("text_contains")
    TEXT_CONTAINS,

    @SerializedName("text_equals")
    TEXT_EQUALS,

    @SerializedName("ticker_text_contains")
    TICKER_TEXT_CONTAINS,

    @SerializedName("ticker_text_equals")
    TICKER_TEXT_EQUALS,

    @SerializedName("is_ongoing")
    IS_ONGOING_EQUALS
}

data class FilterConditionItem(
    @SerializedName("type")
    val type: ConditionType, // Enum defined previously (TITLE_CONTAINS, etc.)
    @SerializedName("value")
    val value: String,
)

enum class FilterAction {
    @SerializedName("block")
    BLOCK,

    @SerializedName("allow_with_notification")
    ALLOW_WITH_NOTIFICATION,

    @SerializedName("allow_silently")
    ALLOW_SILENTLY
}

data class NotificationFilter(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(), // Unique ID for the filter
    @SerializedName("name")
    var name: String,
    @SerializedName("is_enabled")
    var isEnabled: Boolean = true,
    @SerializedName("package_name")
    var packageName: String? = null,
    @SerializedName("match_all_conditions")
    var matchAllConditions: Boolean = true, // true for AND, false for OR
    @SerializedName("conditions")
    var conditions: List<FilterConditionItem>,
    @SerializedName("action")
    var action: FilterAction = FilterAction.BLOCK // Default action
)

data class Filters(
    @SerializedName("filters")
    val filters: List<NotificationFilter> = emptyList()
)
