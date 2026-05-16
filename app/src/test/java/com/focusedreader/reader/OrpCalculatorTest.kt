package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class OrpCalculatorTest {
    // (length + 1) / 3 — shifts pivot slightly left of geometric middle so wider
    // post-pivot tail balances the visual weight; matches user-confirmed
    // "accompanied" (11 -> 4 = 'm') and "commencement" (12 -> 4 = 'e').
    private val cases = listOf(
        1 to 0,
        2 to 1, 3 to 1,
        4 to 1, 5 to 2,
        6 to 2, 7 to 2,
        8 to 3, 9 to 3,
        10 to 3,
        11 to 4, 12 to 4,
        13 to 4, 14 to 5,
        28 to 9, 50 to 17
    )

    @TestFactory
    fun `pivot index uses left-biased formula`() = cases.map { (len, expected) ->
        DynamicTest.dynamicTest("len=$len -> $expected") {
            assertEquals(expected, OrpCalculator.pivotIndex(len))
        }
    }

    @org.junit.jupiter.api.Test
    fun `split accompanied lands on the m`() {
        val s = OrpCalculator.split("accompanied")
        assertEquals('m', s.pivot)
        assertEquals("acco", s.left)
        assertEquals("panied", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split commencement lands on the e`() {
        val s = OrpCalculator.split("commencement")
        assertEquals('e', s.pivot)
        assertEquals("comm", s.left)
        assertEquals("ncement", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split single char`() {
        val s = OrpCalculator.split("a")
        assertEquals("", s.left); assertEquals('a', s.pivot); assertEquals("", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split pad still picks a`() {
        val s = OrpCalculator.split("pad")
        assertEquals('a', s.pivot)
    }
}
