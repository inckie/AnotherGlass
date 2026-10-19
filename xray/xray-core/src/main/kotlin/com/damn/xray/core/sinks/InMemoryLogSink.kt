package com.damn.xray.core.sinks

import com.damn.xray.core.Event
import com.damn.xray.core.ISink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

open class InMemoryLogSink @JvmOverloads constructor(
    private val maxCapacity: Int = 10_000,
) : ISink {
    private val lock = Any()
    private val buffer = ArrayDeque<Event>()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    override fun log(event: Event) {
        val snapshot = synchronized(lock) {
            if (buffer.size >= maxCapacity) {
                buffer.removeFirst()
            }
            buffer.addLast(event)
            buffer.toList()
        }
        _events.value = snapshot
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
        _events.value = emptyList()
    }
}
