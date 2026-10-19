package com.damn.xray.core

import com.damn.xray.core.routing.ISinkFilter
import com.damn.xray.core.routing.Mapper

class Core {

    // LinkedHashMap retains key order
    private val sinks = LinkedHashMap<String, ISink>()

    private val mapper = Mapper(sinks)

    // are any logging classes allowed to throw exceptions to client
    var neverThrow: Boolean = false

    // region Sinks management

    fun addSink(name: String, sink: ISink): Core {
        synchronized(sinks) {
            if (sinks.put(name, sink) != null) {
                if (!neverThrow) {
                    throw IllegalStateException("Sink with name '$name' is already registered!")
                }
            }
        }
        return this
    }

    fun getSink(name: String): ISink? {
        synchronized(sinks) {
            return sinks[name]
        }
    }

    fun removeSink(sink: ISink) {
        synchronized(sinks) {
            sinks.values.remove(sink)
        }
    }

    fun removeSink(sinkName: String) {
        synchronized(sinks) {
            sinks.remove(sinkName)
        }
    }

    // early-exit filter
    fun hasSinks(loggerName: String, tag: String, level: LogLevel): Boolean {
        synchronized(mapper) {
            return mapper.hasSinks(loggerName, tag, level)
        }
    }

    fun hasSinks(loggerName: String, tag: String, level: Int): Boolean {
        synchronized(mapper) {
            return mapper.hasSinks(loggerName, tag, level)
        }
    }

    fun getMapping(event: Event): ArrayList<ISink> {
        val result = ArrayList<ISink>()
        val enabledSinks: Set<String> = synchronized(mapper) {
            mapper.getMapping(event.subsystem, event.category, event.level)
        }
        synchronized(sinks) {
            // keeping the sink order
            for ((key, value) in sinks) {
                if (enabledSinks.contains(key)) {
                    result.add(value)
                }
            }
        }
        return result
    }

    fun setFilter(
        sinkName: String,
        loggerName: String,
        filter: ISinkFilter?
    ): Core {
        // do not care if the sink already/still exists
        synchronized(mapper) {
            mapper.setFilter(loggerName, sinkName, filter)
        }
        return this
    }

    // endregion

    fun submit(event: Event) {
        val mapping = getMapping(event)
        if (mapping.isNotEmpty()) {
            for (sink in mapping) {
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
    }

    fun reset() {
        synchronized(sinks) {
            sinks.clear()
        }
        synchronized(mapper) {
            mapper.reset()
        }
    }

    companion object {
        @JvmStatic
        val instance = Core()

        @JvmStatic
        fun get(): Core = instance
    }
}
