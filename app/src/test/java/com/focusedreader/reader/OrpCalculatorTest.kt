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

    @Test fun `single uppercase char picks self`() {
        assertEquals('Z', OrpCalculator.split("Z").pivot)
    }

    @Test fun `all uppercase word treated like lowercase`() {
        // HELLO -> 5 alphanum -> middle idx 2 -> 'L'
        val s = OrpCalculator.split("HELLO")
        assertEquals('L', s.pivot)
        assertEquals("HE", s.left)
        assertEquals("LO", s.right)
    }

    @Test fun `digits count as alphanumeric`() {
        // "abc123" -> 6 alphanum -> after-middle idx 3 -> '1'
        val s = OrpCalculator.split("abc123")
        assertEquals('1', s.pivot)
        assertEquals("abc", s.left)
        assertEquals("23", s.right)
    }

    @Test fun `leading punctuation excluded from count`() {
        // "(hi)" -> 2 alphanum at positions 1,2 -> alphaPositions[1] = pos 2 -> 'i'
        val s = OrpCalculator.split("(hi)")
        assertEquals('i', s.pivot)
        assertEquals("(h", s.left)
        assertEquals(")", s.right)
    }

    @Test fun `internal digit word`() {
        // "v2word" -> 6 alphanum -> after-middle idx 3 -> 'o'
        val s = OrpCalculator.split("v2word")
        assertEquals('o', s.pivot)
    }

    @Test fun `pivotIndex on empty string returns zero`() {
        assertEquals(0, OrpCalculator.pivotIndex(""))
    }

    @Test fun `split throws on empty`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            OrpCalculator.split("")
        }
    }
}
