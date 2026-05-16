package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class OrpCalculatorTest {
    private val cases = listOf(
        1 to 0,
        2 to 1, 3 to 1, 4 to 1, 5 to 1,
        6 to 2, 7 to 2, 8 to 2, 9 to 2,
        10 to 3, 11 to 3, 12 to 3, 13 to 3,
        14 to 4, 20 to 4, 50 to 4
    )

    @TestFactory
    fun `pivot index per length bucket`() = cases.map { (len, expected) ->
        DynamicTest.dynamicTest("len=$len → $expected") {
            assertEquals(expected, OrpCalculator.pivotIndex(len))
        }
    }

    @org.junit.jupiter.api.Test
    fun `split returns left pivot right`() {
        val s = OrpCalculator.split("reading")
        assertEquals("re", s.left)
        assertEquals('a', s.pivot)
        assertEquals("ding", s.right)
    }
    @org.junit.jupiter.api.Test
    fun `split single char`() {
        val s = OrpCalculator.split("a")
        assertEquals("", s.left); assertEquals('a', s.pivot); assertEquals("", s.right)
    }
}
