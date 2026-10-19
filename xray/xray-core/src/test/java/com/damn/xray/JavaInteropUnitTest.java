package com.damn.xray;

import com.damn.xray.core.Core;
import com.damn.xray.core.Event;
import com.damn.xray.core.LogLevel;
import com.damn.xray.core.Logger;
import com.damn.xray.core.routing.DefaultSinkFilter;
import com.damn.xray.core.sinks.InMemoryLogSink;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class JavaInteropUnitTest {

    private InMemoryLogSink sink;

    @Before
    public void setUp() {
        Core.get().reset();
        sink = new InMemoryLogSink(100);
        Core.get()
                .addSink("mem", sink)
                .setFilter("mem", "", new DefaultSinkFilter(LogLevel.DEBUG));
    }

    @Test
    public void testJavaFluentAndDirectLogging() {
        Logger log = Logger.get("JavaSubsystem/Component");

        // Fluent builder style from Java
        RuntimeException ex = new RuntimeException("boom");
        log.e()
                .tag("JavaTag")
                .exception(ex)
                .putData(Collections.singletonMap("key", "value"))
                .message("Formatted %s %d", "error", 42);

        // Direct call style from Java (2 args: tag, message)
        log.d("JavaTag", "Direct debug message");

        // Filtered out verbose call
        log.v("JavaTag", "Should be filtered out");

        // Single arg direct call from Java
        log.d("Single arg from Java");

        // Message with exception direct call from Java
        log.e("Message with exception from Java", ex);

        List<Event> events = sink.getEvents().getValue();
        assertEquals(4, events.size());

        Event first = events.get(0);
        assertEquals("JavaTag", first.getTag());
        assertEquals("JavaSubsystem/Component", first.getLoggerName());
        assertEquals(LogLevel.ERROR, first.getLevel());
        assertEquals("Formatted error 42", first.getMessage());
        assertSame(ex, first.getException());
        assertEquals("value", first.getData().get("key"));

        Event second = events.get(1);
        assertEquals(LogLevel.DEBUG, second.getLevel());
        assertEquals("Direct debug message", second.getMessage());
        assertEquals("JavaTag", second.getTag());
        assertEquals("JavaSubsystem/Component", second.getLoggerName());

        Event third = events.get(2);
        assertEquals(LogLevel.DEBUG, third.getLevel());
        assertEquals("Single arg from Java", third.getMessage());
        assertEquals("Component", third.getTag());
        assertEquals("JavaSubsystem/Component", third.getLoggerName());

        Event fourth = events.get(3);
        assertEquals(LogLevel.ERROR, fourth.getLevel());
        assertEquals("Message with exception from Java", fourth.getMessage());
        assertSame(ex, fourth.getException());
        assertEquals("Component", fourth.getTag());
        assertEquals("JavaSubsystem/Component", fourth.getLoggerName());
    }
}
