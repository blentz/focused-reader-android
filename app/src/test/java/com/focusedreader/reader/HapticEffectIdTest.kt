package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HapticEffectIdTest {
    @Test fun `intensity 1 picks TICK`() = assertEquals(EffectId.TICK, chooseEffectId(1))
    @Test fun `intensity 11 picks TICK at upper bound`() = assertEquals(EffectId.TICK, chooseEffectId(11))
    @Test fun `intensity 12 picks CLICK at lower bound`() = assertEquals(EffectId.CLICK, chooseEffectId(12))
    @Test fun `intensity 22 picks CLICK at upper bound`() = assertEquals(EffectId.CLICK, chooseEffectId(22))
    @Test fun `intensity 23 picks HEAVY_CLICK at lower bound`() = assertEquals(EffectId.HEAVY_CLICK, chooseEffectId(23))
    @Test fun `intensity 33 picks HEAVY_CLICK`() = assertEquals(EffectId.HEAVY_CLICK, chooseEffectId(33))
    @Test fun `intensity 100 picks HEAVY_CLICK`() = assertEquals(EffectId.HEAVY_CLICK, chooseEffectId(100))
    @Test fun `intensity 0 picks TICK`() = assertEquals(EffectId.TICK, chooseEffectId(0))
}
