package com.focusedreader.reader

object Wpm {
    const val MIN = 100
    const val DEFAULT_MAX = 900
    fun tickMillis(wpm: Int): Long = 60_000L / wpm
    fun clamp(wpm: Int, max: Int = DEFAULT_MAX): Int = wpm.coerceIn(MIN, max)
}
