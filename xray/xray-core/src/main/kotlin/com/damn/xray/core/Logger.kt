package com.damn.xray.core

import com.damn.xray.core.formatting.message.BasicMessageFormatter
import com.damn.xray.core.formatting.message.IMessageFormatter
import com.damn.xray.core.utility.Stack

class Logger internal constructor(
    val name: String,
    private val parent: Logger?,
    val core: Core,
) {
    internal constructor(name: String, parent: Logger?) : this(name, parent, Core.get())

    private val children = HashMap<String, Logger>()
    private var context = LogContext()
    private var messageFormatter: IMessageFormatter? = if (parent == null) BasicMessageFormatter() else null

    val hierarchyPaths: List<String> =
        if (parent == null) listOf("") else listOf(name) + parent.hierarchyPaths

    val defaultTag: String = if (name.isEmpty()) "" else name.substringAfterLast(NameSeparator)

    @PublishedApi
    internal fun resolveTag(tag: String): String =
        tag.ifEmpty { if (defaultTag.isNotEmpty()) defaultTag else getStackTag() }

    @PublishedApi
    internal fun getStackTag(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val frameOffset = Stack.getClientCodeFrameOffset(stackTrace)
        if (frameOffset in stackTrace.indices) {
            return stackTrace[frameOffset].className
        }
        return "Undefined"
    }

    // region Fluent Builders (Java & Kotlin)

    fun builder(level: LogLevel, tag: String = ""): IEventBuilder {
        val resolvedTag = resolveTag(tag)
        val sinks = core.resolveSinks(hierarchyPaths, name, resolvedTag, level)
        if (sinks.isEmpty()) {
            return NullEventBuilder.INSTANCE
        }
        return EventBuilder(
            loggerName = name,
            tag = resolvedTag,
            context = getFullContext(),
            messageFormatter = resolveMessageFormatter(),
            autoSubmit = true,
            core = core,
            hierarchyPaths = hierarchyPaths,
            targetSinks = sinks,
        ).apply {
            setLevel(level)
        }
    }

    fun v(): IEventBuilder = builder(LogLevel.VERBOSE)
    fun d(): IEventBuilder = builder(LogLevel.DEBUG)
    fun i(): IEventBuilder = builder(LogLevel.INFO)
    fun w(): IEventBuilder = builder(LogLevel.WARNING)
    fun e(): IEventBuilder = builder(LogLevel.ERROR)

    // endregion

    // region Direct Convenience Methods

    private fun logDirect(level: LogLevel, tag: String, message: String, exception: Throwable?) {
        val resolvedTag = resolveTag(tag)
        val sinks = core.resolveSinks(hierarchyPaths, name, resolvedTag, level)
        if (sinks.isEmpty()) return
        createDslBuilder(resolvedTag, level, exception, sinks).apply {
            this.message = message
            submit()
        }
    }

    @JvmOverloads
    fun v(message: String, exception: Throwable? = null) =
        logDirect(LogLevel.VERBOSE, "", message, exception)

    fun v(tag: String, message: String) =
        logDirect(LogLevel.VERBOSE, tag, message, null)

    fun v(tag: String, message: String, exception: Throwable?) =
        logDirect(LogLevel.VERBOSE, tag, message, exception)

    @JvmOverloads
    fun d(message: String, exception: Throwable? = null) =
        logDirect(LogLevel.DEBUG, "", message, exception)

    fun d(tag: String, message: String) =
        logDirect(LogLevel.DEBUG, tag, message, null)

    fun d(tag: String, message: String, exception: Throwable?) =
        logDirect(LogLevel.DEBUG, tag, message, exception)

    @JvmOverloads
    fun i(message: String, exception: Throwable? = null) =
        logDirect(LogLevel.INFO, "", message, exception)

    fun i(tag: String, message: String) =
        logDirect(LogLevel.INFO, tag, message, null)

    fun i(tag: String, message: String, exception: Throwable?) =
        logDirect(LogLevel.INFO, tag, message, exception)

    @JvmOverloads
    fun w(message: String, exception: Throwable? = null) =
        logDirect(LogLevel.WARNING, "", message, exception)

    fun w(tag: String, message: String) =
        logDirect(LogLevel.WARNING, tag, message, null)

    fun w(tag: String, message: String, exception: Throwable?) =
        logDirect(LogLevel.WARNING, tag, message, exception)

    @JvmOverloads
    fun e(message: String, exception: Throwable? = null) =
        logDirect(LogLevel.ERROR, "", message, exception)

    fun e(tag: String, message: String) =
        logDirect(LogLevel.ERROR, tag, message, null)

    fun e(tag: String, message: String, exception: Throwable?) =
        logDirect(LogLevel.ERROR, tag, message, exception)

    // endregion

    // region Kotlin Inline DSL Methods

    @PublishedApi
    internal fun createDslBuilder(
        tag: String,
        level: LogLevel,
        exception: Throwable? = null,
        targetSinks: List<ISink>? = null,
    ): EventBuilder =
        EventBuilder(
            loggerName = name,
            tag = tag,
            context = getFullContext(),
            messageFormatter = resolveMessageFormatter(),
            autoSubmit = false,
            core = core,
            hierarchyPaths = hierarchyPaths,
            targetSinks = targetSinks,
        ).apply {
            this.level = level
            this.exception = exception
        }

    inline fun log(
        level: LogLevel,
        tag: String = "",
        exception: Throwable? = null,
        block: EventBuilder.() -> Any?,
    ) {
        val resolvedTag = resolveTag(tag)
        val sinks = core.resolveSinks(hierarchyPaths, name, resolvedTag, level)
        if (sinks.isEmpty()) return
        val builder = createDslBuilder(resolvedTag, level, exception, sinks)
        val result = builder.block()
        if (result is CharSequence) {
            builder.message = result.toString()
        }
        builder.submit()
    }

    inline fun v(tag: String = "", exception: Throwable? = null, block: EventBuilder.() -> Any?) =
        log(LogLevel.VERBOSE, tag, exception, block)

    inline fun v(exception: Throwable, block: EventBuilder.() -> Any?) =
        log(LogLevel.VERBOSE, "", exception, block)

    inline fun d(tag: String = "", exception: Throwable? = null, block: EventBuilder.() -> Any?) =
        log(LogLevel.DEBUG, tag, exception, block)

    inline fun d(exception: Throwable, block: EventBuilder.() -> Any?) =
        log(LogLevel.DEBUG, "", exception, block)

    inline fun i(tag: String = "", exception: Throwable? = null, block: EventBuilder.() -> Any?) =
        log(LogLevel.INFO, tag, exception, block)

    inline fun i(exception: Throwable, block: EventBuilder.() -> Any?) =
        log(LogLevel.INFO, "", exception, block)

    inline fun w(tag: String = "", exception: Throwable? = null, block: EventBuilder.() -> Any?) =
        log(LogLevel.WARNING, tag, exception, block)

    inline fun w(exception: Throwable, block: EventBuilder.() -> Any?) =
        log(LogLevel.WARNING, "", exception, block)

    inline fun e(tag: String = "", exception: Throwable? = null, block: EventBuilder.() -> Any?) =
        log(LogLevel.ERROR, tag, exception, block)

    inline fun e(exception: Throwable, block: EventBuilder.() -> Any?) =
        log(LogLevel.ERROR, "", exception, block)

    // endregion

    // region Context and Formatter

    @PublishedApi
    internal fun resolveMessageFormatter(): IMessageFormatter {
        synchronized(this) {
            messageFormatter?.let { return it }
        }
        return parent?.resolveMessageFormatter() ?: BasicMessageFormatter()
    }

    fun getFullContext(): Map<String, Any?> {
        val mergedContext = HashMap<String, Any?>()
        if (parent != null) {
            mergedContext.putAll(parent.getFullContext())
        }
        synchronized(this) {
            mergedContext.putAll(context.retrieve())
        }
        return mergedContext
    }

    @Synchronized
    fun setContext(context: LogContext): Logger {
        this.context = context
        return this
    }

    fun setFormatter(formatter: IMessageFormatter): Logger {
        synchronized(this) {
            this.messageFormatter = formatter
        }
        return this
    }

    // endregion

    // region Hierarchy

    @Synchronized
    fun getChild(childName: String): Logger {
        val sep = childName.indexOf(NameSeparator)
        if (sep == -1) {
            return getOrMakeChild(childName)
        }
        val child = childName.substring(0, sep)
        val grandChild = childName.substring(sep + 1)
        return getOrMakeChild(child).getChild(grandChild)
    }

    private fun getOrMakeChild(childName: String): Logger {
        val existing = children[childName]
        if (existing != null) {
            return existing
        }
        val fullName = if (name.isEmpty()) childName else "$name$NameSeparator$childName"
        val newLogger = Logger(fullName, this, core)
        children[childName] = newLogger
        return newLogger
    }

    // endregion

    companion object {
        const val NameSeparator: Char = '/'

        @JvmStatic
        fun get(): Logger = Core.get().getLogger()

        @JvmStatic
        fun get(name: String): Logger = Core.get().getLogger(name)

        @JvmStatic
        fun get(name: String, core: Core): Logger = core.getLogger(name)

        inline fun <reified T : Any> get(core: Core = Core.get()): Logger =
            core.getLogger(T::class.java.simpleName)
    }
}

inline fun <reified T : Any> T.logger(
    name: String = "",
    core: Core = Core.get(),
): Lazy<Logger> = lazy(LazyThreadSafetyMode.PUBLICATION) {
    core.getLogger(name.ifEmpty { T::class.java.simpleName })
}
