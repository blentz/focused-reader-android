package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WpmTest {
    @Test fun `100 wpm = 600ms per tick`() {
        assertEquals(600L, Wpm.tickMillis(100))
    }
    @Test fun `300 wpm = 200ms per tick`() {
        assertEquals(200L, Wpm.tickMillis(300))
    }
    @Test fun `900 wpm = 66ms per tick`() {
        assertEquals(66L, Wpm.tickMillis(900))
    }
    @Test fun `clamp respects bounds`() {
        assertEquals(100, Wpm.clamp(50, max = 900))
        assertEquals(900, Wpm.clamp(1500, max = 900))
        assertEquals(500, Wpm.clamp(2000, max = 500))
        assertEquals(300, Wpm.clamp(300, max = 900))
    }
}
