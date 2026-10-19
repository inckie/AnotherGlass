package com.damn.xray.core.routing

import com.damn.xray.core.LogLevel

class DefaultSinkFilter(private val minLevel: LogLevel) : ISinkFilter {

    constructor(minLevel: Int) : this(LogLevel.fromLevel(minLevel))

    override fun accept(loggerName: String, tag: String, level: LogLevel): Boolean {
        return level.level >= minLevel.level
    }
}
