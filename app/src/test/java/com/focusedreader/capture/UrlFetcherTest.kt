package com.focusedreader.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrlFetcherTest {
    private val fetcher = UrlFetcher()

    @Test fun `https URL detected`() = assertTrue(fetcher.looksLikeUrl("https://example.com"))
    @Test fun `http URL detected`() = assertTrue(fetcher.looksLikeUrl("http://example.com"))
    @Test fun `URL with path and query detected`() =
        assertTrue(fetcher.looksLikeUrl("https://www.gutenberg.org/cache/epub/1184/pg1184.txt"))
    @Test fun `URL with leading whitespace detected`() =
        assertTrue(fetcher.looksLikeUrl("  https://example.com  "))
    @Test fun `plain text not detected as URL`() =
        assertFalse(fetcher.looksLikeUrl("Hello world this is a sentence"))
    @Test fun `text containing url not detected`() =
        assertFalse(fetcher.looksLikeUrl("Visit https://example.com for more"))
    @Test fun `empty not detected`() = assertFalse(fetcher.looksLikeUrl(""))
    @Test fun `ftp not detected`() = assertFalse(fetcher.looksLikeUrl("ftp://example.com"))
    @Test fun `case insensitive`() = assertTrue(fetcher.looksLikeUrl("HTTPS://EXAMPLE.COM"))

    // -- looksLikeHtml --
    @Test fun `looksLikeHtml detects full html document`() =
        assertTrue(fetcher.looksLikeHtml("<html><body>x</body></html>"))
    @Test fun `looksLikeHtml uppercase html tag`() =
        assertTrue(fetcher.looksLikeHtml("<HTML>"))
    @Test fun `looksLikeHtml detects body fragment`() =
        assertTrue(fetcher.looksLikeHtml("<body>x</body>"))
    @Test fun `looksLikeHtml plain text not html`() =
        assertFalse(fetcher.looksLikeHtml("hello world"))
    @Test fun `looksLikeHtml inline em tag not html`() =
        assertFalse(fetcher.looksLikeHtml("hello <em>world</em>"))
    @Test fun `looksLikeHtml empty not html`() =
        assertFalse(fetcher.looksLikeHtml(""))
    @Test fun `looksLikeHtml doctype detected`() =
        assertTrue(fetcher.looksLikeHtml("<!DOCTYPE html><p>x</p>"))

    // -- extractHtmlText --
    @Test fun `extractHtmlText returns visible text and drops script`() {
        val out = fetcher.extractHtmlText(
            "<html><body><p>Hello</p><script>alert(1)</script></body></html>"
        )
        assertTrue(out.contains("Hello"))
        assertFalse(out.contains("alert"))
    }

    @Test fun `extractHtmlText prefers article container`() {
        val out = fetcher.extractHtmlText(
            "<html><body><nav>NAV_TEXT</nav><article><p>Main content here</p></article></body></html>"
        )
        assertTrue(out.contains("Main content here"))
        assertFalse(out.contains("NAV_TEXT"))
    }

    @Test fun `extractHtmlText collapses whitespace`() {
        val out = fetcher.extractHtmlText("<html><body><p>a   b\n\tc</p></body></html>")
        assertEquals("a b c", out)
    }

    @Test fun `extractHtmlText on empty returns empty`() {
        // Jsoup tolerates empty input; returns empty string after trim.
        assertEquals("", fetcher.extractHtmlText(""))
    }

    @Test fun `extractHtmlText on malformed html still extracts text`() {
        val out = fetcher.extractHtmlText("<div><p>unterminated")
        assertTrue(out.contains("unterminated"))
    }
}
