package com.damn.xray.core

class NullEventBuilder : IEventBuilder {

    override fun build(): Event? = null

    override fun setLevel(level: LogLevel): IEventBuilder = this

    override fun setLevel(level: Int): IEventBuilder = this

    override fun exception(t: Throwable?): IEventBuilder = this

    override fun putData(data: Map<String, *>): IEventBuilder = this

    override fun data(map: Map<String, *>): IEventBuilder = this

    override fun data(vararg pairs: Pair<String, Any?>): IEventBuilder = this

    override fun data(key: String, value: Any?): IEventBuilder = this

    override fun expandData(): IEventBuilder = this

    override fun withCallStack(): IEventBuilder = this

    override fun message(message: String) {}

    override fun message(message: String, vararg args: Any?) {}

    companion object {
        @JvmField
        val INSTANCE = NullEventBuilder()
    }
}
