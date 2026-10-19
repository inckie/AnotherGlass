package com.damn.xray.core.contexts

import com.damn.xray.core.LogContext

class ThreadContext @JvmOverloads constructor(
    logContext: Map<String, Any?> = emptyMap()
) : LogContext(logContext) {

    @Suppress("DEPRECATION")
    override fun retrieve(): Map<String, Any?> {
        return super.retrieve().toMutableMap().apply {
            val t = Thread.currentThread()
            put("thread_id", t.id)
            put("thread_name", t.name)
        }
    }
}
