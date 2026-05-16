package com.focusedreader.capture

import com.focusedreader.data.ImportSource
import com.focusedreader.data.SessionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImportTextUseCaseTest {
    private val repo: SessionRepository = mockk(relaxed = true)
    private val fetcher: UrlFetcher = mockk(relaxed = true)
    private val useCase = ImportTextUseCase(repo, fetcher)

    @Test fun `null input is Empty`() = runTest {
        assertEquals(ImportTextUseCase.Result.Empty, useCase(null, ImportSource.SHARE))
        coVerify(exactly = 0) { repo.import(any(), any()) }
    }

    @Test fun `whitespace input is Empty`() = runTest {
        assertEquals(ImportTextUseCase.Result.Empty, useCase("   \n  ", ImportSource.SHARE))
    }

    @Test fun `plain text imported as-is`() = runTest {
        coEvery { fetcher.looksLikeUrl(any()) } returns false
        assertEquals(ImportTextUseCase.Result.Ok, useCase("hello world", ImportSource.CLIPBOARD))
        coVerify { repo.import("hello world", ImportSource.CLIPBOARD) }
    }

    @Test fun `URL fetches and imports content`() = runTest {
        coEvery { fetcher.looksLikeUrl("https://example.com") } returns true
        coEvery { fetcher.fetchAndExtract("https://example.com") } returns "fetched content"
        assertEquals(ImportTextUseCase.Result.Ok, useCase("https://example.com", ImportSource.SHARE))
        coVerify { repo.import("fetched content", ImportSource.SHARE) }
    }

    @Test fun `URL fetch failure returns FetchFailed`() = runTest {
        coEvery { fetcher.looksLikeUrl(any()) } returns true
        coEvery { fetcher.fetchAndExtract(any()) } returns null
        assertEquals(ImportTextUseCase.Result.FetchFailed, useCase("https://example.com", ImportSource.SHARE))
        coVerify(exactly = 0) { repo.import(any(), any()) }
    }

    @Test fun `URL fetch returning blank is Empty`() = runTest {
        coEvery { fetcher.looksLikeUrl(any()) } returns true
        coEvery { fetcher.fetchAndExtract(any()) } returns "   "
        assertEquals(ImportTextUseCase.Result.Empty, useCase("https://example.com", ImportSource.SHARE))
    }
}
