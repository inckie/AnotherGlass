package com.damn.xray.android.adapters

import com.damn.xray.core.Logger

object ALog {
    private val defaultLog = Logger.get("ALog")

    @JvmStatic
    @JvmOverloads
    fun v(tag: String, message: String, throwable: Throwable? = null) = defaultLog.v(tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun d(tag: String, message: String, throwable: Throwable? = null) = defaultLog.d(tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun i(tag: String, message: String, throwable: Throwable? = null) = defaultLog.i(tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun w(tag: String, message: String, throwable: Throwable? = null) = defaultLog.w(tag, message, throwable)

    @JvmStatic
    @JvmOverloads
    fun e(tag: String, message: String, throwable: Throwable? = null) = defaultLog.e(tag, message, throwable)
}
