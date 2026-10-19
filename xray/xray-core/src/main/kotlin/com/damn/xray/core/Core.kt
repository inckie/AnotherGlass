package com.damn.xray.core

import com.damn.xray.core.routing.ISinkFilter
import com.damn.xray.core.routing.Mapper

class Core {

    private val writeLock = Any()

    // LinkedHashMap retains key order
    private val internalSinks = LinkedHashMap<String, ISink>()

    @Volatile
    private var sinksSnapshot: Map<String, ISink> = emptyMap()

    private val mapper = Mapper(internalSinks)

    val rootLogger: Logger = Logger("", null, this)

    // are any logging classes allowed to throw exceptions to client
    var neverThrow: Boolean = false

    // region Loggers

    fun getLogger(): Logger = rootLogger

    fun getLogger(name: String): Logger = if (name.isEmpty()) rootLogger else rootLogger.getChild(name)

    // endregion

    // region Sinks management

    fun addSink(name: String, sink: ISink): Core {
        synchronized(writeLock) {
            if (internalSinks.put(name, sink) != null) {
                if (!neverThrow) {
                    throw IllegalStateException("Sink with name '$name' is already registered!")
                }
            }
            sinksSnapshot = LinkedHashMap(internalSinks)
        }
        return this
    }

    fun getSink(name: String): ISink? = sinksSnapshot[name]

    fun removeSink(sink: ISink) {
        synchronized(writeLock) {
            internalSinks.values.remove(sink)
            sinksSnapshot = LinkedHashMap(internalSinks)
        }
    }

    fun removeSink(sinkName: String) {
        synchronized(writeLock) {
            internalSinks.remove(sinkName)
            sinksSnapshot = LinkedHashMap(internalSinks)
        }
    }

    fun resolveSinks(
        hierarchyPaths: List<String>,
        loggerName: String,
        tag: String,
        level: LogLevel
    ): List<ISink> = mapper.resolveSinks(sinksSnapshot, hierarchyPaths, loggerName, tag, level)

    fun resolveSinks(
        loggerName: String,
        tag: String,
        level: LogLevel
    ): List<ISink> = mapper.resolveSinks(sinksSnapshot, Mapper.computeHierarchyPaths(loggerName), loggerName, tag, level)

    fun getMapping(event: Event): ArrayList<ISink> =
        ArrayList(resolveSinks(event.loggerName, event.tag, event.level))

    // early-exit filter
    fun hasSinks(loggerName: String, tag: String, level: LogLevel): Boolean =
        resolveSinks(loggerName, tag, level).isNotEmpty()

    fun hasSinks(loggerName: String, tag: String, level: Int): Boolean =
        hasSinks(loggerName, tag, LogLevel.fromLevel(level))

    fun setFilter(
        sinkName: String,
        loggerName: String,
        filter: ISinkFilter?
    ): Core {
        mapper.setFilter(loggerName, sinkName, filter)
        return this
    }

    // endregion

    @PublishedApi
    internal fun dispatch(event: Event, targetSinks: List<ISink>) {
        for (sink in targetSinks) {
            if (neverThrow) {
                try {
                    sink.log(event)
                } catch (_: Throwable) {
                    // Ignored when neverThrow is true
                }
            } else {
                sink.log(event)
            }
        }
    }

    fun submit(event: Event) = dispatch(event, resolveSinks(event.loggerName, event.tag, event.level))

    fun reset() {
        synchronized(writeLock) {
            internalSinks.clear()
            sinksSnapshot = emptyMap()
        }
        mapper.reset()
    }

    companion object {
        @JvmStatic
        val instance = Core()

        @JvmStatic
        fun get(): Core = instance
    }
}
