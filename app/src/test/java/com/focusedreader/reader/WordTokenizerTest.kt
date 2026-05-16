package com.focusedreader.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WordTokenizerTest {
    @Test fun `splits on whitespace`() {
        assertEquals(listOf("hello", "world"), WordTokenizer.tokenize("hello world"))
    }
    @Test fun `collapses repeated whitespace`() {
        assertEquals(listOf("a", "b"), WordTokenizer.tokenize("a   \t \n b"))
    }
    @Test fun `preserves punctuation attached to words`() {
        assertEquals(listOf("Hello,", "world!"), WordTokenizer.tokenize("Hello, world!"))
    }
    @Test fun `empty input returns empty list`() {
        assertEquals(emptyList<String>(), WordTokenizer.tokenize(""))
    }
    @Test fun `whitespace-only input returns empty list`() {
        assertEquals(emptyList<String>(), WordTokenizer.tokenize("   \n\t  "))
    }
}
