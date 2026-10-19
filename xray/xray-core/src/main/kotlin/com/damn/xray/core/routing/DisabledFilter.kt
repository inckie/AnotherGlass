package com.damn.xray.core.routing

import com.damn.xray.core.LogLevel

class DisabledFilter : ISinkFilter {
    override fun accept(loggerName: String, tag: String, level: LogLevel): Boolean = false
}
