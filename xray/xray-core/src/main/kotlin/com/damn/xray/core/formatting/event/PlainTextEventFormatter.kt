package com.damn.xray.core.formatting.event

import com.damn.xray.core.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlainTextEventFormatter : IEventFormatter {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    override fun format(event: Event): String {
        val dateStr = synchronized(dateFormat) {
            dateFormat.format(Date(event.timestamp))
        }
        val sb = StringBuilder()
            .append(dateStr).append(" ")
            .append(event.level.name).append(" ")
            .append(event.tag).append(" ")
            .append(event.loggerName).append(" ")
            .append(event.message)
            .append("\n")

        val data = event.data
        if (!data.isNullOrEmpty()) {
            sb.append("\n\tdata: ").append(data).append("\n")
        }
        val context = event.context
        if (!context.isNullOrEmpty()) {
            sb.append("\n\tcontext: ").append(context).append("\n")
        }
        event.exception?.let { exc ->
            sb.append("\n\texception: ").append(exc.stackTraceToString()).append("\n")
        }
        return sb.toString()
    }
}
