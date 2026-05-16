package com.focusedreader.capture

import com.focusedreader.data.ImportSource
import com.focusedreader.data.SessionRepository
import javax.inject.Inject

class ImportTextUseCase @Inject constructor(
    private val repo: SessionRepository,
    private val urlFetcher: UrlFetcher
) {
    sealed class Result {
        data object Empty : Result()
        data object Ok : Result()
        data object FetchFailed : Result()
    }

    suspend operator fun invoke(text: String?, source: ImportSource): Result {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return Result.Empty

        val content = when {
            urlFetcher.looksLikeUrl(cleaned) ->
                urlFetcher.fetchAndExtract(cleaned) ?: return Result.FetchFailed
            urlFetcher.looksLikeHtml(cleaned) ->
                urlFetcher.extractHtmlText(cleaned)
            else -> cleaned
        }

        if (content.isBlank()) return Result.Empty
        repo.import(content, source)
        return Result.Ok
    }
}
