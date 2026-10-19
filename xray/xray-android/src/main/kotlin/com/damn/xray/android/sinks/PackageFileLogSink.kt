package com.damn.xray.android.sinks

import android.content.Context
import android.util.Log
import com.damn.xray.core.Event
import com.damn.xray.core.ISink
import com.damn.xray.core.formatting.event.IEventFormatter
import com.damn.xray.core.formatting.event.PlainTextEventFormatter
import java.io.File
import java.io.IOException

/*
  Android specific file log writer
 */
class PackageFileLogSink @JvmOverloads constructor(
    context: Context,
    val fileName: String,
    val fileSizeLimit: Long = -1L
) : ISink {

    private val ctx: Context = context.applicationContext
    private var formatter: IEventFormatter = PlainTextEventFormatter()

    val file: File
        get() = ctx.getFileStreamPath(fileName)

    @Synchronized
    fun setFormatter(formatter: IEventFormatter): PackageFileLogSink {
        this.formatter = formatter
        return this
    }

    private fun writeToFile(msg: String) {
        val f = file
        val mode = if (fileSizeLimit > 0 && f.exists() && f.length() > fileSizeLimit) {
            0
        } else {
            Context.MODE_APPEND
        }
        if (0 == mode) {
            Log.i(TAG, "File log $fileName was truncated due to reaching size limit $fileSizeLimit")
        }
        try {
            ctx.openFileOutput(fileName, mode).use { trace ->
                trace.write(msg.toByteArray(Charsets.UTF_8))
                trace.flush()
                trace.fd.sync()
            }
        } catch (ioe: IOException) {
            Log.e(TAG, "Error writing to $fileName", ioe)
        }
    }

    @Synchronized
    override fun log(event: Event) {
        writeToFile(formatter.format(event))
    }

    companion object {
        private const val TAG = "FileLogSink"
    }
}
