package com.damn.xray.core

@DslMarker
annotation class XRayDsl

interface IEventBuilder {
    fun build(): Event?

    fun tag(tag: String): IEventBuilder

    fun setLevel(level: LogLevel): IEventBuilder

    fun setLevel(level: Int): IEventBuilder = setLevel(LogLevel.fromLevel(level))

    fun exception(t: Throwable?): IEventBuilder

    fun putData(data: Map<String, *>): IEventBuilder

    fun data(map: Map<String, *>): IEventBuilder = putData(map)

    fun data(vararg pairs: Pair<String, Any?>): IEventBuilder

    fun data(key: String, value: Any?): IEventBuilder

    fun expandData(): IEventBuilder

    fun withCallStack(): IEventBuilder

    fun message(message: String)

    fun message(message: String, vararg args: Any?)
}
