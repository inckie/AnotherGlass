package com.damn.xray.core

import com.damn.xray.core.formatting.message.BasicMessageFormatter
import com.damn.xray.core.formatting.message.IMessageFormatter
import com.damn.xray.core.utility.Stack

class Logger private constructor(
    val name: String,
    private val parent: Logger?,
) {
    private val children = HashMap<String, Logger>()
    private var context = LogContext()
    private var messageFormatter: IMessageFormatter? = if (parent == null) BasicMessageFormatter() else null

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

    private fun makeBuilder(tag: String, level: LogLevel): IEventBuilder {
        if (!Core.get().hasSinks(name, tag, level)) {
            return NullEventBuilder.INSTANCE
        }
        return EventBuilder(
            category = tag,
            subsystem = name,
            context = getFullContext(),
            messageFormatter = resolveMessageFormatter(),
            autoSubmit = true,
        ).apply {
            setLevel(level)
        }
    }

    // region Fluent Builders (Java & Kotlin)

    fun v(): IEventBuilder = v(resolveTag(""))
    fun v(tag: String): IEventBuilder = makeBuilder(tag, LogLevel.VERBOSE)

    fun d(): IEventBuilder = d(resolveTag(""))
    fun d(tag: String): IEventBuilder = makeBuilder(tag, LogLevel.DEBUG)

    fun i(): IEventBuilder = i(resolveTag(""))
    fun i(tag: String): IEventBuilder = makeBuilder(tag, LogLevel.INFO)

    fun w(): IEventBuilder = w(resolveTag(""))
    fun w(tag: String): IEventBuilder = makeBuilder(tag, LogLevel.WARNING)

    fun e(): IEventBuilder = e(resolveTag(""))
    fun e(tag: String): IEventBuilder = makeBuilder(tag, LogLevel.ERROR)

    // endregion

    // region Direct Convenience Methods

    @JvmOverloads
    fun v(tag: String, message: String, exception: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        if (!Core.get().hasSinks(name, resolvedTag, LogLevel.VERBOSE)) return
        createDslBuilder(resolvedTag, LogLevel.VERBOSE, exception).apply {
            this.message = message
            submit()
        }
    }

    @JvmOverloads
    fun d(tag: String, message: String, exception: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        if (!Core.get().hasSinks(name, resolvedTag, LogLevel.DEBUG)) return
        createDslBuilder(resolvedTag, LogLevel.DEBUG, exception).apply {
            this.message = message
            submit()
        }
    }

    @JvmOverloads
    fun i(tag: String, message: String, exception: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        if (!Core.get().hasSinks(name, resolvedTag, LogLevel.INFO)) return
        createDslBuilder(resolvedTag, LogLevel.INFO, exception).apply {
            this.message = message
            submit()
        }
    }

    @JvmOverloads
    fun w(tag: String, message: String, exception: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        if (!Core.get().hasSinks(name, resolvedTag, LogLevel.WARNING)) return
        createDslBuilder(resolvedTag, LogLevel.WARNING, exception).apply {
            this.message = message
            submit()
        }
    }

    @JvmOverloads
    fun e(tag: String, message: String, exception: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        if (!Core.get().hasSinks(name, resolvedTag, LogLevel.ERROR)) return
        createDslBuilder(resolvedTag, LogLevel.ERROR, exception).apply {
            this.message = message
            submit()
        }
    }

    // endregion

    // region Kotlin Inline DSL Methods

    @PublishedApi
    internal fun createDslBuilder(tag: String, level: LogLevel, exception: Throwable? = null): EventBuilder =
        EventBuilder(tag, name, getFullContext(), resolveMessageFormatter(), autoSubmit = false).apply {
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
        if (!Core.get().hasSinks(name, resolvedTag, level)) return
        val builder = createDslBuilder(resolvedTag, level, exception)
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
        val newLogger = Logger(fullName, this)
        children[childName] = newLogger
        return newLogger
    }

    // endregion

    companion object {
        const val NameSeparator: Char = '/'

        private val sRoot = Logger("", null)

        @JvmStatic
        fun get(): Logger = sRoot

        @JvmStatic
        fun get(name: String): Logger = sRoot.getChild(name)
    }
}
