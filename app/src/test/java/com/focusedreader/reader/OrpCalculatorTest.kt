package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class OrpCalculatorTest {
    // min(length / 2, 4) — pivot tracks geometric middle for short words and
    // caps at idx 4 so long words don't push the highlight too far right.
    private val cases = listOf(
        1 to 0,
        2 to 1, 3 to 1,
        4 to 2, 5 to 2,
        6 to 3, 7 to 3,
        8 to 4, 9 to 4,
        10 to 4, 11 to 4, 12 to 4, 13 to 4,
        14 to 4, 28 to 4, 50 to 4
    )

    @TestFactory
    fun `pivot index is min length-over-two and four`() = cases.map { (len, expected) ->
        DynamicTest.dynamicTest("len=$len -> $expected") {
            assertEquals(expected, OrpCalculator.pivotIndex(len))
        }
    }

    @org.junit.jupiter.api.Test
    fun `split pad picks a`() {
        assertEquals('a', OrpCalculator.split("pad").pivot)
    }

    @org.junit.jupiter.api.Test
    fun `split discover picks o`() {
        val s = OrpCalculator.split("discover")
        assertEquals('o', s.pivot)
        assertEquals("disc", s.left)
        assertEquals("ver", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split accompanied picks m`() {
        val s = OrpCalculator.split("accompanied")
        assertEquals('m', s.pivot)
        assertEquals("acco", s.left)
        assertEquals("panied", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split commencement picks e`() {
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
}
