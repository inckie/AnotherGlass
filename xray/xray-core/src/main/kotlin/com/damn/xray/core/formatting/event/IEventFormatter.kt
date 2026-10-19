package com.damn.xray.core.formatting.event

import com.damn.xray.core.Event

fun interface IEventFormatter {
    fun format(event: Event): String
}
