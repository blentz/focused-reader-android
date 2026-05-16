package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class OrpCalculatorTest {
    private val cases = listOf(
        1 to 0,
        2 to 1, 3 to 1,
        4 to 2, 5 to 2,
        6 to 3, 7 to 3,
        8 to 4, 9 to 4,
        10 to 5, 13 to 6,
        14 to 7, 28 to 14, 50 to 25
    )

    @TestFactory
    fun `pivot index is length divided by two`() = cases.map { (len, expected) ->
        DynamicTest.dynamicTest("len=$len -> $expected") {
            assertEquals(expected, OrpCalculator.pivotIndex(len))
        }
    }

    @org.junit.jupiter.api.Test
    fun `split returns left pivot right`() {
        val s = OrpCalculator.split("reading")
        assertEquals("rea", s.left)
        assertEquals('d', s.pivot)
        assertEquals("ing", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split single char`() {
        val s = OrpCalculator.split("a")
        assertEquals("", s.left); assertEquals('a', s.pivot); assertEquals("", s.right)
    }

    @org.junit.jupiter.api.Test
    fun `split twenty-eight char word picks true middle`() {
        val word = "antidisestablishmentarianism"
        val s = OrpCalculator.split(word)
        assertEquals(14, OrpCalculator.pivotIndex(word.length))
        assertEquals('s', s.pivot)
        assertEquals("antidisestabli", s.left)
        assertEquals("hmentarianism", s.right)
    }
}
