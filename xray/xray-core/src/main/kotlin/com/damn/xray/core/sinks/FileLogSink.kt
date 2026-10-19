package com.damn.xray.core.sinks

import com.damn.xray.core.Event
import com.damn.xray.core.ISink
import com.damn.xray.core.formatting.event.IEventFormatter
import com.damn.xray.core.formatting.event.PlainTextEventFormatter
import java.io.File
import java.io.FileOutputStream

/**
 * Synchronous file log sink that writes, flushes, and syncs each event immediately
 * on the calling thread in strict order so log entries survive subsequent crashes.
 */
open class FileLogSink @JvmOverloads constructor(
    val file: File,
    val fileSizeLimit: Long = -1L,
    @Volatile var formatter: IEventFormatter = PlainTextEventFormatter(),
) : ISink {

    @Synchronized
    fun setFormatter(formatter: IEventFormatter): FileLogSink {
        this.formatter = formatter
        return this
    }

    @Synchronized
    override fun log(event: Event) {
        val formatted = formatter.format(event)
        val append = !(fileSizeLimit > 0 && file.exists() && file.length() > fileSizeLimit)
        file.parentFile?.mkdirs()
        FileOutputStream(file, append).use { stream ->
            stream.write(formatted.toByteArray(Charsets.UTF_8))
            stream.flush()
            stream.fd.sync()
        }
    }
}
