package com.damn.xray.core.routing

import com.damn.xray.core.ISink
import com.damn.xray.core.LogLevel
import com.damn.xray.core.Logger

/*
 Maps loggers to sinks using rules.
*/
class Mapper(private val sinks: Map<String, ISink?>) {

    // map Logger subsystem -> (sink identifier -> sink filter)
    private val loggerMapping: MutableMap<String, MutableMap<String, ISinkFilter>> = mutableMapOf()

    fun setFilter(
        loggerName: String,
        sinkName: String,
        filter: ISinkFilter?
    ) {
        synchronized(loggerMapping) {
            val mappedSinks = loggerMapping[loggerName]
            if (mappedSinks != null) {
                if (filter != null) {
                    mappedSinks[sinkName] = filter
                } else {
                    mappedSinks.remove(sinkName)
                    if (mappedSinks.isEmpty()) {
                        loggerMapping.remove(loggerName)
                    }
                }
                return
            }
            if (filter == null) {
                return
            }
            loggerMapping[loggerName] = mutableMapOf(sinkName to filter)
        }
    }

    private fun getFilterForSink(sinkName: String, logger: String): ISinkFilter? {
        // Check exact match
        loggerMapping[logger]?.get(sinkName)?.let { return it }
        if (logger.isEmpty()) return null

        // Look for any parent mapping
        var current = logger
        var i = current.lastIndexOf(Logger.NameSeparator)
        while (i > 0) {
            current = current.substring(0, i)
            loggerMapping[current]?.get(sinkName)?.let { return it }
            i = current.lastIndexOf(Logger.NameSeparator)
        }

        // Fall back to root logger mapping
        return loggerMapping[""]?.get(sinkName)
    }

    fun getMapping(
        logger: String,
        tag: String,
        level: LogLevel
    ): Set<String> {
        val sinkKeys = synchronized(sinks) {
            ArrayList(sinks.keys)
        }
        val result = HashSet<String>()
        synchronized(loggerMapping) {
            for (sinkName in sinkKeys) {
                val filter = getFilterForSink(sinkName, logger)
                if (filter == null || filter.accept(logger, tag, level)) {
                    result.add(sinkName)
                }
            }
        }
        return result
    }

    fun getMapping(
        logger: String,
        tag: String,
        level: Int
    ): Set<String> = getMapping(logger, tag, LogLevel.fromLevel(level))

    fun hasSinks(
        loggerName: String,
        tag: String,
        level: LogLevel
    ): Boolean {
        val sinkKeys = synchronized(sinks) {
            ArrayList(sinks.keys)
        }
        synchronized(loggerMapping) {
            for (sinkName in sinkKeys) {
                val filter = getFilterForSink(sinkName, loggerName)
                if (filter == null || filter.accept(loggerName, tag, level)) {
                    return true
                }
            }
        }
        return false
    }

    fun hasSinks(
        loggerName: String,
        tag: String,
        level: Int
    ): Boolean = hasSinks(loggerName, tag, LogLevel.fromLevel(level))

    fun reset() {
        synchronized(loggerMapping) {
            loggerMapping.clear()
        }
    }
}
