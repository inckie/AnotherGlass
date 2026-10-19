package com.damn.xray

import com.damn.xray.core.Event
import com.damn.xray.core.ISink
import org.junit.Assert
import java.util.ArrayDeque

class TestSink(val name: String) : ISink {

    private val deque = ArrayDeque<String>()
    val events = mutableListOf<Event>()

    fun addExpectedMessage(message: String) {
        deque.push(message)
    }

    override fun log(event: Event) {
        events.add(event)
        Assert.assertFalse(
            "Sink $name expects log output",
            deque.isEmpty()
        )
        Assert.assertEquals(
            "Sink $name expects message ${event.message}",
            deque.pop(),
            event.message
        )
    }

    fun isEmpty(): Boolean = deque.isEmpty()

    fun clear() {
        deque.clear()
        events.clear()
    }
}
