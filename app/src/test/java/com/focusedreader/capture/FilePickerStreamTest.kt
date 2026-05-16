package com.focusedreader.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class FilePickerStreamTest {
    private val picker = FilePicker()

    private fun stream(content: String) = ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    @Test fun `reads simple ascii`() {
        assertEquals("hello world", picker.readTextFromStream(stream("hello world")))
    }

    @Test fun `empty stream yields empty string`() {
        assertEquals("", picker.readTextFromStream(stream("")))
    }

    @Test fun `preserves newlines`() {
        val text = "line one\nline two\nline three"
        assertEquals(text, picker.readTextFromStream(stream(text)))
    }

    @Test fun `preserves utf-8 non-ascii`() {
        val text = "héllo wörld ☃"
        assertEquals(text, picker.readTextFromStream(stream(text)))
    }
}
