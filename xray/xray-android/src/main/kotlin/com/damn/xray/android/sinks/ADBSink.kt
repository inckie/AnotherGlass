package com.damn.xray.android.sinks

import android.util.Log
import com.damn.xray.android.formatters.ADBTextEventFormatter
import com.damn.xray.core.Event
import com.damn.xray.core.ISink
import com.damn.xray.core.formatting.event.IEventFormatter

class ADBSink(
    private var formatter: IEventFormatter = ADBTextEventFormatter()
) : ISink {

    fun setFormatter(formatter: IEventFormatter): ADBSink {
        this.formatter = formatter
        return this
    }

    override fun log(event: Event) {
        val priority = Log.VERBOSE + event.level.level
        var text = formatter.format(event)
        if (event.exception != null) {
            text += "\n" + Log.getStackTraceString(event.exception)
        }
        val tag = event.category.ifEmpty { "XRay" }
        Log.println(priority, tag, text)
    }
}
