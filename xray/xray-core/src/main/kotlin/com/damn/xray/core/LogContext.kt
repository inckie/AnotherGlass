package com.damn.xray.core

open class LogContext @JvmOverloads constructor(
    logContext: Map<String, Any?> = emptyMap()
) : HashMap<String, Any?>(logContext) {

    open fun retrieve(): Map<String, Any?> = HashMap(this)
}
