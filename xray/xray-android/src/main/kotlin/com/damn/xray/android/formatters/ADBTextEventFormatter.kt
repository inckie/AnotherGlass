package com.damn.xray.android.formatters

import com.damn.xray.core.Event
import com.damn.xray.core.formatting.event.IEventFormatter

class ADBTextEventFormatter : IEventFormatter {

    override fun format(event: Event): String {
        val sb = StringBuilder()
        if (event.loggerName.isNotEmpty()) {
            sb.append("[${event.loggerName}] ")
        }
        sb.append(event.message)
        if (!event.data.isNullOrEmpty()) {
            sb.append("\n\tdata: ").append(event.data)
        }
        if (!event.context.isNullOrEmpty()) {
            sb.append("\n\tcontext: ").append(event.context)
        }
        return sb.toString()
    }
}
