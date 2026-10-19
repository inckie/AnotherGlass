package com.damn.xray

import com.damn.xray.core.*
import com.damn.xray.core.routing.DefaultSinkFilter
import com.damn.xray.core.routing.Mapper
import org.junit.Assert.*
import org.junit.Test

/**
 * Testing for mapping routines
 */
class MapperUnitTest {

    @Test
    fun testMapperRoot() {
        Core.get().reset()
        val testSinkDebug = TestSink("Debug")
        val testSinkError = TestSink("Error")
        Core.get()
            .addSink("test_sink_debug", testSinkDebug)
            .addSink("test_sink_error", testSinkError)
            .setFilter("test_sink_debug", "", DefaultSinkFilter(LogLevel.DEBUG))
            .setFilter("test_sink_error", "", DefaultSinkFilter(LogLevel.ERROR))

        val debugMessage = "debug message"
        testSinkDebug.addExpectedMessage(debugMessage)
        Logger.get().d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        val errorMessage = "error message"
        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        Logger.get().e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        // run same on child logger
        val childLogger = Logger.get().getChild("childLogger")

        testSinkDebug.addExpectedMessage(debugMessage)
        childLogger.d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        childLogger.e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        // override error filter for child logger
        Core.get()
            .setFilter("test_sink_error", "childLogger", DefaultSinkFilter(LogLevel.DEBUG))

        // now both sinks should receive it
        testSinkDebug.addExpectedMessage(debugMessage)
        testSinkError.addExpectedMessage(debugMessage)
        childLogger.d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        childLogger.e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        val grandChildLogger = childLogger.getChild("grandChildLogger")

        // still both should receive the message
        testSinkDebug.addExpectedMessage(debugMessage)
        testSinkError.addExpectedMessage(debugMessage)
        grandChildLogger.d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        grandChildLogger.e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        // remove filter override
        Core.get()
            .setFilter("test_sink_error", "childLogger", null)

        // validate it removed
        testSinkDebug.addExpectedMessage(debugMessage)
        childLogger.d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        childLogger.e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        // same for grandchild
        testSinkDebug.addExpectedMessage(debugMessage)
        grandChildLogger.d().message(debugMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        testSinkDebug.addExpectedMessage(errorMessage)
        testSinkError.addExpectedMessage(errorMessage)
        grandChildLogger.e().message(errorMessage)
        assertTrue("debug sink does not expect any messages", testSinkDebug.isEmpty())
        assertTrue("error sink does not expect any messages", testSinkError.isEmpty())

        Core.get().reset()
    }

    @Test
    fun testSinksView() {
        // validate the mapper uses sinks HashMap keys view
        val hashMap = mutableMapOf<String, ISink>(
            "sink0" to TestSink("Error"),
            "sink1" to TestSink("Error")
        )
        val mapper = Mapper(hashMap)
        val mappingBefore = mapper.getMapping("", "", LogLevel.DEBUG.level)
        assertEquals(2, mappingBefore.size)
        hashMap.remove("sink0")
        val mappingAfter = mapper.getMapping("", "", LogLevel.DEBUG.level)
        assertEquals(1, mappingAfter.size)
    }

    @Test
    fun testSinksSetViewIntegration() {
        Core.get().reset()
        // same as above, but through Core
        val testSink0 = TestSink("Error")
        val testSink1 = TestSink("Error")

        val testEvent = Event(
            loggerName = "subsystem",
            tag = "tag",
            timestamp = 0L,
            level = LogLevel.DEBUG,
            message = "message"
        )

        // add mapping to not yet registered sinks
        Core.get().setFilter("test_sink_0", "subsystem") { _, _, _ -> true }
        Core.get().setFilter("test_sink_1", "subsystem") { _, _, _ -> true }

        var mapping = Core.get().getMapping(testEvent)
        assertTrue("No sinks available", mapping.isEmpty())

        // add sinks
        Core.get().addSink("test_sink_0", testSink0)
        Core.get().addSink("test_sink_1", testSink1)

        mapping = Core.get().getMapping(testEvent)
        // validate mapping to contain both sinks
        assertTrue("Sink0 was added", mapping.contains(testSink0))
        assertTrue("Sink1 was added", mapping.contains(testSink1))

        Core.get().removeSink(testSink1)
        // validate mapping to contain only one sink
        mapping = Core.get().getMapping(testEvent)
        assertTrue("Sink0 is still available", mapping.contains(testSink0))
        assertFalse("Sink1 was removed", mapping.contains(testSink1))

        Core.get().reset()
    }

    @Test
    fun testChildLoggerPreservesParentFiltersForOtherSinks() {
        Core.get().reset()
        val sinkA = TestSink("SinkA")
        val sinkB = TestSink("SinkB")

        Core.get()
            .addSink("sink_a", sinkA)
            .addSink("sink_b", sinkB)
            // Root filters: sink_a allows INFO+, sink_b allows ERROR+
            .setFilter("sink_a", "", DefaultSinkFilter(LogLevel.INFO))
            .setFilter("sink_b", "", DefaultSinkFilter(LogLevel.ERROR))

        val childLogger = Logger.get().getChild("childLogger")

        // Child logger overrides sink_a to allow DEBUG+
        Core.get().setFilter("sink_a", "childLogger", DefaultSinkFilter(LogLevel.DEBUG))

        // Log DEBUG message on child logger:
        // sink_a should accept (DEBUG >= DEBUG)
        // sink_b should inherit root filter (ERROR) and reject (DEBUG < ERROR)
        sinkA.addExpectedMessage("child debug")
        childLogger.d().message("child debug")
        assertTrue(sinkA.isEmpty())
        assertTrue(sinkB.isEmpty())

        // Log INFO message on child logger:
        // sink_a should accept (INFO >= DEBUG)
        // sink_b should inherit root filter (ERROR) and reject (INFO < ERROR)
        sinkA.addExpectedMessage("child info")
        childLogger.i().message("child info")
        assertTrue(sinkA.isEmpty())
        assertTrue(sinkB.isEmpty())

        // Log ERROR message on child logger:
        // sink_a should accept (ERROR >= DEBUG)
        // sink_b should accept (ERROR >= ERROR)
        sinkA.addExpectedMessage("child error")
        sinkB.addExpectedMessage("child error")
        childLogger.e().message("child error")
        assertTrue(sinkA.isEmpty())
        assertTrue(sinkB.isEmpty())

        Core.get().reset()
    }
}
