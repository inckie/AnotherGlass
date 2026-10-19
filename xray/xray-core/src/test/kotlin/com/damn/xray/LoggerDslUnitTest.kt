package com.damn.xray

import com.damn.xray.core.Core
import com.damn.xray.core.LogLevel
import com.damn.xray.core.Logger
import com.damn.xray.core.logger
import com.damn.xray.core.routing.DefaultSinkFilter
import com.damn.xray.core.routing.DisabledFilter
import com.damn.xray.core.routing.and
import com.damn.xray.core.routing.not
import com.damn.xray.core.routing.or
import com.damn.xray.core.sinks.InMemoryLogSink
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoggerDslUnitTest {

    private lateinit var sink: TestSink

    @Before
    fun setUp() {
        Core.get().reset()
        sink = TestSink("TestSink")
        Core.get().addSink("test_sink", sink)
    }

    @Test
    fun testSimpleDsl() {
        val log = Logger.get("TestLogger")
        sink.addExpectedMessage("simple debug message")

        log.d { "simple debug message" }

        assertTrue(sink.isEmpty())
        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals("simple debug message", event.message)
        assertEquals(LogLevel.DEBUG, event.level)
        assertEquals("TestLogger", event.loggerName)
        assertEquals("TestLogger", event.tag) // defaultTag for "TestLogger"
        assertNull(event.exception)
    }

    @Test
    fun testExceptionDsl() {
        val log = Logger.get("TestLogger")
        val exc = IllegalArgumentException("something went wrong")
        sink.addExpectedMessage("error with exception")

        log.e(exc) { "error with exception" }

        assertTrue(sink.isEmpty())
        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals("error with exception", event.message)
        assertEquals(LogLevel.ERROR, event.level)
        assertSame(exc, event.exception)
    }

    @Test
    fun testTagAndDataDsl() {
        val log = Logger.get("TestLogger")
        sink.addExpectedMessage("data message")

        log.d("CUSTOM_TAG") {
            data("key1" to "val1", "key2" to 42)
            "data message"
        }

        assertTrue(sink.isEmpty())
        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals("CUSTOM_TAG", event.tag)
        assertEquals("data message", event.message)
        assertNotNull(event.data)
        assertEquals("val1", event.data?.get("key1"))
        assertEquals(42, event.data?.get("key2"))
    }

    @Test
    fun testCustomizationAfterMessage() {
        val log = Logger.get("TestLogger")
        val exc = IllegalStateException("runtime issue")
        sink.addExpectedMessage("explicit message")

        // Verifies customization after message() works because autoSubmit is false in DSL mode
        log.e("CUSTOM_TAG") {
            message("explicit message")
            exception(exc)
            data("k" to "v")
        }

        assertTrue(sink.isEmpty())
        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals("CUSTOM_TAG", event.tag)
        assertEquals("explicit message", event.message)
        assertSame(exc, event.exception)
        assertNotNull(event.data)
        assertEquals("v", event.data?.get("k"))
    }

    @Test
    fun testBlockNotEvaluatedWhenNoSinkAccepts() {
        val log = Logger.get("TestLogger")
        Core.get().setFilter("test_sink", "", DisabledFilter())

        var evaluated = false
        log.d {
            evaluated = true
            "this should never be built"
        }

        assertFalse("Block must NOT be evaluated when no sink accepts the event", evaluated)
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun testDirectConvenienceMethods() {
        val log = Logger.get("TestLogger")
        sink.addExpectedMessage("direct info")

        log.i("INFO_TAG", "direct info")

        assertTrue(sink.isEmpty())
        assertEquals(1, sink.events.size)
        val event = sink.events[0]
        assertEquals("INFO_TAG", event.tag)
        assertEquals("direct info", event.message)
        assertEquals(LogLevel.INFO, event.level)
    }

    @Test
    fun testOneArgAndTwoArgDirectLogging() {
        val log = Logger.get("TestLogger")
        sink.addExpectedMessage("one arg message")
        log.d("one arg message")

        assertEquals(1, sink.events.size)
        assertEquals("one arg message", sink.events[0].message)
        assertEquals(LogLevel.DEBUG, sink.events[0].level)
        assertEquals("TestLogger", sink.events[0].tag)

        val exc = RuntimeException("crash")
        sink.addExpectedMessage("error message")
        log.e("error message", exc)

        assertEquals(2, sink.events.size)
        assertEquals("error message", sink.events[1].message)
        assertEquals(LogLevel.ERROR, sink.events[1].level)
        assertSame(exc, sink.events[1].exception)
    }

    class SampleService {
        val delegatedLog by logger()
    }

    @Test
    fun testPropertyDelegateLogger() {
        val service = SampleService()
        assertEquals("SampleService", service.delegatedLog.name)
        sink.addExpectedMessage("service log")
        service.delegatedLog.i("service log")

        assertEquals(1, sink.events.size)
        assertEquals("SampleService", sink.events[0].loggerName)
        assertEquals("service log", sink.events[0].message)
    }

    @Test
    fun testIsolatedCoreInstance() {
        val customCore = Core()
        val customSink = TestSink("CustomSink")
        customCore.addSink("custom_sink", customSink)

        val customLog = customCore.getLogger("Isolated")
        customSink.addExpectedMessage("custom message")
        customLog.i("custom message")

        // Should be received by customSink
        assertEquals(1, customSink.events.size)
        assertEquals("custom message", customSink.events[0].message)
        assertEquals("Isolated", customSink.events[0].loggerName)

        // Global sink should NOT have received this message
        assertTrue(sink.events.isEmpty())

        // And logging to global logger does NOT go to customSink
        val globalLog = Logger.get("GlobalLogger")
        sink.addExpectedMessage("global message")
        globalLog.i("global message")
        assertEquals(1, sink.events.size)
        assertEquals(1, customSink.events.size)
    }

    @Test
    fun testFilterCombinators() {
        val filterA = DefaultSinkFilter(LogLevel.INFO)
        val filterB = DefaultSinkFilter(LogLevel.ERROR)

        val andFilter = filterA and filterB
        assertTrue(andFilter.accept("log", "tag", LogLevel.ERROR))
        assertFalse(andFilter.accept("log", "tag", LogLevel.INFO))

        val orFilter = filterA or filterB
        assertTrue(orFilter.accept("log", "tag", LogLevel.INFO))
        assertFalse(orFilter.accept("log", "tag", LogLevel.DEBUG))

        val notFilter = !filterA
        assertTrue(notFilter.accept("log", "tag", LogLevel.DEBUG))
        assertFalse(notFilter.accept("log", "tag", LogLevel.INFO))
    }

    @Test
    fun testInMemoryLogSink() {
        Core.get().reset()
        val inMemorySink = InMemoryLogSink(maxCapacity = 3)
        Core.get().addSink("memory_sink", inMemorySink)

        val log = Logger.get("MemoryTest")
        log.d("TAG", "message 1")
        log.d("TAG", "message 2")
        log.d("TAG", "message 3")

        assertEquals(3, inMemorySink.events.value.size)
        assertEquals("message 1", inMemorySink.events.value[0].message)

        // Over capacity test: oldest should be dropped
        log.d("TAG", "message 4")
        assertEquals(3, inMemorySink.events.value.size)
        assertEquals("message 2", inMemorySink.events.value[0].message)
        assertEquals("message 4", inMemorySink.events.value[2].message)

        inMemorySink.clear()
        assertTrue(inMemorySink.events.value.isEmpty())
    }

    @Test
    fun testSynchronousFileLogSinkOrder() {
        Core.get().reset()
        val tempFile = java.io.File.createTempFile("xray_sync_test", ".log")
        tempFile.deleteOnExit()
        val fileSink = com.damn.xray.core.sinks.FileLogSink(tempFile) { event -> "${event.message}\n" }
        Core.get().addSink("file_sink", fileSink)

        val log = Logger.get("CrashTest")
        log.i { "step 1" }
        log.i { "step 2" }
        log.e { "step 3 before crash" }

        val lines = tempFile.readLines()
        assertEquals(listOf("step 1", "step 2", "step 3 before crash"), lines)
    }
}
