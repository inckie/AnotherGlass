package com.damn.xray.core.routing

import com.damn.xray.core.ISink
import com.damn.xray.core.LogLevel
import com.damn.xray.core.Logger

/*
 Maps loggers to sinks using rules.
*/
class Mapper(private val sinks: Map<String, ISink?> = emptyMap()) {

    // map Logger hierarchy path -> (sink identifier -> sink filter)
    @Volatile
    private var rules: Map<String, Map<String, ISinkFilter>> = emptyMap()

    fun setFilter(
        loggerName: String,
        sinkName: String,
        filter: ISinkFilter?
    ) {
        synchronized(this) {
            val newRules = HashMap<String, MutableMap<String, ISinkFilter>>()
            for ((k, v) in rules) {
                newRules[k] = HashMap(v)
            }
            if (filter != null) {
                val sinkMap = newRules.getOrPut(loggerName) { HashMap() }
                sinkMap[sinkName] = filter
            } else {
                val sinkMap = newRules[loggerName]
                if (sinkMap != null) {
                    sinkMap.remove(sinkName)
                    if (sinkMap.isEmpty()) {
                        newRules.remove(loggerName)
                    }
                }
            }
            rules = newRules.mapValues { it.value.toMap() }.toMap()
        }
    }

    private fun getFilterForSink(
        currentRules: Map<String, Map<String, ISinkFilter>>,
        sinkName: String,
        hierarchyPaths: List<String>,
    ): ISinkFilter? {
        for (path in hierarchyPaths) {
            currentRules[path]?.get(sinkName)?.let { return it }
        }
        return null
    }

    fun resolveSinks(
        sinksSnapshot: Map<String, ISink?>,
        hierarchyPaths: List<String>,
        loggerName: String,
        tag: String,
        level: LogLevel,
    ): List<ISink> {
        if (sinksSnapshot.isEmpty()) return emptyList()
        val currentRules = rules
        var result: ArrayList<ISink>? = null
        for ((sinkName, sink) in sinksSnapshot) {
            if (sink == null) continue
            val filter = getFilterForSink(currentRules, sinkName, hierarchyPaths)
            if (filter == null || filter.accept(loggerName, tag, level)) {
                if (result == null) result = ArrayList(sinksSnapshot.size)
                result.add(sink)
            }
        }
        return result ?: emptyList()
    }

    fun getMapping(
        logger: String,
        tag: String,
        level: LogLevel
    ): Set<String> {
        val currentRules = rules
        val paths = computeHierarchyPaths(logger)
        val sinkKeys = synchronized(sinks) { ArrayList(sinks.keys) }
        val result = HashSet<String>()
        for (sinkName in sinkKeys) {
            val filter = getFilterForSink(currentRules, sinkName, paths)
            if (filter == null || filter.accept(logger, tag, level)) {
                result.add(sinkName)
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
        val currentRules = rules
        val paths = computeHierarchyPaths(loggerName)
        val sinkKeys = synchronized(sinks) { ArrayList(sinks.keys) }
        for (sinkName in sinkKeys) {
            val filter = getFilterForSink(currentRules, sinkName, paths)
            if (filter == null || filter.accept(loggerName, tag, level)) {
                return true
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
        synchronized(this) {
            rules = emptyMap()
        }
    }

    companion object {
        @JvmStatic
        fun computeHierarchyPaths(loggerName: String): List<String> {
            if (loggerName.isEmpty()) return listOf("")
            val paths = ArrayList<String>()
            var current = loggerName
            while (true) {
                paths.add(current)
                val idx = current.lastIndexOf(Logger.NameSeparator)
                if (idx <= 0) break
                current = current.substring(0, idx)
            }
            paths.add("")
            return paths
        }
    }
}
