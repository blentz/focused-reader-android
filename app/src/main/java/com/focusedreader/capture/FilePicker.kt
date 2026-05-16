package com.focusedreader.capture

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilePicker @Inject constructor() {

    sealed class Result {
        data class Ok(val text: String) : Result()
        data class TooLarge(val sizeBytes: Long) : Result()
        data object ReadFailed : Result()
    }

    suspend fun read(ctx: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        val size = fileSize(ctx, uri)
        if (size > MAX_BYTES) return@withContext Result.TooLarge(size)

        val mime = ctx.contentResolver.getType(uri)
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                if (mime == "application/pdf") {
                    PDFBoxResourceLoader.init(ctx.applicationContext)
                    PDDocument.load(stream).use { doc -> PDFTextStripper().getText(doc) }
                } else {
                    readTextFromStream(stream)
                }
            }
        }.getOrNull()
        if (text.isNullOrBlank()) Result.ReadFailed else Result.Ok(text)
    }

    @Deprecated("Use read() to get TooLarge / ReadFailed distinction")
    suspend fun readText(ctx: Context, uri: Uri): String? =
        (read(ctx, uri) as? Result.Ok)?.text

    private fun fileSize(ctx: Context, uri: Uri): Long {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && !c.isNull(idx)) return c.getLong(idx)
            }
        }
        return -1
    }

    internal fun readTextFromStream(stream: InputStream): String =
        stream.bufferedReader().use { it.readText() }

    companion object {
        const val MAX_BYTES: Long = 50L * 1024 * 1024 // 50 MB
    }
}
