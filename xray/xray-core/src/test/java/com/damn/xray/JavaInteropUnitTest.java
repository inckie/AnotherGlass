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
        log.e("JavaTag")
                .exception(ex)
                .putData(Collections.singletonMap("key", "value"))
                .message("Formatted %s %d", "error", 42);

        // Direct call style from Java
        log.d("JavaTag", "Direct debug message");

        // Filtered out verbose call
        log.v("JavaTag", "Should be filtered out");

        List<Event> events = sink.getEvents().getValue();
        assertEquals(2, events.size());

        Event first = events.get(0);
        assertEquals("JavaTag", first.getCategory());
        assertEquals("JavaSubsystem/Component", first.getSubsystem());
        assertEquals(LogLevel.ERROR, first.getLevel());
        assertEquals("Formatted error 42", first.getMessage());
        assertSame(ex, first.getException());
        assertEquals("value", first.getData().get("key"));

        Event second = events.get(1);
        assertEquals(LogLevel.DEBUG, second.getLevel());
        assertEquals("Direct debug message", second.getMessage());
    }
}
