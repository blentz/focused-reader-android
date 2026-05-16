package com.focusedreader.reader

import kotlin.math.abs

object Wpm {
    const val MIN = -100
    const val DEFAULT_MAX = 400

    /**
     * Returns the delay between ticks. Caller must check for 0 wpm first
     * (engine pauses instead of ticking) — passing 0 returns Long.MAX_VALUE
     * as a defensive fallback.
     */
    fun tickMillis(wpm: Int): Long = if (wpm == 0) Long.MAX_VALUE else 60_000L / abs(wpm)

    fun clamp(wpm: Int, max: Int = DEFAULT_MAX): Int = wpm.coerceIn(MIN, max)

    fun direction(wpm: Int): Int = if (wpm < 0) -1 else 1
}
