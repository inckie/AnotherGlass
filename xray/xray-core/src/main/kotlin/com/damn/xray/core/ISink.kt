package com.damn.xray.core

fun interface ISink {
    fun log(event: Event)
}
