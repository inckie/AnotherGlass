package com.damn.xray.core.formatting.message

fun interface IMessageFormatter {
    fun format(template: String, outParameters: MutableMap<String, Any?>?, vararg args: Any?): String
}
