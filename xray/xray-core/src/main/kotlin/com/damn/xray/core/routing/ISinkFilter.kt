package com.damn.xray.core.routing

import com.damn.xray.core.LogLevel

fun interface ISinkFilter {
    fun accept(loggerName: String, tag: String, level: LogLevel): Boolean

    fun accept(loggerName: String, tag: String, level: Int): Boolean =
        accept(loggerName, tag, LogLevel.fromLevel(level))
}

infix fun ISinkFilter.and(other: ISinkFilter): ISinkFilter = ISinkFilter { l, t, lv ->
    this.accept(l, t, lv) && other.accept(l, t, lv)
}

infix fun ISinkFilter.or(other: ISinkFilter): ISinkFilter = ISinkFilter { l, t, lv ->
    this.accept(l, t, lv) || other.accept(l, t, lv)
}

operator fun ISinkFilter.not(): ISinkFilter = ISinkFilter { l, t, lv ->
    !this.accept(l, t, lv)
}
