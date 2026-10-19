package com.damn.xray.ui.sinks

open class InMemoryLogSink @JvmOverloads constructor(
    maxCapacity: Int = 10_000,
) : com.damn.xray.core.sinks.InMemoryLogSink(maxCapacity)

