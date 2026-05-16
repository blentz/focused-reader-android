package com.focusedreader.capture

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
}
