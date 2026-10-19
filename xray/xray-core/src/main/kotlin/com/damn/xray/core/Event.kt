package com.damn.xray.core

data class Event @JvmOverloads constructor(
    val loggerName: String,
    val tag: String,
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.DEBUG,
    val message: String = "",
    val data: Map<String, Any?>? = null,
    val context: Map<String, Any?>? = null,
    val exception: Throwable? = null,
) {
    val levelInt: Int
        get() = level.level

    constructor(
        loggerName: String,
        tag: String,
        timestamp: Long,
        level: Int,
        message: String,
        data: Map<String, Any?>?,
        context: Map<String, Any?>?,
        exception: Throwable?,
    ) : this(
        loggerName = loggerName,
        tag = tag,
        timestamp = timestamp,
        level = LogLevel.fromLevel(level),
        message = message,
        data = data,
        context = context,
        exception = exception,
    )
}
