package com.focusedreader.capture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlFetcher @Inject constructor() {

    private val urlPattern = Regex("""^\s*https?://\S+\s*$""", RegexOption.IGNORE_CASE)

    fun looksLikeUrl(text: String): Boolean = urlPattern.matches(text)

    suspend fun fetchAndExtract(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val response = Jsoup.connect(url.trim())
                .timeout(15_000)
                .userAgent("Mozilla/5.0 FocusedReader/0.1")
                .ignoreContentType(true)
                .followRedirects(true)
                .execute()

            val contentType = response.contentType().orEmpty().lowercase()
            val body = response.body()

            val extracted = if (contentType.startsWith("text/plain") ||
                (!body.contains("<html", ignoreCase = true) && !body.contains("<body", ignoreCase = true))
            ) {
                body
            } else {
                val doc = Jsoup.parse(body)
                doc.select("script, style, nav, footer, aside, header, noscript").remove()
                val container = doc.selectFirst("article, main") ?: doc.body()
                container?.text().orEmpty()
            }

            extracted.replace(Regex("\\s+"), " ").trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
