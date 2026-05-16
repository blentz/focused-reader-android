package com.focusedreader.capture

import com.focusedreader.data.ImportSource
import com.focusedreader.data.SessionRepository
import javax.inject.Inject

class ImportTextUseCase @Inject constructor(private val repo: SessionRepository) {
    sealed class Result {
        data object Empty : Result()
        data object Ok : Result()
    }
    suspend operator fun invoke(text: String?, source: ImportSource): Result {
        val cleaned = text?.trim().orEmpty()
        if (cleaned.isBlank()) return Result.Empty
        repo.import(cleaned, source)
        return Result.Ok
    }
}
