package com.damn.xray.core.utility

object Stack {
    private const val SELF_PACKAGE_PREFIX = "com.damn.xray."

    @JvmStatic
    fun getClientCodeFrameOffset(stackTrace: Array<StackTraceElement>): Int {
        var reachedSelf = false
        for (i in stackTrace.indices) {
            val element = stackTrace[i]
            if (element.className.startsWith(SELF_PACKAGE_PREFIX)) {
                reachedSelf = true
                continue
            }
            if (reachedSelf) {
                return i
            }
        }
        return 0
    }
}
