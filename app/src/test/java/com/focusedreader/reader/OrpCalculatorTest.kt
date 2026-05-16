package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrpCalculatorTest {

    @Test fun `single char picks self`() {
        assertEquals('a', OrpCalculator.split("a").pivot)
    }

    @Test fun `pad odd length picks middle`() {
        val s = OrpCalculator.split("pad")
        assertEquals('a', s.pivot)
        assertEquals("p", s.left)
        assertEquals("d", s.right)
    }

    @Test fun `discover even length picks after-middle`() {
        val s = OrpCalculator.split("discover")
        assertEquals('o', s.pivot)
        assertEquals("disc", s.left)
        assertEquals("ver", s.right)
    }

    @Test fun `accompanied odd length picks middle`() {
        val s = OrpCalculator.split("accompanied")
        assertEquals('p', s.pivot)
        assertEquals("accom", s.left)
        assertEquals("anied", s.right)
    }

    @Test fun `commencement even length picks after-middle`() {
        val s = OrpCalculator.split("commencement")
        assertEquals('c', s.pivot)
        assertEquals("commen", s.left)
        assertEquals("ement", s.right)
    }

    @Test fun `trailing punctuation excluded from count`() {
        val s = OrpCalculator.split("Hello,")
        assertEquals('l', s.pivot)
    }

    @Test fun `internal apostrophe excluded from count`() {
        val s = OrpCalculator.split("isn't")
        assertEquals('n', s.pivot)
    }

    @Test fun `all symbols falls back to middle char`() {
        val s = OrpCalculator.split("---")
        assertEquals('-', s.pivot)
    }
}
