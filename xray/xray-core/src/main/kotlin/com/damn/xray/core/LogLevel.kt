package com.damn.xray.core

enum class LogLevel(@JvmField val level: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4);

    companion object {
        @JvmField
        val verbose = VERBOSE

        @JvmField
        val debug = DEBUG

        @JvmField
        val info = INFO

        @JvmField
        val warning = WARNING

        @JvmField
        val error = ERROR

        @JvmStatic
        fun fromLevel(level: Int): LogLevel = entries.firstOrNull { it.level == level } ?: ERROR
    }
}
