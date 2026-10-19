package com.damn.xray.core

import com.damn.xray.core.formatting.message.IMessageFormatter
import com.damn.xray.core.utility.Stack

@XRayDsl
class EventBuilder @JvmOverloads constructor(
    val category: String,
    val subsystem: String,
    context: Map<String, Any?>,
    private val messageFormatter: IMessageFormatter,
    @PublishedApi internal var autoSubmit: Boolean = true,
) : IEventBuilder {

    val timestamp: Long = System.currentTimeMillis()
    var level: LogLevel = LogLevel.DEBUG
    var message: String = ""
    var exception: Throwable? = null
    val data: MutableMap<String, Any?> = LinkedHashMap()
    val context: MutableMap<String, Any?> = LinkedHashMap(context)

    private var expandData: Boolean = false
    private var submitted: Boolean = false

    override fun build(): Event = Event(
        category = category,
        subsystem = subsystem,
        timestamp = timestamp,
        level = level,
        message = message,
        data = data.takeIf { it.isNotEmpty() }?.toMap(),
        context = context.takeIf { it.isNotEmpty() }?.toMap(),
        exception = exception,
    )

    override fun setLevel(level: LogLevel): EventBuilder {
        this.level = level
        return this
    }

    override fun setLevel(level: Int): EventBuilder = setLevel(LogLevel.fromLevel(level))

    override fun exception(t: Throwable?): EventBuilder {
        this.exception = t
        return this
    }

    override fun putData(data: Map<String, *>): EventBuilder {
        this.data.putAll(data)
        return this
    }

    override fun data(map: Map<String, *>): EventBuilder = putData(map)

    override fun data(vararg pairs: Pair<String, Any?>): EventBuilder {
        pairs.forEach { (k, v) -> this.data[k] = v }
        return this
    }

    override fun data(key: String, value: Any?): EventBuilder {
        this.data[key] = value
        return this
    }

    override fun expandData(): EventBuilder {
        this.expandData = true
        return this
    }

    override fun withCallStack(): EventBuilder {
        val stackTrace = Thread.currentThread().stackTrace
        val frameOffset = Stack.getClientCodeFrameOffset(stackTrace)
        val stack = ArrayList<String>()
        for (i in frameOffset until stackTrace.size) {
            stack.add(stackTrace[i].toString() + '\n')
        }
        this.context["stackTrace"] = stack
        return this
    }

    override fun message(message: String) {
        this.message = message
        if (autoSubmit) {
            submit()
        }
    }

    override fun message(message: String, vararg args: Any?) {
        this.message = if (args.isEmpty()) message else messageFormatter.format(message, data, *args)
        if (autoSubmit) {
            submit()
        }
    }

    @PublishedApi
    internal fun submit() {
        if (!submitted) {
            submitted = true
            Core.get().submit(build())
        }
    }
}
