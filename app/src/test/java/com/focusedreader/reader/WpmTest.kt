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
    @Test fun `negative wpm uses absolute value for tick delay`() {
        assertEquals(200L, Wpm.tickMillis(-300))
        assertEquals(600L, Wpm.tickMillis(-100))
    }
    @Test fun `zero wpm returns Long MAX_VALUE`() {
        assertEquals(Long.MAX_VALUE, Wpm.tickMillis(0))
    }
    @Test fun `clamp respects bounds with negative min`() {
        assertEquals(-100, Wpm.clamp(-500, max = 900))
        assertEquals(900, Wpm.clamp(1500, max = 900))
        assertEquals(500, Wpm.clamp(2000, max = 500))
        assertEquals(300, Wpm.clamp(300, max = 900))
        assertEquals(0, Wpm.clamp(0, max = 900))
        assertEquals(-50, Wpm.clamp(-50, max = 900))
    }
    @Test fun `direction reflects sign`() {
        assertEquals(1, Wpm.direction(300))
        assertEquals(1, Wpm.direction(0))
        assertEquals(-1, Wpm.direction(-50))
        assertEquals(-1, Wpm.direction(-900))
    }
}
