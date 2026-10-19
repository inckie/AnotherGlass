package com.damn.xray.core.formatting.message

class BasicMessageFormatter : IMessageFormatter {
    override fun format(
        template: String,
        outParameters: MutableMap<String, Any?>?,
        vararg args: Any?,
    ): String {
        return if (args.isEmpty()) template else String.format(template, *args)
    }
}
